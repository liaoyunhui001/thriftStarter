package com.rst.thrift.server;

import com.rst.thrift.client.ThriftAddress;
import com.rst.thrift.export.ThriftAddressFactory;
import com.rst.thrift.export.ZookeeperService;
import com.rst.thrift.tools.SpringUtil;

/**
 * Created by hujia on 2017/4/7.
 */
public class ZookeeperThriftAddressFactory implements ThriftAddressFactory {

    private ZookeeperService zookeeperService;

    @Override
    public ThriftAddress get(Class service) {
        if (zookeeperService == null) {
            zookeeperService = SpringUtil.getBean(ZookeeperService.class);
        }

        ZookeeperService.Data data = zookeeperService.getService(service.getSimpleName());

        if (data != null) {
            return new ThriftAddress(data.getIp(), data.getPort(),
                    service, data.isMultiplexedProtocol(), data.isNonblocking());
        }
        return null;
    }
}
