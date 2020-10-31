package de.superfusion.transport.jms;

import org.apache.http.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.util.*;

public class ProxyInterceptorHttpResponse {
    private ProxyConfig p;
    private ProxyCookiePrefixer proxyCookiePrefixer;
    private HeaderValueProvider headerValueProvider;

    public ProxyInterceptorHttpResponse(ProxyConfig p, ProxyCookieResolver proxyCookieResolver) {
        this.p = p;
        this.proxyCookiePrefixer = proxyCookieResolver.proxyCookiePrefixer;
    }

    public void intercept(HttpRequest proxyRequest, HttpResponse proxyResponse,
                          HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception {
        /**
         * Pass the response code. This method with the "reason phrase" is deprecated <br/>
         * but it's the only way to pass the reason along too.
         */
        int statusCode = proxyResponse.getStatusLine().getStatusCode();
        // noinspection deprecation
        servletResponse.setStatus(statusCode, proxyResponse.getStatusLine().getReasonPhrase());

        /**
         * Copying response headers to make sure SESSIONID or other Cookie which comes from the remote<br/>
         * server will be saved in client when the proxied url was redirected to another one.<br/>
         * See issue [#51](https://github.com/mitre/HTTP-Proxy-Servlet/issues/51)
         */
        copyResponseHeaders(proxyResponse, servletRequest, servletResponse);

        if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
            /**
             * 304 needs special handling.  See: http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304 <br/>
             * Don't send body entity/content!
             */
            servletResponse.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
        } else {
            /**
             * Send the content to the client
             */
            copyResponseEntity(proxyResponse, servletResponse, proxyRequest, servletRequest);
        }

    }

