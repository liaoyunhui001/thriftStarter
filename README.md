如何使用，pox文件添加如下依赖：
<repositories>
    <repository>
        <id>nexus</id>
        <name>nexus</name>
        <url>http://114.55.125.148:8081/nexus/content/groups/public/</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>com.rst</groupId>
    <artifactId>thrift-starter</artifactId>
    <version>1.2.0</version>
</dependency>

新增zookeeper进行thrift服务的注册与发现
zookeeper.server.uri=host:ip#(比如zookeeper服务起于127.0.0.1:2181)
zookeeper.server.enable=false(通过此配置关闭zookeeper)
如果定义了自己的ThriftAddressFactory，自定义的逻辑会取代zookeeper接管thrift服务的注册与发现。