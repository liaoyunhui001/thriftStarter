package com.rst.thrift.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hujia on 2017/4/7.
 */
public interface ZookeeperService {
    void registerService(String service, int port, boolean multiplexedProtocol, boolean nonblocking);
    Data getService(String service);
    List<Data> getAllService(String service);

    class Data {
        private static Logger logger = LoggerFactory.getLogger(ThriftServer.class);
        private String ip;
        private int port;
        private boolean multiplexedProtocol;
        private boolean nonblocking;

        private static Pattern pattern = Pattern.compile(
                "\\s*([\\w,.]*)\\s*:\\s*(\\d{2,10})" +
                        "\\s*:*\\s*(\\w*)\\s*:*\\s*(\\w*)\\s*");

        public static Data fromIdentify(String identify) {
            Data data = new Data();
            Matcher matcher = pattern.matcher(identify);

            if (matcher.matches()) {
                matcher.group();
                data.ip = matcher.group(1).trim();
                data.port = Integer.parseInt(matcher.group(2).trim());
                data.multiplexedProtocol = "true".equalsIgnoreCase(matcher.group(3));
                data.nonblocking = "true".equalsIgnoreCase(matcher.group(4));
            }

            return data;
        }

        public static boolean isValidData(Data data) {
            return data != null && data.isValid();
        }

        public boolean isValid() {
            return !StringUtils.isEmpty(ip) && port != 0;
        }

        @Override
        public String toString() {
            return identify();
        }

        public String identify() {
            return ip + ":" + port + ":" + multiplexedProtocol + ":" + nonblocking;
        }

        public Data() {

        }

        public Data(String ip, int port, boolean multiplexedProtocol, boolean nonblocking) {
            this.ip = ip;
            this.port = port;
            this.multiplexedProtocol = multiplexedProtocol;
            this.nonblocking = nonblocking;
        }

        public boolean isMultiplexedProtocol() {
            return multiplexedProtocol;
        }

        public void setMultiplexedProtocol(boolean multiplexedProtocol) {
            this.multiplexedProtocol = multiplexedProtocol;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isNonblocking() {
            return nonblocking;
        }

        public void setNonblocking(boolean nonblocking) {
            this.nonblocking = nonblocking;
        }
    }
}
