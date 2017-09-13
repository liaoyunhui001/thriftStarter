package com.rst.thrift.export;

/**
 * Created by hujia on 2017/3/1.
 */
public interface ThriftClient<T> {
    T get(Class<T> clientType);
}