    /**
     * Copy response body data (the entity) from the proxy to the servlet client.
     */
    void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse, HttpRequest proxyRequest, HttpServletRequest servletRequest) throws IOException {
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
            OutputStream servletOutputStream = servletResponse.getOutputStream();
            entity.writeTo(servletOutputStream);
        }
    }


    /**
     * Copy proxied response headers back to the servlet client.
     */
    void copyResponseHeaders(HttpResponse proxyResponse, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        for (Header header : proxyResponse.getAllHeaders()) {
            copyResponseHeader(servletRequest, servletResponse, header);
        }
    }

    /**
     * Copy a proxied response header back to the servlet client.
     * This is easily overwritten to filter out certain headers if desired.
     */
    void copyResponseHeader(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Header header) {
        String headerName = header.getName();
        if (ProxyHopHeader.isHopHeader(headerName))
            return;
        String headerValue = header.getValue();
        headerValue = getHeaderValueProvider().getValue(headerName, headerValue, servletRequest, servletResponse);
        if (null != headerValue)
            servletResponse.addHeader(headerName, headerValue);
    }

    /**
     * This method may be overridden to return the modified response header parameter value<br/>
     * This method does rename the proxy cookie.<br/>
     * This method does replace the header <code>Location</code> with the <code>targetUri</code>.<br/>
     *
     * @param headerName
     * @param headerValue
     * @param servletRequest
     * @param servletResponse
     * @return The header parameter value to set or <code>null</code> to not set the header parameter at all
     */
    String copyResponseHeaderValueForProxyResponse(String headerName, String headerValue, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE) ||
                headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE2)) {
            copyProxyCookie(servletRequest, servletResponse, headerValue);
            return null;
        } else if (headerName.equalsIgnoreCase(HttpHeaders.LOCATION)) {
            /**
             * LOCATION Header may have to be rewritten.
             */
            headerValue = rewriteUrlFromResponse(servletRequest, headerValue);
        }
        return headerValue;
    }

    /**
     * Copy cookie from the proxy to the servlet client.
     * Replaces cookie path to local path and renames cookie to avoid collisions.
     *
     * @param headerValue
     * @param servletPath         Should be {@link HttpServletRequest#getServletPath()}
     * @param contextPath         Should be {@link HttpServletRequest#getContextPath()}
     * @param doPreserveCookies
     * @param preserveCookiePath
     * @param proxyCookiePrefixer
     */
    private List<Cookie> copyProxyCookie(String headerValue, String servletPath, String contextPath, boolean doPreserveCookies, boolean preserveCookiePath, ProxyCookiePrefixer proxyCookiePrefixer, String requestUri) {
        List<Cookie> cookieList = new ArrayList<>();
        List<HttpCookie> cookies = ProxyCookieResolver.parseCookies(headerValue);
        if (cookies.isEmpty()) {
            return cookieList;
        }
        // TODO this is required only when on proxying consumer side
        boolean donCopyCookiesFromSupPath_ThisShouldSave_JSESSIONID = Config.Producer.CONSUMER_IS_PROXY();
        if (donCopyCookiesFromSupPath_ThisShouldSave_JSESSIONID) {
            String requestUrl = ProxyCookieResolver.removeAuthorityFromUrl(requestUri);
            int nexIx;
            if (-1 != (nexIx = requestUrl.indexOf("/", 1))
                    && -1 != (requestUrl.indexOf("/", nexIx + 1))
            ) {
                /**
                 * Don't set Cookies for sub-path.<br/>
                 * This will otherwise have a huge negative impact when handling with JSESSIONID.<br/>
                 */
                cookies.clear();
                return cookieList;
            }
        }

        // TODO fix this: setting the path to '/' will not work when cookie should be reused after a redirect 302, so the path should be reused from the cookie
        /**
         * path starts with / or is empty string
         */
        String path = (null != contextPath ? contextPath : "");
        /**
         * servlet path starts with / or is empty string
         */
        path += (null != servletPath ? servletPath : "");
        if (path.isEmpty()) {
            path = "/";
        }

        if (!doPreserveCookies) {
            ProxyCookieResolver.keepOnlyCookieWithShortenedPath(requestUri, cookies);
        }

        for (HttpCookie cookie : cookies) {
            /**
             * set cookie name prefixed w/ a proxy value so it won't collide w/ other cookies
             */
            String proxyCookieName = doPreserveCookies ? cookie.getName() : proxyCookiePrefixer.getCookieNamePrefix(cookie.getName());
            Cookie servletCookie = new Cookie(proxyCookieName, cookie.getValue());
            servletCookie.setComment(cookie.getComment());
            servletCookie.setMaxAge((int) cookie.getMaxAge());
            if (preserveCookiePath) {
                servletCookie.setPath(cookie.getPath());
            } else {
                /**
                 * set to the path of the proxy servlet
                 */
                servletCookie.setPath(path);
            }
            /**
             * don't set cookie domain<br/>
             * <code>servletCookie.setDomain(cookie.getDomain());</code><br/>
             * But what if we use the proxy uri domain...?
             */

            servletCookie.setSecure(cookie.getSecure());
            servletCookie.setVersion(cookie.getVersion());
            servletCookie.setHttpOnly(cookie.isHttpOnly());
            cookieList.add(servletCookie);
        }
        return cookieList;
    }

    /**
     * Copy cookie from the proxy to the servlet client.
     * Replaces cookie path to local path and renames cookie to avoid collisions.
     */
    void copyProxyCookie(HttpServletRequest servletRequest, HttpServletResponse servletResponse, String headerValue) {
        List<Cookie> cookieList = copyProxyCookie(
                headerValue,
                servletRequest.getServletPath(),
                servletRequest.getContextPath(),
                servletRequest.getRequestURI());
        for (Cookie servletCookie : cookieList) {
            servletResponse.addCookie(servletCookie);
        }
    }

    /**
     * Copy cookie from the proxy to the servlet client.
     * Replaces cookie path to local path and renames cookie to avoid collisions.
     */
    List<Cookie> copyProxyCookie(String headerValue, String servletPath, String contextPath, String requestUri) {
        List<Cookie> cookieList = copyProxyCookie(
                headerValue,
                servletPath,
                contextPath,
                p.isDoPreserveCookies()/**doPreserveCookies*/,
                p.isPreserveCookiePath()/**preserveCookiePath*/,
                proxyCookiePrefixer,
                requestUri);
        return cookieList;
    }

    /**
     * For a redirect response from the target server, this translates {@code theUrl} to redirect to
     * and translates it to one the original client can use.
     */
    /**
     * @param locationUrl The URL from HTTP Header Parameter <code>Location</code>
     * @param targetUri
     * @param requestURL  Should be {@link HttpServletRequest#getRequestURL()} (no query)
     * @return
     */
    String rewriteUrlFromResponse(String locationUrl, String targetUri, StringBuffer requestURL) {
        String newURL = locationUrl;
        if (locationUrl.startsWith(targetUri)) {
            /**
             * The URL points back to the back-end server.<br/>
             * Instead of returning it verbatim we replace the target path with our<br/>
             * source path in a way that should instruct the original client to<br/>
             * request the URL pointed through this Proxy.<br/>
             * We do this by taking the current request and rewriting the path part<br/>
             * using this servlet's absolute path and the path from the returned URL<br/>
             * after the base target URL.
             */
            StringBuffer curUrl = requestURL;//no query
            int pos;
            /**
             * Skip the protocol part
             */
            if ((pos = curUrl.indexOf("://")) >= 0) {
                /**
                 * Skip the authority part<br/>
                 * + 3 to skip the separator between protocol and authority
                 */
                if ((pos = curUrl.indexOf("/", pos + 3)) >= 0) {
                    /**
                     * Trim everything after the authority part.
                     */
                    curUrl.setLength(pos);
                }
            }
            curUrl.append(locationUrl, targetUri.length(), locationUrl.length());
            newURL = curUrl.toString();
        }
        return newURL;
    }

    /**
     * For a redirect response from the target server, this translates {@code theUrl} to redirect to
     * and translates it to one the original client can use.
     */
    String rewriteUrlFromResponse(HttpServletRequest servletRequest, String theUrl) {
        //TODO document example paths
        final String targetUri = p.getTargetUri();//getTargetUri(servletRequest);
        if (theUrl.startsWith(targetUri)) {
            /*-
             * The URL points back to the back-end server.
             * Instead of returning it verbatim we replace the target path with our
             * source path in a way that should instruct the original client to
             * request the URL pointed through this Proxy.
             * We do this by taking the current request and rewriting the path part
             * using this servlet's absolute path and the path from the returned URL
             * after the base target URL.
             */
            StringBuffer curUrl = servletRequest.getRequestURL();//no query
            int pos;
            // Skip the protocol part
            if ((pos = curUrl.indexOf("://")) >= 0) {
                // Skip the authority part
                // + 3 to skip the separator between protocol and authority
                if ((pos = curUrl.indexOf("/", pos + 3)) >= 0) {
                    // Trim everything after the authority part.
                    curUrl.setLength(pos);
                }
            }
            // Context path starts with a / if it is not blank
            curUrl.append(servletRequest.getContextPath());
            // Servlet path starts with a / if it is not blank
            curUrl.append(servletRequest.getServletPath());
            curUrl.append(theUrl, targetUri.length(), theUrl.length());
            theUrl = curUrl.toString();
        }
        return theUrl;
    }


    /**
     * For a redirect response from the target server, this translates {@code theUrl} to redirect to
     * and translates it to one the original client can use.
     */
    protected String _rewriteUrlFromResponse(HttpServletRequest servletRequest, String locationUrl) {
        final String targetUri = p.getTargetUri();//getTargetUri(servletRequest);
        return rewriteUrlFromResponse(locationUrl, targetUri, servletRequest.getRequestURL());
    }

    public void setHeaderValueProvider(HeaderValueProvider headerValueProvider) {
        this.headerValueProvider = headerValueProvider;
    }

    public HeaderValueProvider getHeaderValueProvider() {
        if (null == this.headerValueProvider) {
            this.headerValueProvider = this::copyResponseHeaderValueForProxyResponse;
        }
        return headerValueProvider;
    }

    interface HeaderValueProvider {
        String getValue(String headerName, String headerValue, HttpServletRequest servletRequest, HttpServletResponse servletResponse);
    }

}
