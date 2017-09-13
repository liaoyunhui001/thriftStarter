package com.rst.thrift.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rst.thrift.ThriftConfiguration;
import com.rst.thrift.export.EnableThriftServer;
import com.rst.thrift.export.ThriftServer;
import com.rst.thrift.tools.SpringUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hujia on 2017/4/5.
 */
@Aspect
@Component
public class ThriftServerAspect implements ApplicationListener<ApplicationReadyEvent> {
    private static final int DEFAULT_THRIFT_SERVER_PORT = 9527;

    private ThreadLocal<Gson> localGson = ThreadLocal.withInitial(
            ()-> new GsonBuilder().serializeNulls().setPrettyPrinting().create());

    @Autowired
    ThriftConfiguration configuration;

    private List<String> basePackages = new ArrayList<>();

    private ThriftServer thriftServer;

    ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Class applicationClass = event.getSpringApplication().getMainApplicationClass();
        if (applicationClass.isAnnotationPresent(EnableThriftServer.class)) {
            EnableThriftServer enableThriftServer =
                    (EnableThriftServer) applicationClass.getAnnotation(EnableThriftServer.class);
            for (int i = 0; i < enableThriftServer.basePackages().length; i++) {
                basePackages.add(enableThriftServer.basePackages()[i]);
            }

            if (basePackages.isEmpty()) {
                basePackages.add(applicationClass.getPackage().getName());
            }

            //优先配置文件，其次代码注解，最后都没有配置的话默认为DEFAULT_THRIFT_SERVER_PORT
            int port = configuration.getServerPort();
            if (port == 0) {
                if (enableThriftServer.port() != 0) {
                    port = enableThriftServer.port();
                } else {
                    port = DEFAULT_THRIFT_SERVER_PORT;
                }
            }

            boolean nonblocking = configuration.isEnableNonblockingServer();

            if (thriftServer == null) {
                thriftServer = SpringUtil.getBean(ThriftServer.class);
            } else {
                thriftServer.stop();
            }

            thriftServer.start(port, configuration.isEnableNonblockingServer(), basePackages);
        }
    }

    @Pointcut("@within(com.rst.thrift.export.EnableLog)" +
            "||@within(com.rst.thrift.export.ThriftService)")
    public void thriftLog() {}

    @Before("thriftLog()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        if (!configuration.isEnableLog()) {
            return;
        }

        Gson gson = localGson.get();

        if (gson == null) {
            gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
            localGson.set(gson);
        }

        try {
            Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

            Signature signature = joinPoint.getSignature();
            MethodSignature methodSignature = (MethodSignature) signature;
            String[] paramNames = methodSignature.getParameterNames();
            Object[] params = joinPoint.getArgs();

            if (paramNames == null || paramNames.length < params.length) {
                paramNames = new String[params.length];
                for (int i = 0; i < params.length; i++) {
                    paramNames[i] = "arg" + i;
                }
            }

            StringBuilder msg = new StringBuilder("【Call】 " + signature.getName() + "(\n");

            for (int i = 0; i < params.length - 1; i++) {
                msg.append(paramNames[i] + ":" + gson.toJson(params[i]) + ",\n");
            }

            if (params.length > 0) {
                msg.append(paramNames[params.length - 1] + ":" + gson.toJson(params[params.length - 1]) + "\n)");
            } else {
                msg.append("\n)");
            }

            logger.info(msg.toString());
            startTime.set(System.currentTimeMillis());
        } catch (Exception e) {

        }
    }

    @AfterReturning(returning = "ret", pointcut = "thriftLog()")
    public void doAfterReturning(JoinPoint joinPoint, Object ret) throws Throwable {
        if (!configuration.isEnableLog()) {
            return;
        }

        Gson gson = localGson.get();

        if (gson == null) {
            gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
            localGson.set(gson);
        }

        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        logger.info("【Return】{}(time cost: {} ms):\n {}", joinPoint.getSignature().getName(),
                System.currentTimeMillis() - startTime.get(), gson.toJson(ret));
    }
}
