package de.superfusion.transport.jms;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * Abstract representation of a servlet proxy.<br/>
 * Minimal instantiation is.<br/>
 * <pre>
 * import org.apache.http.HttpRequest;
 * import org.apache.http.HttpResponse;
 *
 * public class MyProxy extends AbstractInterceptorProxyServlet {
 *
 * &#64;Override
 * protected HttpResponse doService(HttpRequest request) {
 * return null;
 * }
 * }
 *
 * </pre>
 * <p>
 * This proxy is taken from : https://github.com/mitre/HTTP-Proxy-Servlet<br/>
 * and heavely enhanced by @author daniele
 */
public abstract class AbstractInterceptorProxyServlet extends HttpServlet {


    protected static final String ATTR_TARGET_URI = AbstractInterceptorProxyServlet.class.getSimpleName() + ".targetUri";
    protected static final String ATTR_TARGET_HOST = AbstractInterceptorProxyServlet.class.getSimpleName() + ".targetHost";


    /**
     * These next 3 are cached here, and should only be referred to in initialization logic. See the <code>ATTR_* ServletRequestAttributes.</code><br/>
     * From the configured servlet init parameter "targetUri".<br/>
     * The target URI as configured. Not null.
     */
    protected String targetUri;
    private HttpClient proxyClient;


    protected ProxyCookieResolver proxyCookieResolver;
    private ProxyConfig p;

    @Override
    public String getServletInfo() {
        return "JMS Proxy Servlet";
    }

    /**
     * Reads a configuration parameter. By default it reads servlet init parameters but
     * it can be overridden.
     */
    protected String getConfigParam(String key) {
        return getServletConfig().getInitParameter(key);
    }

    protected String getTargetUri(HttpServletRequest servletRequest) {
        return (String) servletRequest.getAttribute(ATTR_TARGET_URI);
    }

    protected HttpHost getTargetHost(HttpServletRequest servletRequest) throws Exception {
        return ProxyInterceptorHttpRequest.extractHost(p.getTargetUri());
        // return (HttpHost) servletRequest.getAttribute(ATTR_TARGET_HOST);
    }


    @Override
    public void init() throws ServletException {
        initConfig();
        initTarget();
        initProxyClient();
    }

    private void initConfig() {
        proxyCookieResolver =
                new ProxyCookieResolver(new ProxyCookiePrefixer(getCookieNamePrefix("!Proxy!" + getServletConfig().getServletName())));
        p = new ProxyConfig(getServletConfig());
    }

    protected void initTarget() throws ServletException {
        targetUri = p.getTargetUri()/**getConfigParam(P_TARGET_URI)*/;
        if (targetUri == null)
            throw new ServletException(p.P_TARGET_URI + " is required.");
//        /**
//         * test it's valid
//         */
//        try {
//            targetUriObj = new URI(targetUri);
//        } catch (Exception e) {
//            throw new ServletException("Trying to process targetUri init parameter: " + e, e);
//        }
//        targetHost = URIUtils.extractHost(targetUriObj);
    }

    private void initProxyClient() throws ServletException {
        try {
            proxyClient = createHttpClient(buildRequestConfig());
        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Sub-classes can override specific behaviour of {@link RequestConfig}.
     */
    protected RequestConfig buildRequestConfig() {
        RequestConfig.Builder builder = RequestConfig.custom()
                .setRedirectsEnabled(p.isDoHandleRedirects()/**doHandleRedirects*/)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES) // we handle them in the servlet instead
                .setConnectTimeout(p.getConnectTimeout()/**connectTimeout*/)
                .setSocketTimeout(p.getReadTimeout()/**readTimeout*/);
        return builder.build();
    }


    /**
     * Called from {@link #init(javax.servlet.ServletConfig)}.
     * HttpClient offers many opportunities for customization.
     * In any case, it should be thread-safe.
     **/
    protected HttpClient createHttpClient(final RequestConfig requestConfig) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        if (p.isIgnoreSSLCerts()/**ignoreSSLCerts*/) {
            final SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                    .build();
            httpClientBuilder = httpClientBuilder
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setSSLContext(sslContext)
            ;
        }
        return httpClientBuilder
                .setDefaultRequestConfig(requestConfig).build();
    }

    /**
     * The http client used.
     *
     * @see #createHttpClient(RequestConfig)
     */
    protected HttpClient getProxyClient() {
        return proxyClient;
    }

    @Override
    public void destroy() {
        //Usually, clients implement Closeable:
        if (proxyClient instanceof Closeable) {
            try {
                ((Closeable) proxyClient).close();
            } catch (IOException e) {
                log("While destroying servlet, shutting down HttpClient: " + e, e);
            }
        } else {
            //Older releases require we do this:
            if (proxyClient != null)
                proxyClient.getConnectionManager().shutdown();
        }
        super.destroy();
    }

    /**
     * @param request
     * @param proxyRequestContext
     * @return {@code null} if the request should be processed by the http proxy.<br/>
     * Otherwise consume the request and return a valid response.<br/>
     */
    protected abstract HttpResponse doService(HttpRequest request, ProxyRequestContext proxyRequestContext);

    /**
     * This method provides the header parameter value to be stored for the proxy request.<br/>
     * Every header parameter that will be copied to the proxy request passes this method.<br/>
     * If not overridden this method will replace the <code>target host</code> in the <code>Host</code> and <code>Cookie</code> header parameter.<br/>
     *
     * @return The header parameter value to set or <code>null</code> to not set the header parameter at all
     */
    protected ProxyInterceptorHttpRequest.HeaderValueProvider getRequestHeaderValueProvider() {
        return null;
    }

