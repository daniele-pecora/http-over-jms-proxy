# HTML over JMS Configuration

## JMS Proxy

The JMS proxy is a sevlet-container that contains the proxy-webapplication.

The proxy-webapplication can be controlled via JVM arguments.

The JMS proxy does behave like a regular proxy and provides the HTTP header parameter  `X-Forwarded-For`.

It must be taken in count that a firewall or webapp-firewall don't have to block 
redirects or HTTP POST requests from the host where the JMS proxy runs.


## On the internet-server


#### Broker

The broker-address is set via configuration parameter  `de.superfusion.jms.BROKER_DESTINATION`.  

It requires to have the following form 

`<protocol:ip:port>` z.B. `tcp://192.168.11.166:61616`, `ssl://192.168.11.166:61616`

or 

`<ip:port>` z.B. `192.168.11.166:61616` 


If no protocol is set the the default protocoll `tcp://` will be used.


#### Messagequeue

The name of the messagequeue is set via configuration parameter  `de.superfusion.jms.REQUEST_MESSAGE_QUEUE_NAME`.
 
When using Apache MQ as broker you can effect the queue configuration via messagequeue name.
For example, by specifying consumer behavior.

See http://activemq.apache.org/exclusive-consumer.html fo further documentation.


#### Persistence

How the messages are persisted is set via configuration parameter  `de.superfusion.jms.DELIVERY_MODE`.

|Value|Description||
|----|-------------|--|
|1|Non persistent at all|Is default value|
|NOT_PERSISTENT|Non persistent||
|2|Persistent||
|PERSISTENT|Persistent||

More see http://activemq.apache.org/what-is-the-difference-between-persistent-and-non-persistent-delivery.html

#### Target-URL

If `de.superfusion.jms.CONSUMER_IS_PROXY = true` then `de.superfusion.jms.TARGET_URI` tells the proxy where to get the original content for all requests.

If `de.superfusion.jms.CONSUMER_IS_PROXY = false` then it tells the consumer to tie all requests to host of `de.superfusion.jms.TARGET_URI`.
If the host doesn't match, the request will be rejected (HTTP 400).

The setting `de.superfusion.jms.CONSUMER_IS_PROXY` set to `true` will resolve all request to the URL set in `Target-URL` 
If it required that only the internal JMS consumer should have knowledge about the target url  

`true` This tells the producer not to modify the http request in any way, because it will be modified by the JMS consumer.
`false` This tells the consumer to modify all http requests so `de.superfusion.jms.TARGET_URI` will be set as HTTP header `Host`.

## On the intranet-server

The configuration sections *Broker*, *Messagequeue*  und *Persistence* must be also configured for the JMS consumer

#### Target-URL

The target-url (destination) is configured via setting `de.superfusion.jms.TARGET_URI`.

The target-url is where the JMS consumer will redirect all requests received from JMS producer that were original addressed to the internal webapplication. 

```
    de.superfusion.jms.TARGET_URI.doPreserveHost
    de.superfusion.jms.TARGET_URI.doPreserveCookies
    de.superfusion.jms.TARGET_URI_doSendUrlFragment
```


#### SSL Passthrough

Configure to ignore SSL certificates for target-url via setting `de.superfusion.jms.IGNORE_TARGET_SSL`.

Value are : `true` or `false`

#### Consumer Anzahl

The amount of JMS consumers can be configured via setting `de.superfusion.jms.CONSUMER_AMOUNT`.

Default is `2`.


#### Configuration files

Mainly the JMS proxy, broker, producer and consumer can be configuren via JVM arguments.  
The configuration via JVM arguments can be overwritten when providing a configuration file `jms-proxy.properties`.  
The configuration file must then be located in a directory `jms-proxy` in path set by JVM argument `HTTP2JMS_CONF`.  

Example path:
```
   ${HTTP2JMS_CONF}/jms-proxy/jms-proxy.properties 
```


The intervall to check if any changes are made to configuration file and reload it if so  
 is set via JVM argument `de.superfusion.jms.RELOAD_CONF_INTERVAL_SEC` (don't use in production).

Has no effect on the settings below
- `de.superfusion.jms.BROKER_DESTINATION`
- `de.superfusion.jms.REQUEST_MESSAGE_QUEUE_NAME`
- `de.superfusion.jms.CONSUMER_AMOUNT`

## Proxy

Proxy settings

```
    de.superfusion.transport.jms.PROXY.log = true
    de.superfusion.transport.jms.PROXY.forwardip
    de.superfusion.transport.jms.PROXY.preserveHost
    de.superfusion.transport.jms.PROXY.preserveCookies = true
    de.superfusion.transport.jms.PROXY.http.protocol.handle-redirects
    de.superfusion.transport.jms.PROXY.http.socket.timeout
    de.superfusion.transport.jms.PROXY.http.read.timeout
    de.superfusion.transport.jms.PROXY.ignoreSSLCerts = true
    de.superfusion.transport.jms.PROXY.preserveCookiePath = true
    de.superfusion.transport.jms.PROXY.targetUri
```

Default values in `web.xml`

```
    de.superfusion.transport.jms.PROXY.log = true
    de.superfusion.transport.jms.PROXY.preserveCookies = true
    de.superfusion.transport.jms.PROXY.ignoreSSLCerts = true
    de.superfusion.transport.jms.PROXY.preserveCookiePath = true
```


## Tomcat

An instance of `http-over-jms-proxy-producer` may be deployed for different application context dependent on how many are required.   

In our case the is deployed under the root path via `${catalina.home}/server.xml` 

```xml
<Host>

    <!-- ... -->

<Context 
    docBase="${catalina.home}/webapps/http-over-jms-proxy-producer" 
    path="" 
    reloadable="true"/>
    
    <!-- ... -->

</Host>
```

 or its own context file in path `${catalina.home}/conf/Catalina/localhost/ROOT.xml`
```xml
<?xml version='1.0' encoding='utf-8'?>
<Context docBase="${HTTP2JMS_CONF}/jms-proxy/producer/http-over-jms-proxy-producer.war"
         path=""
         reloadable="true"
         unpackWAR="true"/>

```

## Incompatibility

Not every HTTP available feature is supported.  
Below a list of known not supported features 
- Streams (video, audio etc...)
- Websockets
- Long polling



## Documentation

[ActiveMQ via SSL](http://activemq.apache.org/how-do-i-use-ssl.html)




Example configuration as JVM arguments
```bash
-D____________________PRODUCER=PRODUCER_______________
-Dde.superfusion.jms.DELIVERY_MODE=NOT_PERSISTENT
-Dde.superfusion.jms.REQUEST_MESSAGE_QUEUE_NAME=HTTP2JMS_TEST_QUEUE
-Dde.superfusion.jms.BROKER_DESTINATION=localhost:61616
-Dde.superfusion.jms.CONSUMER_IS_PROXY=true
-Dde.superfusion.transport.jms.PROXY.targetUri=https://angular.io
-D____________________CONSUMER=CONSUMER_______________
-Dde.superfusion.jms.TARGET_URI=https://angular.io
-Dde.superfusion.jms.TARGET_URI.doPreserveHost=false
-Dde.superfusion.jms.TARGET_URI.doPreserveCookies=true
-Dde.superfusion.jms.TARGET_URI.preserveCookiePath=true
-Dde.superfusion.jms.TARGET_URI.rewrite_servletPath=""
-Dde.superfusion.jms.TARGET_URI.rewrite_contextPath=""
-Dde.superfusion.jms.CONSUMER_AMOUNT=10
-Dde.superfusion.jms.IGNORE_TARGET_SSL=true

```
