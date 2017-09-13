package com.rst.thrift.export;

import com.rst.thrift.client.ThriftAddress;

/**
 * Created by hujia on 2017/3/21.
 */
public interface ThriftAddressFactory {
    ThriftAddress get(Class service);
}
