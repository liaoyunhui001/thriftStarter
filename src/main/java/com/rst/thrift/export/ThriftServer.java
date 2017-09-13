package com.rst.thrift.export;

import java.util.List;

/**
 * Created by hujia on 2017/2/23.
 */
public interface ThriftServer {
    void start(int port, boolean nonblocking, List<String> servicePackages);
    void stop();
    void addService(Object object);
}
