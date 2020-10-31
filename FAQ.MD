## Common known issues

### No target defined `Target host is not specified`

When the `producer` is configured with `-Dde.superfusion.jms.CONSUMER_IS_PROXY=true`

but the `consumer` has not set a target uri with `de.superfusion.jms.TARGET_URI=...`
 
then this error may occur.

```
ERROR | ID:<some-hostname>-56069-1540536954312-7:1:1:1 Exception while processing message: ID:<some-hostname>-56083-1540536965040-1:1:1:1:3
java.lang.IllegalStateException: org.apache.http.client.ClientProtocolException
	at JMSMessageClientConsumer.respond(JMSMessageClientConsumer.java:122)
	at org.apache.activemq.ActiveMQMessageConsumer.dispatch(ActiveMQMessageConsumer.java:1404)
	at org.apache.activemq.ActiveMQMessageConsumer.iterate(ActiveMQMessageConsumer.java:1575)
	at org.apache.activemq.ActiveMQSessionExecutor.iterate(ActiveMQSessionExecutor.java:191)
	at org.apache.activemq.thread.PooledTaskRunner.runTask(PooledTaskRunner.java:133)
	at org.apache.activemq.thread.PooledTaskRunner$1.run(PooledTaskRunner.java:48)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	at java.lang.Thread.run(Thread.java:745)
Caused by: org.apache.http.client.ClientProtocolException
	at org.apache.http.impl.client.InternalHttpClient.doExecute(InternalHttpClient.java:187)
	at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:118)
	at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:56)
	at JMSMessageClientConsumer.executeHTTPRequest(JMSMessageClientConsumer.java:154)
	at JMSMessageClientConsumer.respond(JMSMessageClientConsumer.java:119)
	... 8 more
Caused by: org.apache.http.ProtocolException: Target host is not specified
	at org.apache.http.impl.conn.DefaultRoutePlanner.determineRoute(DefaultRoutePlanner.java:71)
	at org.apache.http.impl.client.InternalHttpClient.determineRoute(InternalHttpClient.java:125)
	at org.apache.http.impl.client.InternalHttpClient.doExecute(InternalHttpClient.java:184)
	... 12 more
```



### Bad Request 400 `Not in GZIP format`

When this error occurs then mostly a HTTP 400 is the reason.

A webapp firewall may have blocked this request.

Make sure the webapp firewall allowas the target host for redirects, http posts etc...

```
ERROR | ID:<some-hostname>-53815-1540661467320-1:1:1:1 Exception while processing message: ID:<some-hostname>-53818-1540661467958-1:1:1:1:10
java.lang.IllegalStateException: java.util.zip.ZipException: Not in GZIP format
	at JMSMessageClientConsumer.respond(JMSMessageClientConsumer.java:151)
	at org.apache.activemq.ActiveMQMessageConsumer.dispatch(ActiveMQMessageConsumer.java:1404)
	at org.apache.activemq.ActiveMQMessageConsumer.iterate(ActiveMQMessageConsumer.java:1575)
	at org.apache.activemq.ActiveMQSessionExecutor.iterate(ActiveMQSessionExecutor.java:191)
	at org.apache.activemq.thread.PooledTaskRunner.runTask(PooledTaskRunner.java:133)
	at org.apache.activemq.thread.PooledTaskRunner$1.run(PooledTaskRunner.java:48)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	at java.lang.Thread.run(Thread.java:745)
Caused by: java.util.zip.ZipException: Not in GZIP format
	at java.util.zip.GZIPInputStream.readHeader(GZIPInputStream.java:165)
	at java.util.zip.GZIPInputStream.<init>(GZIPInputStream.java:79)
	at java.util.zip.GZIPInputStream.<init>(GZIPInputStream.java:91)
	at org.apache.http.client.protocol.ResponseContentEncoding$1.create(ResponseContentEncoding.java:68)
	at org.apache.http.client.entity.LazyDecompressingInputStream.initWrapper(LazyDecompressingInputStream.java:51)
	at org.apache.http.client.entity.LazyDecompressingInputStream.read(LazyDecompressingInputStream.java:63)
	at com.google.common.io.ByteStreams.copy(ByteStreams.java:110)
	at com.google.common.io.ByteStreams.toByteArray(ByteStreams.java:168)
	at HTTPMapper.createPayload(HTTPMapper.java:302)
	at HTTPMapper.createSimpleHTTPResponse(HTTPMapper.java:290)
	at JMSMessageClientConsumer.executeHTTPRequest(JMSMessageClientConsumer.java:203)
	at JMSMessageClientConsumer.respond(JMSMessageClientConsumer.java:148)
	... 8 more
```
