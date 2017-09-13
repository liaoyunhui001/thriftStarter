package com.rst.thrift.server;

import com.rst.thrift.ThriftConfiguration;
import com.rst.thrift.export.ThriftServer;
import com.rst.thrift.export.ZookeeperService;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created by hujia on 2017/4/7.
 */
@Service
public class ZookeeperServiceImpl implements ZookeeperService {
    private static Logger logger = LoggerFactory.getLogger(ThriftServer.class);

    @Autowired
    private ThriftConfiguration configuration;

    private ZkClient zkClient;

    private Map<String, List<Data>> serviceNameToData = new HashMap<>();

    private Map<String, Data> registeredServices = new HashMap<>();

    private List<String> services = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (configuration.isEnableZookeeperServer()) {
            zkClient = new ZkClient(configuration.getZookeeperServerUri(), 30000);

            final String zookeeperThriftRootPath = configuration.getThriftZookeeperRootPath();

            if (!zkClient.exists(zookeeperThriftRootPath)) {
                zkClient.createPersistent(zookeeperThriftRootPath);
            }

            //init zookeeper registered services data
            services = zkClient.getChildren(zookeeperThriftRootPath);
            services.forEach(serviceConsumer);

            zkClient.subscribeChildChanges(zookeeperThriftRootPath, (parent, child) ->
                    zkClient.getChildren(zookeeperThriftRootPath)
                    .stream()
                    .filter(item -> !services.contains(item))
                    .forEach(serviceConsumer));

            zkClient.subscribeStateChanges(new IZkStateListener() {
                @Override
                public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {

                }

                @Override
                public void handleNewSession() throws Exception {
                    registeredServices.forEach((service, data) -> registerService(
                            service, data.getPort(), data.isMultiplexedProtocol(), data.isNonblocking()));
                }

                @Override
                public void handleSessionEstablishmentError(Throwable error) throws Exception {

                }
            });
        }
    }

    private Consumer<String> serviceConsumer = item-> {
        String path = configuration.getThriftZookeeperRootPath() + "/" + item;
        List<Data> dataList = new ArrayList<>();
        serviceNameToData.put(item, zkClient.getChildren(path).stream()
                .map(child -> Data.fromIdentify(child))
                .filter(data -> Data.isValidData(data))
                .collect(Collectors.toList()));

        zkClient.subscribeChildChanges(
                path,
                (parentPath, currentServiceInstance) -> serviceNameToData.put(item, currentServiceInstance
                        .stream().map(child -> Data.fromIdentify(child))
                        .filter(data -> Data.isValidData(data))
                        .collect(Collectors.toList())));
    };


    @Override
    public void registerService(String service, int port, boolean multiplexedProtocol, boolean nonblocking) {
        if (!configuration.isEnableZookeeperServer()) {
            return;
        }

        String servicePath = configuration.getThriftZookeeperRootPath() + "/" + service;
        boolean rootExists = zkClient.exists(servicePath);
        if (!rootExists) {
            zkClient.createPersistent(servicePath, true);
        }


        String servicesIp = configuration.getServicesIp();
        if ("127.0.0.1".equalsIgnoreCase(servicesIp)) {
            InetAddress address = null;
            try {
                address = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            servicesIp = address.getHostAddress().toString();
        }

        // 注册当前服务
        Data data = new Data(servicesIp, port, multiplexedProtocol, nonblocking);
        String serviceInstancePath = servicePath + "/" + data.identify();
        if (!zkClient.exists(serviceInstancePath)) {
            zkClient.createEphemeral(serviceInstancePath);
            logger.info("Register thrift service to zookeeper：{}", serviceInstancePath);
        } else {
            zkClient.delete(serviceInstancePath);
            zkClient.createEphemeral(serviceInstancePath);
            logger.info("Delete old and Register new thrift service to zookeeper: {}", serviceInstancePath);
        }

        registeredServices.put(service, data);
    }

    @Override
    public Data getService(String service) {
        List<Data> dataList = serviceNameToData.get(service);

        if (dataList == null || dataList.isEmpty()) {
            //force sync
            String path = configuration.getThriftZookeeperRootPath() + "/" + service;

            if (!zkClient.exists(path)) {
                return null;
            }

            dataList = zkClient.getChildren(path).stream()
                    .map(child -> Data.fromIdentify(child))
                    .filter(data -> Data.isValidData(data))
                    .collect(Collectors.toList());

            if (dataList == null || dataList.isEmpty()) {
                return null;
            } else {
                serviceNameToData.put(service, dataList);
            }
        }

        return dataList.get((int)(Math.random() * dataList.size()));
    }

    @Override
    public List<Data> getAllService(String service) {
        return serviceNameToData.get(service);
    }
}
