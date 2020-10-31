# JMS Proxy 

The HMTL over JMS solution consists of 3 main components **broker**, **consumer** and **producer**


**Broker**

The broker is represented by an _[ActiveMQ-Broker](https://activemq.apache.org/run-broker)_.  
A broker will manage all publishing and subscriptions and makes sure that producer and consumer share messages.  
The broker should be part of the internet-server.  
The currently used broker version is `apache-activemq-5.15.7-broker`.   

**Consumer**

A consumer a subscribes to the broker and gets notified on incoming messages from the **producer**.    

In this solution the consumer is represented by the a webapp application `http-over-jms-proxy-consumer.war`

**Producer**

A producer receives HTTP requests directly from users browsers or a webserver reverse proxy.   
The producer converts every HTTP request to a JMS message and sends it to the broker so the consumer will be receive the message.  

In this solution the consumer is represented by the a webapp application `http-over-jms-proxy-producer.war` 


## Setting up

The setup mainly covers the configuration of the content in directory `internet/` and `dmz/`.

Most of the configuration is done in the same way in both directories.



1. Copy the `internet` directory to a server instance that can be accessed from the Internet.

2. Copy the directory `dmz` on the server instance in the DMZ.

3. Configure the broker host in the file `internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties`.
    
    To do this, edit the following entry, but leave the port at `61616`
    ```properties
        de.superfusion.jms.BROKER_DESTINATION=tcp://localhost:61616
    ```
    The host set here must be accessible from the `dmz` instance via the specified port.
    
4. Configure the broker host in the file `dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties`.
    
    To do this, edit the following entry, but leave the port at `61616`
    ```properties
        de.superfusion.jms.BROKER_DESTINATION=tcp://localhost:61616
    ```
    The host set here must be accessible from the `internet` instance via the specified port.
    
5. Configure the target instance host URL in file `dmz/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties`.

    To do this, edit the following entry
    ```properties
        de.superfusion.transport.jms.PROXY.targetUri=https://angular.io
        de.superfusion.jms.TARGET_URI=https://angular.io
    ```
    The url of the actual target server that provides the web content is expected here.
    
    The host set here must be accessible from the `dmz` instance via the specified port.
    
6. Configure the target instance host URL in file `internet/HTTP2JMS_CONF/jms-proxy/jms-proxy.properties`. 

    To do this, edit the following entry
    ```properties
        de.superfusion.transport.jms.PROXY.targetUri=https://angular.io
        de.superfusion.jms.TARGET_URI=https://angular.io
    ```
    The url of the actual target server that provides the web content is expected here.
    
    The host set here must be accessible from the `internet` instance via the specified port.


### Prerequisite

Requires Java 8 Environment



## Start
 
The instances must be started in the given order

1. Broker
2. `dmz` instance
3. `internet` instance


If may be necessary, to set the executable flag for the files in the following directories

- `internet/apache-activemq-5.15.7-broker/bin`
- `internet/apache-tomcat-8.0.53-producer/bin`
- `dmz/apache-tomcat-8.0.53-consumer/bin`


### Broker


Start 

```bash
   $ internet/apache-activemq-5.15.7-broker/bin/activemq start
```

Stop

```bash
   $ internet/apache-activemq-5.15.7-broker/bin/activemq stop
```

For Windows systems the .BAT file `activemq.bat` has to be executed instead of `activemq`.

The administration interface of the broker can be reached at url `http://localhost:8161/admin/`.

The admin interface should be disabled when running in production.


### Consumer Tomcat

The consumer Tomcat delivers the web content from the target server

Start 

To start, the absolute path to directory `dmz/HTTP2JMS_CONF` must be set as JVM parameter.

```bash
   $ JAVA_OPTS="-DHTTP2JMS_CONF=<absolute-path>/dmz/HTTP2JMS_CONF"; \
     dmz/apache-tomcat-8.0.53-consumer/bin/catalina.sh start
```

Stop

```bash
     dmz/apache-tomcat-8.0.53-consumer/bin/catalina.sh stop
```

For Windows systems the .BAT file `catalina.bat` has to be executed instead of `catalina.sh`.


### Producer Tomcat

The producer Tomcat asks the broker for content from the web.

The Tomcat producer is set up with the host name `localhost`, port` 9374` and with the transport protocol `TLS`.

All requests sent to `https://localhost:9374` will be responded by the _target-server (`TARGET_URI`/`targetUri`)_.

Start 

To start, the absolute path to directory `internet/HTTP2JMS_CONF` must be set as JVM parameter.

```bash
   $ export JAVA_OPTS="-DHTTP2JMS_CONF=<absolute-path>/internet/HTTP2JMS_CONF"; \
     internet/apache-tomcat-8.0.53-producer/bin/catalina.sh start
```

Stop

```bash
     internet/apache-tomcat-8.0.53-producer/bin/catalina.sh stop
```

For Windows systems the .BAT file `catalina.bat` has to be executed instead of `catalina.sh`.


### Notes
 
Setting up the Secure Socket Layer on the **broker** is basically possible, but was not taken into account in this installation.

In general, the **consumer** can also be deployed into an existing Tomcat installation.
