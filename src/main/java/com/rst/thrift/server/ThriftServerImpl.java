package com.rst.thrift.server;

import com.rst.thrift.ThriftConfiguration;
import com.rst.thrift.export.ThriftServer;
import com.rst.thrift.export.ThriftService;
import com.rst.thrift.export.ZookeeperService;
import com.rst.thrift.tools.ClassScaner;
import com.rst.thrift.tools.SpringUtil;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hujia on 2017/2/23.
 */
@Service
public class ThriftServerImpl implements ThriftServer {
    private static Logger logger = LoggerFactory.getLogger(ThriftServer.class);
    private Set<Object> serviceList = new HashSet<Object>();

    ExecutorService executor = Executors.newSingleThreadExecutor();

    private TServerTransport serverTransport;

    @Autowired
    private ZookeeperService zookeeperService;

    @Autowired
    private ThriftConfiguration configuration;

    @Override
    public void start(int port, boolean nonblocking, List<String> servicePackages) {
        executor.execute(() -> {
            for (String servicePackage : servicePackages) {
                Set<Class> classSet = ClassScaner.scan(servicePackage, ThriftService.class);
                for (Class aClass : classSet) {
                    addService(SpringUtil.getBean(aClass.getInterfaces()[0]));
                }
            }

            if (serviceList.isEmpty()) {
                logger.error("Start thrift server failed: NONE services");
                return;
            }

            init(port, nonblocking);
        });
    }

    @Override
    public void stop() {
        if (serverTransport != null) {
            serverTransport.close();
            serverTransport = null;
        }
    }

    @Override
    public void addService(Object object) {
        if (object != null) {
            serviceList.add(object);
        }
    }

    private void init(int port, boolean nonblocking) {

        try {
            if (serverTransport != null) {
                serverTransport.close();
            }

            if (nonblocking) {
                serverTransport = new TNonblockingServerSocket(port);
            } else {
                serverTransport = new TServerSocket(port);
            }
        } catch (TTransportException e) {
            e.printStackTrace();
            return;
        }

        TMultiplexedProcessor multiplexedProcessor = new TMultiplexedProcessor();

        int registerCount = 0;
        for (Object o : serviceList) {
            Class<?> serviceClass = o.getClass();
            Class<?>[] interfaces = serviceClass.getInterfaces();

            TProcessor processor = null;
            for (Class<?> anInterface : interfaces) {
                String className = anInterface.getEnclosingClass().getSimpleName();
                String serviceName = anInterface.getEnclosingClass().getName();
                String pName = serviceName + "$Processor";

                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Class<?> pClass = classLoader.loadClass(pName);
                    if (!TProcessor.class.isAssignableFrom(pClass)) {
                        continue;
                    }
                    Constructor<?> constructor = pClass.getConstructor(anInterface);
                    processor = (TProcessor) constructor.newInstance(o);
                    multiplexedProcessor.registerProcessor(className, processor);
                    zookeeperService.registerService(className, port, true, nonblocking);
                    registerCount++;
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (processor == null) {
                logger.error("service-class should implements Iface : {}", serviceClass);
            }
        }

        if (registerCount <= 0) {
            logger.error("ACTION: NONE valid service！");
        }

        logger.info("Thrift server[nonblocking:{}] start at port: {}, with services:{}",nonblocking, port, serviceList);

        TServer server;
        int cpuWorkerThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        if (nonblocking) {
            TThreadedSelectorServer.Args tArgs = new TThreadedSelectorServer.Args(
                    (TNonblockingServerSocket)serverTransport);
            tArgs.processor(multiplexedProcessor);
            if (configuration.getNonblockingWorker() > 0) {
                tArgs.workerThreads(configuration.getNonblockingWorker());
            } else {
                tArgs.workerThreads(cpuWorkerThreads);
            }
            tArgs.transportFactory(new TFramedTransport.Factory());
            tArgs.protocolFactory(new TCompactProtocol.Factory());
            server = new TThreadedSelectorServer(tArgs);
        } else {
            server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport)
                    .processor(multiplexedProcessor)
                    .minWorkerThreads(configuration.getMinWorker() > 0 ? configuration.getMinWorker() : cpuWorkerThreads)
                    .maxWorkerThreads(configuration.getMaxWorker() > 0 ? configuration.getMaxWorker() : 320));
        }

        server.serve();
    }
}
