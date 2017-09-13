package com.rst.thrift.client;

/**
 * Created by hujia on 2017/3/21.
 */
public class ThriftAddress {
    private String host;
    private int port;
    private Class service;
    private boolean multiplexedProtocol;
    private boolean nonblocking;

    public ThriftAddress(String host, int port, Class service, boolean multiplexedProtocol) {
        this(host, port, service, multiplexedProtocol, false);
    }

    public ThriftAddress(String host, int port, Class service, boolean multiplexedProtocol, boolean nonblocking) {
        this.host = host;
        this.port = port;
        this.service = service;
        this.multiplexedProtocol = multiplexedProtocol;
        this.nonblocking = nonblocking;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Class getService() {
        return service;
    }

    public boolean isMultiplexedProtocol() {
        return multiplexedProtocol;
    }

    public boolean isNonblocking() {
        return nonblocking;
    }
}
