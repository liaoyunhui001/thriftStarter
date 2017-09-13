package com.rst.thrift.client;

import com.rst.thrift.export.ThriftAddressFactory;
import com.rst.thrift.server.ThriftClientImpl;
import com.rst.thrift.server.ZookeeperThriftAddressFactory;
import com.rst.thrift.tools.SpringUtil;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by hujia on 2017/2/28.
 */
public class ThriftClientPoolFactory extends BasePooledObjectFactory<TServiceClient> {
    private static Logger log = LoggerFactory.getLogger(ThriftClientImpl.class);

    private ThriftAddressFactory thriftAddressFactory;

    private Class service;

    public interface PoolOperationCallBack {
        //销毁client之前执行
        void destroy(TServiceClient client);
        //创建成功是执行
        void make(TServiceClient client);
    }

    private final TServiceClientFactory<TServiceClient> clientFactory;
    private PoolOperationCallBack callback;

    protected ThriftClientPoolFactory(Class service,
                                      TServiceClientFactory<TServiceClient> clientFactory,
                                      PoolOperationCallBack callback) {
        this.clientFactory = clientFactory;
        this.callback = callback;
        this.service = service;
        try {
            this.thriftAddressFactory = SpringUtil.getBean(ThriftAddressFactory.class);
        } catch (Exception e) {
            this.thriftAddressFactory = new ZookeeperThriftAddressFactory();
        }
    }

    @Override
    public TServiceClient create() throws Exception {
        ThriftAddress thriftAddress = thriftAddressFactory.get(service);

        if (thriftAddress == null) {
            log.error("Create {} Client Error: null ThriftAddress", service.getSimpleName());
            throw new Exception("Create " + service.getSimpleName() + " Client Error: null ThriftAddress");
        }

        log.info("Create Client For {} with [{}:{}|MultiplexedProtocol:{}]",
                thriftAddress.getService().getSimpleName(),
                thriftAddress.getHost(),
                thriftAddress.getPort(),
                thriftAddress.isMultiplexedProtocol());
        TTransport transport;
        TProtocol protocol;
        if (thriftAddress.isNonblocking()) {
            transport = new TFramedTransport(new TSocket(thriftAddress.getHost(), thriftAddress.getPort(), 30000));
            protocol = new TCompactProtocol(transport);
        } else {
            transport = new TSocket(thriftAddress.getHost(), thriftAddress.getPort(), 30000);
            protocol = new TBinaryProtocol(transport);
        }

        if (thriftAddress.isMultiplexedProtocol()) {
            protocol = new TMultiplexedProtocol(protocol, service.getSimpleName());
        }

        TServiceClient client = this.clientFactory.getClient(protocol);
        transport.open();

        if(callback != null){
            try{
                callback.make(client);
            }catch(Exception e){
                //
            }
        }
        return client;
    }

    @Override
    public PooledObject<TServiceClient> wrap(TServiceClient obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(PooledObject<TServiceClient> p) throws Exception {
        if(callback != null){
            try{
                callback.destroy(p.getObject());
            }catch(Exception e){
                //
            }
        }
        TTransport pin = p.getObject().getInputProtocol().getTransport();
        pin.close();
    }

    @Override
    public boolean validateObject(PooledObject<TServiceClient> p) {
        TTransport pin = p.getObject().getInputProtocol().getTransport();
        return pin.isOpen();
    }
}