    /**
     * This method may be overridden to return the modified response header parameter value<br/>
     * This method does rename the proxy cookie.<br/>
     * This method does replace the header <code>Location</code> with the <code>targetUri</code>.<br/>
     *
     * @return The header parameter value to set or <code>null</code> to not set the header parameter at all
     */
    protected ProxyInterceptorHttpResponse.HeaderValueProvider getResponseHeaderValueProvider() {
        return null;
    }

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        HttpRequest proxyRequest = null;
        HttpResponse proxyResponse = null;
        try {
            /**
             *
             * initialize request attributes from caches if unset by a subclass by this point
             */
            if (servletRequest.getAttribute(ATTR_TARGET_URI) == null) {
                targetUri = p.getTargetUri();
                servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
            }
            if (servletRequest.getAttribute(ATTR_TARGET_HOST) == null) {
                servletRequest.setAttribute(ATTR_TARGET_HOST, getTargetHost(servletRequest));
            }

            /**
             * Make the Request<br/>
             */
            proxyRequest = createHttpRequest(servletRequest);

            ProxyInterceptorHttpRequest interceptorHttpRequest = new ProxyInterceptorHttpRequest(p, proxyCookieResolver);
            interceptorHttpRequest.setHeaderValueProvider(getRequestHeaderValueProvider());
            interceptorHttpRequest.intercept(proxyRequest, servletRequest);


            /**
             * -----------------------
             * Execute the request
             * -----------------------
             */
            proxyResponse = doService(proxyRequest,
                    new ProxyRequestContext(servletRequest.getRequestURL().toString(),
                            servletRequest.getServletPath(),
                            servletRequest.getContextPath(),
                            servletRequest.getPathInfo(),
                            servletRequest.getQueryString()));

            /**
             * if the request has not being consumed by the above <code>doService</code> method<br/>
             * then process it as a regular proxy request.
             */

            /**
             * this does proxy the servlet request
             */
            if (null == proxyResponse)
                proxyResponse = doExecute(servletRequest, servletResponse, proxyRequest);

            /**
             * Process the response:
             */
            ProxyInterceptorHttpResponse interceptorHttpResponse = new ProxyInterceptorHttpResponse(p, proxyCookieResolver);
            interceptorHttpResponse.setHeaderValueProvider(getResponseHeaderValueProvider());
            interceptorHttpResponse.intercept(proxyRequest, proxyResponse, servletRequest, servletResponse);

        } catch (Exception e) {
            handleRequestException(proxyRequest, e);
        } finally {
            /**
             * make sure the entire entity was consumed, so the connection is released
             */
            if (proxyResponse != null)
                consumeQuietly(proxyResponse.getEntity());
            /**
             * Note: Don't need to close servlet outputStream: http://stackoverflow.com/questions/1159168/should-one-call-close-on-httpservletresponse-getoutputstream-getwriter
             */
        }
    }


    public HttpRequest createHttpRequest(HttpServletRequest servletRequest, String method, String requestUri) throws IOException {
        return new ProxyHttpRequest(p.getTargetUri(), p.isDoSendUrlFragment())
                .createHttpRequest(servletRequest, method, requestUri);
    }

    protected HttpRequest createHttpRequest(HttpServletRequest servletRequest) throws IOException {
        return new ProxyHttpRequest(p.getTargetUri(), p.isDoSendUrlFragment())
                .createHttpRequest(servletRequest);
    }

    protected void handleRequestException(HttpRequest proxyRequest, Exception e) throws ServletException, IOException {
        /**
         * abort request, according to best practice with HttpClient
         */
        if (proxyRequest instanceof AbortableHttpRequest) {
            AbortableHttpRequest abortableHttpRequest = (AbortableHttpRequest) proxyRequest;
            abortableHttpRequest.abort();
        }
        if (e instanceof RuntimeException)
            throw (RuntimeException) e;
        if (e instanceof ServletException)
            throw (ServletException) e;
        //noinspection ConstantConditions
        if (e instanceof IOException)
            throw (IOException) e;
        throw new RuntimeException(e);
    }

    protected HttpResponse doExecute(HttpServletRequest servletRequest, HttpServletResponse servletResponse, HttpRequest proxyRequest) throws Exception {
        if (p.isDoLog()/**doLog*/) {
            log("proxy " + servletRequest.getMethod() + " uri: " + servletRequest.getRequestURI() + " -- " +
                    proxyRequest.getRequestLine().getUri());
        }
        return proxyClient.execute(ProxyInterceptorHttpRequest.extractHost(p.getTargetUri()), proxyRequest);
        // return proxyClient.execute(getTargetHost(servletRequest), proxyRequest);
    }

    protected void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            log(e.getMessage(), e);
        }
    }

    /**
     * HttpClient v4.1 doesn't have the
     * {@link EntityUtils#consumeQuietly(HttpEntity)} method.
     */
    protected void consumeQuietly(HttpEntity entity) {
        try {
            EntityUtils.consume(entity);
        } catch (IOException e) {//ignore
            log(e.getMessage(), e);
        }
    }


    /**
     * The string prefixing rewritten cookies.
     */
    protected String getCookieNamePrefix(String name) {
        return "!Proxy!" + getServletConfig().getServletName();
    }

}
