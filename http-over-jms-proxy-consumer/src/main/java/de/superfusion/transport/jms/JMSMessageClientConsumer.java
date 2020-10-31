package de.superfusion.transport.jms;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;

import javax.jms.*;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.lang.IllegalStateException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/**
 * This instance receives the http requests as jms message and proxies them using an apache http client.<br/>
 * Example:<br/>
 * <pre>
 * public static void main(String[] args) throws JMSException {
 *      JMSMessageClientConsumer jmsMessageClientReceiver = new JMSMessageClientConsumer();
 *      jmsMessageClientReceiver.start();
 *      System.out.println("Start listening for requests...");
 *      while (true) {
 *          try {
 *          Thread.sleep(100);
 *          } catch (InterruptedException e) {
 *              e.printStackTrace();
 *          }
 *      }
 * }
 * </pre>
 *
 * @author daniele
 */
public class JMSMessageClientConsumer {

    private JMSAbstractClient.JMSSessionClientConsumer sessionClient;
    private String instanceId;
    private RequestValidator requestValidator = new RequestValidator();

    public JMSMessageClientConsumer() throws JMSException {
        this(null);
    }

    /**
     * @param instanceId Any ID that will be used to recognize this instance.<br/>
     *                   Mostly used for logging purposes.<br/>
     *                   May be <code>null</code> or empty , then a random UUID will be created.<br/>
     * @throws JMSException
     */
    public JMSMessageClientConsumer(final String instanceId) throws JMSException {
        String _instanceId = instanceId;
        if (null == _instanceId || String.valueOf(_instanceId).trim().length() < 1) {
            try {
                _instanceId = String.valueOf(UUID.randomUUID());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.instanceId = _instanceId;
        sessionClient = new JMSAbstractClient.JMSSessionClientConsumer();
    }

    private MessageConsumer consumer;

    /**
     * Register the consumer.<br/>
     * Start listening for incoming messages.<br/>
     *
     * @throws JMSException
     */
    public void start() throws JMSException {
        if (null != this.consumer) {
            try {
                this.consumer.close();
            } catch (JMSException e) {
                e.printStackTrace();
            } finally {
                this.consumer = null;
            }
        }
        consumer = sessionClient.getConsumer();
        consumer.setMessageListener(this::respond);
    }


    public void stop() {
        if (null != this.consumer) {
            try {
                this.consumer.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
        if (null != sessionClient) {
            try {
                sessionClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private HttpClient getHttpClient() throws Exception {
        // TODO refactor config into container
        boolean doForwardIP = true;
        /** User agents shouldn't send the url fragment but what if it does? */
        boolean doSendUrlFragment = false;
        boolean doPreserveHost = false;
        boolean doPreserveCookies = false;
        boolean doHandleRedirects = false;
        int connectTimeout = -1;
        int readTimeout = -1;

        RequestConfig.Builder builder = RequestConfig.custom()
                .setRedirectsEnabled(doHandleRedirects)
                .setRelativeRedirectsAllowed(true)
                .setContentCompressionEnabled(true)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES) // we handle them in the servlet instead
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(readTimeout);
        RequestConfig requestConfig = builder.build();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig);
        if (Config.Consumer.IGNORE_TARGET_SSL()) {
            final SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (x509CertChain, authType) -> true).build();
            httpClientBuilder = httpClientBuilder
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setSSLContext(sslContext)
            ;
        }
        HttpClient proxyClient = httpClientBuilder.build();
        return proxyClient;
    }

    private void respond(Message message) {
        try {
            final TextMessage textMessage = (TextMessage) message;
            final String response = textMessage.getText();
            String responseText = executeHTTPRequest(response);
            Message responseMessage = respond(message, responseText);
        } catch (JMSException | IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private String executeHTTPRequest(String response) throws JMSException, IOException, URISyntaxException {
        HTTPMapper.SimplePlainHTTPRequest rq = HTTPMapper.SimplePlainHTTPRequest.fromJson(response);
        String originalRequestURL = rq.requestURL;
        boolean modifiedRequest = HTTPRequestResponseMapper.prepareSimplePlainHTTPRequest(rq);
        String targetUri = rq.uri;
        boolean modifiedRequestURI = !rq.uri.equals(originalRequestURL);
        URI targetUriObj = new URI(targetUri);

        if (Config.Logging.isLogRequestURL(originalRequestURL)) {
            if (modifiedRequestURI) {
                System.out.println(prependInstanceID("** Request URI replaced to : " + rq.uri));
                System.out.println(prependInstanceID("** Host replaced to : " + targetUriObj.getAuthority()));
            }
        }

        HttpHost targetHost = URIUtils.extractHost(targetUriObj);
        if (null == targetHost) {
            /**
             * The host may be empty when relative urls are allowed
             */
            targetHost = URIUtils.extractHost(new URI(Config.Consumer.TARGET_URI()));
        }


        boolean loggableRequestURL = Config.Logging.isLogRequestURL(rq.uri);
        boolean loggableRequestURLCookies = Config.Logging.isLogCookiesOnly();
        HTTPMapper.SimplePlainHTTPResponse rs;

        try {
            requestValidator.isTargetHostValid(targetHost, rq);

            String proxyRequest = response;

            if (loggableRequestURL) {
                System.out.println(prependInstanceID("** Request URI: " + rq.uri));
                System.out.println(prependInstanceID("REQUEST: " + proxyRequest));
            }
            if (modifiedRequest) {
                proxyRequest = rq.toJson();
                if (loggableRequestURL) {
                    if (loggableRequestURLCookies) {
                        logRequest(rq);
                    } else {
                        System.out.println(prependInstanceID("**mod**: " + proxyRequest));
                    }
                }
            }

            HttpRequest httpRequest = HTTPMapper.requestFromJson(proxyRequest);
            HttpClient proxyClient;
            try {
                proxyClient = getHttpClient();
            } catch (Exception e) {
                throw new JMSException(prependInstanceID("Error initializing http client."), e.getMessage());
            }
            HttpResponse httpResponse = proxyClient.execute(targetHost, httpRequest);

            rs = HTTPMapper.createSimpleHTTPResponse(httpResponse);
        } catch (RequestValidator.InvalidHostException | RequestValidator.InvalidTargetHostException | RequestValidator.MissingHostHeaderException | RequestValidator.InvalidTargetHostConfigurationException e) {
            e.printStackTrace();
            /**
             * TODO create HTTP 400
             */
            rs = new HTTPMapper.SimplePlainHTTPResponse();
            rs.reasonPhrase = "Invalid Host";
            rs.status = 400;
            rs.protocol = rq.protocol;
            rs.timestamp = new Date();
            rs.payload = "";
            rs.headers = new ArrayList<>();
        }

        boolean modifiedResponse = HTTPRequestResponseMapper.prepareSimplePlainHTTPResponse(originalRequestURL, rq, rs);
        String responseText = rs.toJson();
        if (loggableRequestURL) {
            if (loggableRequestURLCookies) {
                logResponse(rs);
            } else {
                System.out.println(prependInstanceID(
                        (modifiedResponse ? "*** " : "") + "RESPOND" + (modifiedResponse ? " ***" : "")
                                + ": " + responseText));
            }
        }
        return responseText;
    }

    private void logRequest(HTTPMapper.SimplePlainHTTPRequest rq) {
        StringBuilder sb = new StringBuilder();
        rq.headers.forEach(headerVar -> {
            if (0 == headerVar.getName().compareToIgnoreCase("cookie")) {
                if (sb.length() < 1)
                    sb.append(" # ");
                sb.append(headerVar);
            }
        });
        System.out.println("------------------------------------------------------------------------------------");
        System.out.println(rq.uri);
        System.out.println("# " + sb.toString());
    }

    private void logResponse(HTTPMapper.SimplePlainHTTPResponse rs) {
        StringBuilder sb = new StringBuilder();
        rs.headers.forEach(headerVar -> {
            if (0 == headerVar.getName().compareToIgnoreCase("location")
                    || 0 == headerVar.getName().compareToIgnoreCase("cookie")) {
                if (sb.length() < 1)
                    sb.append(" # ");
                sb.append(headerVar);
            }
        });
        System.out.println(rs.status + "] " + rs.reasonPhrase);
        System.out.println("# " + sb.toString());
    }

    private String prependInstanceID(String string) {
        return String.format("[%1$s] %2$s", this.instanceId, string);
    }

    private Message respond(Message requestMessage, String text) throws JMSException {
        /**
         * Setup a message producer to respond to messages from clients, we will get the destination
         * to send to from the JMSReplyTo header field from a Message
         */
        MessageProducer replyProducer = sessionClient.createProducer(requestMessage.getJMSReplyTo());
        replyProducer.setDeliveryMode(Config.Producer.DELIVERY_MODE());

        /**
         * Create a message
         */
        TextMessage responseMessage = sessionClient.createTextMessage(text);

        /**
         * Set the correlation ID from the received message to be the correlation id of the response message
         * this lets the client identify which message this is a response to if it has more than
         * one outstanding message to the server
         */
        responseMessage.setJMSCorrelationID(requestMessage.getJMSCorrelationID());

        /**
         * Send the response to the Destination specified by the JMSReplyTo field of the received message,
         * this is presumably a temporary queue created by the client
         */
        replyProducer.send(requestMessage.getJMSReplyTo(), responseMessage);
        return responseMessage;
    }

}
