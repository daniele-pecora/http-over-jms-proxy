package de.superfusion.transport.jms;

import org.apache.http.HttpHeaders;
import org.apache.http.cookie.SM;

import javax.servlet.http.Cookie;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Some helpers for preparing http request and http response from and to jms messages.<br/>
 * This will only be used when the {@link Config.Consumer#CONSUMER_IS_PROXY()} is set.<br/>
 *
 * @author daniele
 */
public class HTTPRequestResponseMapper {
    static boolean allowAlsoRelativeRequestURLPaths = true;

    private static ProxyCookieResolver proxyCookieResolver = new ProxyCookieResolver(new ProxyCookiePrefixer());
    private static ProxyConfig proxyConfig = new ProxyConfig(new EmptyServletConfig());

    public static boolean prepareSimplePlainHTTPResponse(String originalRequestURL, HTTPMapper.SimplePlainHTTPRequest rq, HTTPMapper.SimplePlainHTTPResponse rs) {
        boolean madeAnyReplacements = false;
        if (Config.Consumer.CONSUMER_IS_PROXY()) {
            String targetUri = Config.Consumer.TARGET_URI();
            if (headersSimplePlainHTTPResponse(targetUri, originalRequestURL, rq, rs)) {
                madeAnyReplacements = true;
            }
        }
        return madeAnyReplacements;
    }

    private static boolean headersSimplePlainHTTPResponse(
            String targetUri,
            String originalRequestURL,
            HTTPMapper.SimplePlainHTTPRequest rq,
            HTTPMapper.SimplePlainHTTPResponse rs) {
        ProxyInterceptorHttpResponse interceptorHttpResponse =
                new ProxyInterceptorHttpResponse(proxyConfig, proxyCookieResolver);
        boolean madeAnyChange = false;
        String contextPath = rq.contextPath;
        String servletPath = rq.servletPath;
        for (HTTPMapper.HeaderVar header : rs.headers) {
            String headerName = header.getName();
            if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE) ||
                    headerName.equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE2)) {
                String headerValue = header.getValue();
                List<Cookie> cookies = interceptorHttpResponse
                        .copyProxyCookie(
                                headerValue,
                                servletPath,
                                contextPath,
                                rq.requestURL);
                StringBuilder sb = new StringBuilder();
                if (!cookies.isEmpty()) {
                    for (Cookie item : cookies) {
                        HttpCookie c = new HttpCookie(item.getName(), item.getValue());
                        c.setComment(item.getComment());
                        /**
                         * don't set cookie domain<br/>
                         * <code>c.setDomain(item.getDomain());</code><br/>
                         * But what if we use the proxy uri domain...?
                         */
                        c.setHttpOnly(item.isHttpOnly());
                        c.setMaxAge(item.getMaxAge());
                        c.setPath(item.getPath());
                        c.setSecure(item.getSecure());
                        c.setVersion(item.getVersion());
                        if (sb.length() > 0) {
                            sb.append(";");
                        }
                        sb.append(c.toString());
                    }
                    headerValue = sb.toString();
                    header.setValue(headerValue);
                } else {
                    header.setName("X-Renamed-" + headerName);
                }
                madeAnyChange = true;
//                if (Config.Logging.isLogCookiesOnly()) {
                System.out.println("-----------------------------------------------------------");
                System.out.println("Rewrite " + header.getName() + " for " + rq.requestURL);
                System.out.println("-  " + sb.toString());
//                }
            } else if (headerName.equalsIgnoreCase(HttpHeaders.LOCATION)) {
                /**
                 * LOCATION Header may have to be rewritten.<br/>
                 * So it points to our proxy (e.g. https://localhost:8443)
                 */
                String locationUrl = header.getValue();
                StringBuffer requestURL = new StringBuffer(originalRequestURL);
                String headerValue = interceptorHttpResponse
                        .rewriteUrlFromResponse(locationUrl, targetUri, requestURL);
                header.setValue(headerValue);
                madeAnyChange = true;
            }
        }
        return madeAnyChange;
    }


    public static boolean prepareSimplePlainHTTPRequest(HTTPMapper.SimplePlainHTTPRequest rq) {
        boolean madeAnyReplacements = false;
        if (Config.Consumer.CONSUMER_IS_PROXY()) {
            String targetUri = Config.Consumer.TARGET_URI();
            if (replaceRequestUri(rq, targetUri)) {
                madeAnyReplacements = true;
            }
            if (headersSimplePlainHTTPRequest(targetUri, rq)) {
                madeAnyReplacements = true;
            }
        }
        return madeAnyReplacements;
    }

    private static boolean headersSimplePlainHTTPRequest(String targetUri, HTTPMapper.SimplePlainHTTPRequest rq) {
        boolean madeAnyChange = false;
        for (HTTPMapper.HeaderVar header : rq.headers) {
            String headerName = header.getName();
            /**
             * In case the proxy host is running multiple virtual servers,<br/>
             * rewrite the Host header to ensure that we get content from<br/>
             * the correct virtual server
             */
            if (!Config.Consumer.TARGET_URI_doPreserveHost() && headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
                String headerValue = getTargetAuthority(targetUri);
                header.setValue(headerValue);
                madeAnyChange = true;
            } else if (!Config.Consumer.TARGET_URI_doPreserveCookies()
                    && (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.COOKIE) || headerName.equalsIgnoreCase(SM.COOKIE2))) {
                String headerValue = header.getValue();
                String requestUrl = rq.requestURL;
                // send only prefixed cookies...

                headerValue = proxyCookieResolver.getRealCookie(requestUrl, headerValue, true);
                header.setValue(headerValue);
                System.out.println("-----------------------------------------------------------");
                System.out.println("Send Header " + header.getName() + " for " + rq.requestURL);
                System.out.println("-  " + headerValue);
                madeAnyChange = true;
            }
        }
        return madeAnyChange;
    }

    private static String getTargetAuthority(String targetUri) {
        URI targetUriObj;
        try {
            targetUriObj = new URI(targetUri);
            return targetUriObj.getAuthority();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    static boolean replaceRequestUri(HTTPMapper.SimplePlainHTTPRequest request, String targetUri) {
        String newRequestURI = ProxyHttpRequest.rewriteUriFromRequest(
                allowAlsoRelativeRequestURLPaths ? "" : targetUri,
                request.servletPath,
                request.contextPath,
                request.pathInfo,
                request.queryString,
                Config.Consumer.TARGET_URI_doSendUrlFragment(),
                ProxyHttpRequest.proxyQueryEncoder
        );
        if (!request.uri.equals(newRequestURI)) {
            request.uri = newRequestURI;
            return true;
        }
        return false;
    }


}
