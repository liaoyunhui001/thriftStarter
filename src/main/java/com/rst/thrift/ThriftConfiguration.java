package com.rst.thrift;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Created by hujia on 2017/5/18.
 */
@Configuration
public class ThriftConfiguration {
    @Value("${thrift.server.enable-log:true}")
    private boolean enableLog;
    @Value("${thrift.server.port:0}")
    private int serverPort;
    @Value("${zookeeper.server.uri:127.0.0.1:2181}")
    private String zookeeperServerUri;
    @Value("${zookeeper.server.enable:true}")
    private boolean enableZookeeperServer;
    @Value("${thrift.server.ip:127.0.0.1}")
    private String servicesIp;
    @Value("${zookeeper.server.root:/thrift}")
    private String thriftZookeeperRootPath;
    @Value("${thrift.server.nonblocking:false}")
    private boolean enableNonblockingServer;
    @Value("${thrift.server.nonblocking.thread.worker:0}")
    private int nonblockingWorker;
    @Value("${thrift.server.thread.min-worker:0}")
    private int minWorker;
    @Value("${thrift.server.thread.max-worker:0}")
    private int maxWorker;

    public boolean isEnableLog() {
        return enableLog;
    }

    public void setEnableLog(boolean enableLog) {
        this.enableLog = enableLog;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getZookeeperServerUri() {
        return zookeeperServerUri;
    }

    public void setZookeeperServerUri(String zookeeperServerUri) {
        this.zookeeperServerUri = zookeeperServerUri;
    }

    public boolean isEnableZookeeperServer() {
        return enableZookeeperServer;
    }

    public void setEnableZookeeperServer(boolean enableZookeeperServer) {
        this.enableZookeeperServer = enableZookeeperServer;
    }

    public String getServicesIp() {
        return servicesIp;
    }

    public void setServicesIp(String servicesIp) {
        this.servicesIp = servicesIp;
    }

    public String getThriftZookeeperRootPath() {
        return thriftZookeeperRootPath;
    }

    public void setThriftZookeeperRootPath(String thriftZookeeperRootPath) {
        this.thriftZookeeperRootPath = thriftZookeeperRootPath;
    }

    public boolean isEnableNonblockingServer() {
        return enableNonblockingServer;
    }

    public void setEnableNonblockingServer(boolean enableNonblockingServer) {
        this.enableNonblockingServer = enableNonblockingServer;
    }

    public int getNonblockingWorker() {
        return nonblockingWorker;
    }

    public void setNonblockingWorker(int nonblockingWorker) {
        this.nonblockingWorker = nonblockingWorker;
    }

    public int getMinWorker() {
        return minWorker;
    }

    public void setMinWorker(int minWorker) {
        this.minWorker = minWorker;
    }

    public int getMaxWorker() {
        return maxWorker;
    }

    public void setMaxWorker(int maxWorker) {
        this.maxWorker = maxWorker;
    }
}
