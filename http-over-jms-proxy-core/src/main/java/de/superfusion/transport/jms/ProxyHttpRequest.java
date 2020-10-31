package de.superfusion.transport.jms;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ProxyHttpRequest {
     static ProxyQueryEncoder proxyQueryEncoder = new ProxyQueryEncoder();
    private String targetUri;
    private boolean doSendUrlFragment;

    public ProxyHttpRequest(String targetUri, boolean doSendUrlFragment) {
        this.targetUri = targetUri;
        this.doSendUrlFragment = doSendUrlFragment;
    }

    /**
     * This will create an {@link HttpRequest} with the replace request uri from {@link ProxyHttpRequest#rewriteUrlFromRequest(HttpServletRequest)}
     *
     * @param servletRequest
     * @return
     * @throws IOException
     */
    public HttpRequest createHttpRequest(HttpServletRequest servletRequest) throws IOException {
        /**
         * note: we won't transfer the protocol version because I'm not sure it would truly be compatible
         */
        String method = servletRequest.getMethod();
        String proxyRequestUri = rewriteUrlFromRequest(servletRequest);
        HttpRequest proxyRequest = createHttpRequest(servletRequest, method, proxyRequestUri);
        return proxyRequest;
    }

    /**
     * This will create an {@link HttpRequest} whith the given <code>method</code> and <code>requestUri</code><br/>
     * any post body will be taken from the <code>servletRequest</code>
     *
     * @param servletRequest
     * @param method         This should be taken from the <code>servletRequest</code>.<br/>
     *                       May be <code>null</code>, then it will be set from <code>servletRequest</code>
     * @param requestUri     The target request uri.<br/> If <code>null</code>, then it will taken from <code>servletRequest</code>
     * @return
     * @throws IOException
     */
    public HttpRequest createHttpRequest(HttpServletRequest servletRequest, String method, String requestUri) throws IOException {
        String _method = null != method ? method : servletRequest.getMethod();
        String _requestUri = null != requestUri ? requestUri : servletRequest.getRequestURI();
        HttpRequest proxyRequest;
        /**
         * spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
         */
        if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null ||
                servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
            proxyRequest = newProxyRequestWithEntity(_method, _requestUri, servletRequest);
        } else {
            proxyRequest = new BasicHttpRequest(_method, _requestUri);
        }
        return proxyRequest;
    }

    private HttpRequest newProxyRequestWithEntity(String method, String proxyRequestUri, HttpServletRequest servletRequest) throws IOException {
        HttpEntityEnclosingRequest eProxyRequest =
                new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
        /**
         * Add the input entity (streamed)<br/>
         * note: we don't bother ensuring we close the servletInputStream since the container handles it
         */
        eProxyRequest.setEntity(
                new InputStreamEntity(servletRequest.getInputStream(), getContentLength(servletRequest)));
        return eProxyRequest;
    }

    /**
     * Get the header value as a long in order to more correctly proxy very large requests
     *
     * @param request
     * @return
     */
    private long getContentLength(HttpServletRequest request) {
        String contentLengthHeader = request.getHeader("Content-Length");
        if (contentLengthHeader != null) {
            return Long.parseLong(contentLengthHeader);
        }
        return -1L;
    }

    /**
     * Reads the request URI from {@code servletRequest} and rewrites it, considering targetUri.
     * It's used to make the new request.
     *
     * @param servletRequest
     */
    private String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
        StringBuilder uri = new StringBuilder(500);
        uri.append(/**getTargetUri(servletRequest)*/targetUri);
        /**
         * Handle the path given to the servlet.<br/>
         * <code>ex: /my/path.html</code>
         */
        String pathInfo = servletRequest.getPathInfo();
        if (pathInfo != null) {
            /**
             * getPathInfo() returns decoded string, so we need encodeUriQuery to encode "%" characters
             */
            uri.append(proxyQueryEncoder.encodeUriQuery(pathInfo, true));
        }
        /**
         * Handle the query string & fragment.<br/>
         * <code>ex:(following '?'): name=value&foo=bar#fragment</code>
         */
        String queryString = servletRequest.getQueryString();
        String fragment = null;
        /**
         * split off fragment from queryString, updating queryString if found
         */
        if (queryString != null) {
            int fragIdx = queryString.indexOf('#');
            if (fragIdx >= 0) {
                fragment = queryString.substring(fragIdx + 1);
                queryString = queryString.substring(0, fragIdx);
            }
        }

        if (queryString != null && queryString.length() > 0) {
            uri.append('?');
            /**
             * queryString is not decoded, so we need encodeUriQuery not to encode "%" characters, to avoid double-encoding
             */
            uri.append(proxyQueryEncoder.encodeUriQuery(queryString, false));
        }

        if (doSendUrlFragment && fragment != null) {
            uri.append('#');
            /**
             * fragment is not decoded, so we need encodeUriQuery not to encode "%" characters, to avoid double-encoding
             */
            uri.append(proxyQueryEncoder.encodeUriQuery(fragment, false));
        }
        return uri.toString();
    }

    /**
     * Reads the request URI from {@code servletRequest} and rewrites it, considering targetUri.<br/>
     * It's used to make the new request.
     *
     * @param targetUri
     * @param servletPath
     * @param contextPath
     * @param pathInfo
     * @param queryString
     * @return
     */
    public String rewriteUriFromRequest(String targetUri, String servletPath, String contextPath, String pathInfo, String queryString, boolean doSendUrlFragment) {
        return rewriteUriFromRequest(targetUri,
                servletPath, contextPath,
                pathInfo, queryString,
                doSendUrlFragment, proxyQueryEncoder);
    }

    /**
     * Reads the request URI from {@code servletRequest} and rewrites it, considering targetUri.<br/>
     * It's used to make the new request.
     *
     * @param targetUri
     * @param servletPath
     * @param contextPath
     * @param pathInfo
     * @param queryString
     * @return
     */
    static String rewriteUriFromRequest(String targetUri, String servletPath, String contextPath, String pathInfo, String queryString, boolean doSendUrlFragment, ProxyQueryEncoder proxyQueryEncoder) {
        StringBuilder uri = new StringBuilder(500);
        if (null != targetUri && !targetUri.isEmpty()) {
            /**
             * make a absolute url ...
             */
            uri.append(targetUri);
        } else {
            /**
             * .. or relative url
             */
            if (null != servletPath) {
                uri.append(servletPath);
            }
            if (null != contextPath) {
                uri.append(contextPath);
            }
        }

        /**
         * Handle the path given to the servlet.<br/>
         * <code>ex: /my/path.html</code>
         */
        if (pathInfo != null) {
            /**
             * getPathInfo() returns decoded string, so we need encodeUriQuery to encode "%" characters
             */
            uri.append(proxyQueryEncoder.encodeUriQuery(pathInfo, true));
        }
        /**
         * Handle the query string & fragment.<br/>
         * <code>ex:(following '?'): name=value&foo=bar#fragment</code>
         */
        String fragment = null;
        /**
         * split off fragment from queryString, updating queryString if found
         */
        if (queryString != null) {
            int fragIdx = queryString.indexOf('#');
            if (fragIdx >= 0) {
                fragment = queryString.substring(fragIdx + 1);
                queryString = queryString.substring(0, fragIdx);
            }
        }

        if (queryString != null && queryString.length() > 0) {
            uri.append('?');
            /**
             * queryString is not decoded, so we need encodeUriQuery not to encode "%" characters, to avoid double-encoding
             */
            uri.append(proxyQueryEncoder.encodeUriQuery(queryString, false));
        }

        if (doSendUrlFragment && fragment != null) {
            uri.append('#');
            /**
             * fragment is not decoded, so we need encodeUriQuery not to encode "%" characters, to avoid double-encoding
             */
            uri.append(proxyQueryEncoder.encodeUriQuery(fragment, false));
        }
        return uri.toString();
    }

    CharSequence encodeUriQuery(CharSequence charSequence, boolean encodePercent) {
        return proxyQueryEncoder.encodeUriQuery(charSequence, encodePercent);
    }


}
