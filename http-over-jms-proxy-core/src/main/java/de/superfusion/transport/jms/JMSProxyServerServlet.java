package de.superfusion.transport.jms;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import javax.jms.JMSException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * A proxy servlet sending every http request to jms broker.<br/>
 *
 * @author daniele
 */
public class JMSProxyServerServlet extends AbstractInterceptorProxyServlet {

    private JMSMessageClientProducer jmsMessageClientProducer;

    /**
     * If set to {@link Config.Producer#CONSUMER_IS_PROXY()} <code>true</code> no replacements in HTTP request and HTTP response will be made regarding the <code>targetUri</code>.<br/>
     * To do this will be up to the jms consumer client.<br/>
     */

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            getJmsMessageClientProducer();
        } catch (JMSException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Override servlet init-parameter with jvm parameter.<br/>
     *
     * @param key
     * @return
     */
    @Override
    protected String getConfigParam(String key) {
        String valueFromWebXML = super.getConfigParam(key);
        return Config.System_getProperty("de.superfusion.transport.jms.PROXY." + key, valueFromWebXML);
    }

    @Override
    protected HttpRequest createHttpRequest(HttpServletRequest servletRequest) throws IOException {
        if (Config.Producer.CONSUMER_IS_PROXY()) {
            /**
             * don't let the target uri get placed into this request
             */
            return super.createHttpRequest(servletRequest, null, null);
        }
        return super.createHttpRequest(servletRequest);
    }

    @Override
    protected ProxyInterceptorHttpRequest.HeaderValueProvider getRequestHeaderValueProvider() {
        if (Config.Producer.CONSUMER_IS_PROXY()) {
            /**
             * don't let the target uri get placed into this request
             */
            return (count, headerName, headerValue, servletRequest) -> headerValue;
        }
        return super.getRequestHeaderValueProvider();
    }

    @Override
    protected ProxyInterceptorHttpResponse.HeaderValueProvider getResponseHeaderValueProvider() {
        if (Config.Producer.CONSUMER_IS_PROXY()) {
            /**
             * don't let the target uri get placed into this request
             */
            return (headerName, headerValue, servletRequest, servletResponse) -> headerValue;
        }
        return super.getResponseHeaderValueProvider();
    }

    @Override
    protected HttpResponse doService(HttpRequest request, ProxyRequestContext proxyRequestContext) {
        HttpResponse response = null;
        try {
            String jsonRequest = HTTPMapper.stringify(request,
                    proxyRequestContext.requestURL,
                    proxyRequestContext.servletPath,
                    proxyRequestContext.contextPath,
                    proxyRequestContext.pathInfo,
                    proxyRequestContext.queryString);
            boolean loggableRequestURL = Config.Logging.isLogRequestURL(proxyRequestContext.requestURL);
            if (loggableRequestURL) {
                System.out.println("************jsonRequest************");
                System.out.println(jsonRequest);
                System.out.println("***********************************");
            }
            JMSMessageClientProducer jmsMessageClientProducer = getJmsMessageClientProducer();
            String jsonResponse = jmsMessageClientProducer.send(jsonRequest);
            if (loggableRequestURL) {
                System.out.println("************jsonResponse************");
                System.out.println(jsonResponse);
                System.out.println("************************************");
            }
            response = HTTPMapper.responseFromJson(jsonResponse);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return response;
    }


    public JMSMessageClientProducer getJmsMessageClientProducer() throws JMSException {
        if (null == jmsMessageClientProducer)
            jmsMessageClientProducer = new JMSMessageClientProducer();
        return jmsMessageClientProducer;
    }
}
