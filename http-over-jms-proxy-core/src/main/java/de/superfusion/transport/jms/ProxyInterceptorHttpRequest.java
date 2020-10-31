package de.superfusion.transport.jms;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.utils.URIUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Enumeration;

public class ProxyInterceptorHttpRequest {
    private ProxyConfig p;
    private ProxyCookieResolver proxyCookieResolver;
    private HeaderValueProvider headerValueProvider;

    ProxyInterceptorHttpRequest(ProxyConfig p, ProxyCookieResolver proxyCookieResolver) {
        this.p = p;
        this.proxyCookieResolver = proxyCookieResolver;
    }

    void intercept(HttpRequest proxyRequest, HttpServletRequest servletRequest) throws Exception {
        copyRequestHeaders(servletRequest, proxyRequest);
        setXForwardedForHeader(servletRequest, proxyRequest);
    }

    private void setXForwardedForHeader(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
        if (p.isDoForwardIP()/** doForwardIP */) {
            String forHeaderName = "X-Forwarded-For";
            String forHeader = servletRequest.getRemoteAddr();
            String existingForHeader = servletRequest.getHeader(forHeaderName);
            if (existingForHeader != null) {
                forHeader = existingForHeader + ", " + forHeader;
            }
            proxyRequest.setHeader(forHeaderName, forHeader);

            String protoHeaderName = "X-Forwarded-Proto";
            String protoHeader = servletRequest.getScheme();
            proxyRequest.setHeader(protoHeaderName, protoHeader);
        }
    }

    /**
     * Copy request headers from the servlet client to the proxy request.
     * This is easily overridden to add your own.
     */
    private void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) throws Exception {
        /**
         * Get an Enumeration of all of the header names sent by the client
         */
        @SuppressWarnings("unchecked")
        Enumeration<String> enumerationOfHeaderNames = servletRequest.getHeaderNames();
        while (enumerationOfHeaderNames.hasMoreElements()) {
            String headerName = enumerationOfHeaderNames.nextElement();
            copyRequestHeader(servletRequest, proxyRequest, headerName);
        }
    }

    /**
     * Copy a request header from the servlet client to the proxy request.
     * This is easily overridden to filter out certain headers if desired.
     */
    private void copyRequestHeader(HttpServletRequest servletRequest, HttpRequest proxyRequest, String headerName) throws Exception {
        /**
         * Instead the content-length is effectively set via InputStreamEntity
         */
        if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH))
            return;
        if (ProxyHopHeader.isHopHeader(headerName))
            return;

        @SuppressWarnings("unchecked")
        Enumeration<String> headers = servletRequest.getHeaders(headerName);
        int count = 0;
        while (headers.hasMoreElements()) {
            /**
             * sometimes more than one value
             */
            String headerValue = headers.nextElement();
            headerValue = getHeaderValueProvider().getValue(count, headerName, headerValue, servletRequest);
            if (null != headerValue)
                proxyRequest.addHeader(headerName, headerValue);
            count++;
        }
    }

    /**
     * This method provides the header parameter value to be stored for the proxy request.<br/>
     * Every header parameter that will be copied to the proxy request passes this method.<br/>
     * If not overridden this method will replace the <code>target host</code> in the <code>Host</code> and <code>Cookie</code> header parameter.<br/>
     *
     * @param count
     * @param headerName
     * @param headerValue
     * @param servletRequest
     * @return The header parameter value to set or <code>null</code> to not set the header parameter at all
     */
    private String copyRequestHeaderValueForProxyRequest(int count, String headerName, String headerValue, HttpServletRequest servletRequest) throws Exception {
        String _headerValue = headerValue;
        /**
         * In case the proxy host is running multiple virtual servers,<br/>
         * rewrite the Host header to ensure that we get content from<br/>
         * the correct virtual server
         */
        if (!p.isDoPreserveHost()
                /** !doPreserveHost */
                && headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
            HttpHost host = extractHost(p.getTargetUri())/**getTargetHost(servletRequest)*/;
            _headerValue = host.getHostName();
            if (host.getPort() != -1)
                _headerValue += ":" + host.getPort();
        } else if (
                !p.isDoPreserveCookies()
                        /** !doPreserveCookies*/
                        && headerName.equalsIgnoreCase(org.apache.http.cookie.SM.COOKIE)) {
            _headerValue = proxyCookieResolver.getRealCookie(headerValue);
        }
        return _headerValue;
    }


    static HttpHost extractHost(String targetUri) throws Exception {
        URI targetUriObj;
        /**
         * test it's valid
         */
        try {
            targetUriObj = new URI(targetUri);
        } catch (Exception e) {
            throw new Exception("Trying to process targetUri init parameter: " + e, e);
        }
        return URIUtils.extractHost(targetUriObj);
    }

    public void setHeaderValueProvider(HeaderValueProvider headerValueProvider) {
        this.headerValueProvider = headerValueProvider;
    }

    public HeaderValueProvider getHeaderValueProvider() {
        if (null == this.headerValueProvider) {
            this.headerValueProvider = this::copyRequestHeaderValueForProxyRequest;
        }
        return headerValueProvider;
    }

    interface HeaderValueProvider {
        String getValue(int count, String headerName, String headerValue, HttpServletRequest servletRequest) throws Exception;
    }
}
