|Property|Value - Type|Default-Value|Description|
|--- |--- |--- |--- |
|GLOBAL||||
|de.superfusion.jms.DELIVERY_MODE|NOT_PERSISTENT , PERSISTENT|NOT_PERSISTENT|The persistence mode for the message handled by the broker|
|de.superfusion.jms.REQUEST_MESSAGE_QUEUE_NAME|String|HTTP2JMS_TEST_QUEUE|The name of the message queue|
|de.superfusion.jms.BROKER_DESTINATION|protocol://host:port , host:port|localhost:61616|The address of the broker.You may not set the protocol then tcp:// will be set as default.|
|de.superfusion.jms.CONSUMER_IS_PROXY|boolean (true, false)|false|This tells the producer not to modify the http request in any way.This tells the consumer to modify all http requests.(experimental)|
|PRODUCER||||
|de.superfusion.transport.jms.PROXY.targetUri|Absolute URL|https://angular.io|Tells the proxy where to get the original content for all requests.|
|||||
|CONSUMER||||
|de.superfusion.jms.TARGET_URI|Absolute URL|https://angular.io|If de.superfusion.jms.CONSUMER_IS_PROXY = true then it tells the proxy where to get the original content for all requests.If de.superfusion.jms.CONSUMER_IS_PROXY = false then it tells the consumer to tie all requests to this host.If the host doesn't match, the request will be rejected (HTTP 400).|
|de.superfusion.jms.TARGET_URI.doPreserveHost|boolean (true, false)|false|Don't change http host header.|
|de.superfusion.jms.TARGET_URI.doPreserveCookies|boolean (true, false)|false|Don't proxy cookies.When true all cookies will be handled by the proxy.|
|de.superfusion.jms.TARGET_URI.preserveCookiePath|boolean (true, false)|true|Don't modify the path set in cookie.|
|de.superfusion.jms.TARGET_URI.rewrite_servletPath|String|-|Override servlet path in cookie.|
|de.superfusion.jms.TARGET_URI.rewrite_contextPath|String|-|Override context path in cookie.|
|de.superfusion.jms.CONSUMER_AMOUNT|Integer|10|Set the amount of consumer that will be registered at startup.Making any changes in configuration requires re-start of Tomcat.|
|de.superfusion.jms.IGNORE_TARGET_SSL|boolean (true, false)|true|Ignore SSL certificates from destination url in de.superfusion.jms.TARGET_URI or de.superfusion.transport.jms.PROXY.targetUri.Set if self signer SSL certs should be accepted or not.|
|||||
|PROXY||||
|de.superfusion.transport.jms.PROXY.log|boolean (true, false)|true|Enables some servlet logging.|
|de.superfusion.transport.jms.PROXY.preserveCookies|boolean (true, false)|false|Keep COOKIES as-is|
|de.superfusion.transport.jms.PROXY.preserveCookiePath|boolean (true, false)|true|Keep the path of COOKIES as-is.|
|de.superfusion.transport.jms.PROXY.ignoreSSLCerts|boolean (true, false)|true|Set if self signer SSL certs should be accepted or not.|
|de.superfusion.transport.jms.PROXY.forwardip|boolean (true, false)|true|Forwarding of the client IP|
|#de.superfusion.transport.jms.PROXY.preserveHost|boolean (true, false)|false|Keep HOST parameter as-is|
|de.superfusion.transport.jms.PROXY.http.protocol.handle-redirects|boolean (true, false)|false|Auto-handle redirects.When enable no redirect with HTTP 302 will be delivered to the client.|
|de.superfusion.transport.jms.PROXY.http.socket.timeout|Integer|-1|Set the socket connection timeout (millis)|
|de.superfusion.transport.jms.PROXY.http.read.timeout|Integer|-1|Set the socket read timeout (millis)|
|de.superfusion.transport.jms.PROXY.targetUri|Absolute URL|https://angular.io|Tells the proxy where to get the original content for all requests.|
|||||
|this settings only when option 'CONSUMER_IS_PROXY=true' is set||||
|de.superfusion.transport.jms.PROXY.reduceCookiePathToFirstChild|boolean (true, false)|false|This will reduce all cookie paths to the first path segment.|
|de.superfusion.transport.jms.PROXY.sendOnlyPrefixedCookie|boolean (true, false)|false|This will prevent client cookies provided by the proxy to be sent to the target host.|
|||||
|LOGGING||||
|de.superfusion.transport.jms.log|boolean (true, false)|true|Enable requests logging.|
|de.superfusion.transport.jms.log.MATCH_REQUEST_URL|String,  Regex|.*|Any request that matches with this regex will be logged out.|
|de.superfusion.transport.jms.log.MATCH_TARGET_URL.cookieOnly|boolean (true, false)|true|Will not log any request or response but the cookies that will be sent to or receive from target host.|
|de.superfusion.transport.jms.log.MATCH_TARGET_URL|String,  Regex|.*|unused|
|||||
|||||
