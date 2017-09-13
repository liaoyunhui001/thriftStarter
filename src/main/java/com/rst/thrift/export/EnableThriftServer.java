package com.rst.thrift.export;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by hujia on 2017/4/5.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableThriftServer {
    @AliasFor("port")
    int value() default 0;

    String[] basePackages() default {};

    @AliasFor("value")
    int port() default 0;
}
