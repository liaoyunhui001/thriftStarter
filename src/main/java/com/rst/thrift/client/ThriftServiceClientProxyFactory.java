package com.rst.thrift.client;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/**
 * Created by hujia on 2017/2/28.
 */
public class ThriftServiceClientProxyFactory implements FactoryBean {
    private Object proxyClient;
    private Class objectClass;

    //service对应的client对象池
    private GenericObjectPool<TServiceClient> pool;
    private Integer maxActive = 32;//最大活跃连接数
    //ms,default 3 min,链接空闲时间
    //-1,关闭空闲检测
    private Integer idleTime = 180000;

    private ThriftClientPoolFactory.PoolOperationCallBack callback =
            new ThriftClientPoolFactory.PoolOperationCallBack() {

        @Override
        public void make(TServiceClient client) {
            System.out.println("create");

        }

        @Override
        public void destroy(TServiceClient client) {
            System.out.println("destroy");

        }
    };

    @Override
    public Object getObject() {
        return proxyClient;
    }

    @Override
    public Class<?> getObjectType() {
        return objectClass;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public boolean initService(Class thriftService, Class interfaceClass) {
        String service = thriftService.getName();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        objectClass = interfaceClass;

        boolean isAsync = interfaceClass.getSimpleName().equals("AsyncIface");

        if (isAsync) {
            return false;
        }

        //加载Client.Factory类
        Class<TServiceClientFactory<TServiceClient>> fi;
        try {
            fi = (Class<TServiceClientFactory<TServiceClient>>)classLoader.loadClass(service + "$Client$Factory");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        TServiceClientFactory<TServiceClient> clientFactory = null;
        try {
            clientFactory = fi.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        ThriftClientPoolFactory clientPoolFactory = new ThriftClientPoolFactory(thriftService, clientFactory, callback);

        initObjectPool(clientPoolFactory);

        proxyClient = Proxy.newProxyInstance(classLoader, new Class[]{objectClass}, (proxy, method, args) -> {
            TServiceClient client = pool.borrowObject();
            boolean needReturn = true;
            try {
                return method.invoke(client, args);
            } catch (InvocationTargetException e) {
                pool.invalidateObject(client);
                needReturn = false;
                throw e.getTargetException();
            } finally {
                if (needReturn) {
                    pool.returnObject(client);
                }
            }
        });

        return true;
    }

    private void initObjectPool(ThriftClientPoolFactory clientPool) {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMinIdle(0);
        poolConfig.setMinEvictableIdleTimeMillis(idleTime);
        poolConfig.setTimeBetweenEvictionRunsMillis(idleTime/2L);
        poolConfig.setTestOnBorrow(true);
        pool = new GenericObjectPool<>(clientPool, poolConfig);
    }
}
