Build with support for 

Requirements

- Apache Active MQ 5.15.3 as Broker
- 2 x Tomcat 8.0.53  (Producer / Consumer)
- Java 8

Install

Copy content of directory `jms-proxy/producer/conf` into **Producer** Tomcat `${catalina.home}/conf`
Copy content of directory `jms-proxy/consumer/conf` into **Consumer** Tomcat `${catalina.home}/conf`


Start Tomcat with JVM Argument `-DHTTP2JMS_CONF` 
pointing to the directory `HTTP2JMS_CONF`

The instances must be started in following order:

1. Apache Active MQ **Broker** 
2. **Producer** Tomcat
3. **Consumer** Tomcat
