package com.rst.thrift.server;

import com.rst.thrift.client.ThriftServiceClientProxyFactory;
import com.rst.thrift.export.ThriftClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hujia on 2017/3/1.
 */
@Service
public class ThriftClientImpl<T> implements ThriftClient<T> {
    private Map<Class, ThriftServiceClientProxyFactory> serviceClientProxyFactoryMap
            = new HashMap<>();

    @Override
    public T get(Class<T> clientType) {
        Class service = clientType.getEnclosingClass();
        ThriftServiceClientProxyFactory clientProxyFactory = serviceClientProxyFactoryMap.get(service);
        if (clientProxyFactory == null) {
            clientProxyFactory = new ThriftServiceClientProxyFactory();

            clientProxyFactory.initService(service, clientType);

            serviceClientProxyFactoryMap.put(service, clientProxyFactory);
        }

        return (T)(clientProxyFactory.getObject());
    }
}
