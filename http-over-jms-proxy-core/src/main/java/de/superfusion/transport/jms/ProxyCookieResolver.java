package de.superfusion.transport.jms;

import java.net.HttpCookie;
import java.util.*;

public class ProxyCookieResolver {
    ProxyCookiePrefixer proxyCookiePrefixer;

    ProxyCookieResolver(ProxyCookiePrefixer proxyCookiePrefixer) {
        this.proxyCookiePrefixer = proxyCookiePrefixer;
    }

    /**
     * By default when parsing cookies the version is set to <code>0</code>.<br/>
     * This will cause the {@link HttpCookie#toString()} to output the cookie as netscape format</br>
     * This method sets the version to <code>1</code> which will cause the string representation to be
     * <code>RFC2965</code> compliant.<br/>
     *
     * @param cookieHeaderValueString
     * @return
     * @throws IllegalArgumentException when cookie string is invalid
     */
    static List<HttpCookie> parseCookies(String cookieHeaderValueString) {
//        List<HttpCookie> parse = new ArrayList<>();
//        if (null != cookieHeaderValueString && !cookieHeaderValueString.isEmpty()) {
//            String[] cookieValues = cookieHeaderValueString.split("[;,]");
//            for (String cookieValue : cookieValues) {
//                if (null != cookieValue && !cookieValue.isEmpty()) {
//                    List<HttpCookie> cookies = HttpCookie.parse(cookieValue);
//                    parse.addAll(cookies);
//                }
//            }
//        }
//        List<HttpCookie> parse;
//        try {
//            parse = CookieParser.parseHttpCookie(cookieHeaderValueString);
//        } catch (MalformedCookieException e) {
//            e.printStackTrace();
//            throw new IllegalArgumentException("Cookie not parsable: " + cookieHeaderValueString, e);
//        }
        List<HttpCookie> parse = HttpCookie.parse(
                /**
                 * If we set to 'Set-Cookie2' the parser will handles it as a (rfc2965/2109) multi cookie value,
                 * otherwise it will parse only the first one.
                 * */
                // "Set-Cookie: "
                cookieHeaderValueString);
        if (!parse.isEmpty() && parse.get(0).getVersion() < 1 && parse.get(0).getValue().contains(";")) {
            parse.clear();
            String[] cookies = cookieHeaderValueString.split(";");
            for (String cookie : cookies) {
                /**
                 * Parse as Version 0
                 */
                List<HttpCookie> cookieList = HttpCookie.parse(
                        /** Set 'Set-Cookie' to help the parser to guess the cookie version */
                        "Set-Cookie:"
                                + cookie
                );
                parse.addAll(cookieList);
            }
        }
        // parse.forEach(cookie -> cookie.setVersion(1));
        return parse;
    }

    /**
     * Take any client cookies that were originally from the proxy and prepare them to send to the
     * proxy.  This relies on cookie headers being set correctly according to RFC 6265 Sec 5.4.
     * This also blocks any local cookies from being sent to the proxy.
     */
    String getRealCookie(String cookieValue) {
        return getRealCookie(null, cookieValue, false);
    }

    /**
     * Take any client cookies that were originally from the proxy and prepare them to send to the
     * proxy.  This relies on cookie headers being set correctly according to RFC 6265 Sec 5.4.
     * This also blocks any local cookies from being sent to the proxy.
     */
    String getRealCookie(String requestUrl, String cookieValue, boolean skipNonPrefixed) {
        StringBuilder escapedCookie = new StringBuilder();
        if (Config.Proxy.SEND_ONLY_PREFIXED_COOKIE()) {
            /**
             * If there are cookies with the same name, then we must transmit
             * that prefixed one.<br/>
             */
            HashMap<String, String> cookieMap = new LinkedHashMap<>();
            Set<String> prefixed = new HashSet<>();
            List<HttpCookie> cookies = parseCookies(cookieValue);

            keepOnlyCookieWithShortenedPath(requestUrl, cookies);

            for (HttpCookie cookie : cookies) {
//            if (null != requestUrl && (null != cookie.getPath() && !cookie.getPath().isEmpty())) {
//                if (requestUrl.startsWith(cookie.getPath())) {
//                    continue;
//                }
//            }
                String cookieName = cookie.getName();
                // don't set the domain
                cookie.setDomain(null);
                if (cookieName.startsWith(proxyCookiePrefixer.prefix)) {
                    cookieName = cookieName.substring(proxyCookiePrefixer.prefix.length());
                    prefixed.add(cookieName);
                    String cv = cookie.toString();
                    cv = cv.substring(cv.indexOf("="));
                    cookieMap.put(cookieName, cv);
                } else if (skipNonPrefixed) {
                    if (Config.Logging.isLogActive()) {
                        System.out.println("Skip Cookie " + cookieName + "=" + cookieValue);
                    }
                } else if (!prefixed.contains(cookieName)) {
                    String cv = cookie.toString();
                    cv = cv.substring(cv.indexOf("="));
                    cookieMap.put(cookieName, cv);
                }
            }
            cookieMap.forEach((key, value) -> {
                if (escapedCookie.length() > 0) {
                    // (rfc2965/2109) -  must use comma
                    escapedCookie.append(", ");
                }
                escapedCookie.append(key + value);
            });
        } else {
            String cookies[] = cookieValue.split("[;,]");
            for (String cookie : cookies) {
                String cookieSplit[] = cookie.split("=");
                if (cookieSplit.length == 2) {
                    String cookieName = cookieSplit[0].trim();
                    if (cookieName.startsWith(proxyCookiePrefixer.prefix)) {
                        cookieName = cookieName.substring(proxyCookiePrefixer.prefix.length());
                        if (escapedCookie.length() > 0) {
                            escapedCookie.append("; ");
                        }
                        escapedCookie.append(cookieName).append("=").append(cookieSplit[1].trim());
                    }
                }
            }
        }
        return escapedCookie.toString();
    }

    static void keepOnlyCookieWithShortenedPath(String requestUrlString, List<HttpCookie> cookies) {
        if (!Config.Proxy.REDUCE_COOKIE_PATH_TO_1_CHILD())
            return;
        if (null == cookies)
            return;

        String requestUrl = removeAuthorityFromUrl(requestUrlString);

        String pathForEmpty = computeCookiePath(requestUrl);

        /**
         * Prevent cookies with sub path.<br/>
         * e.g. <br/>
         * Is allowed: <code>/app</code> <br/>
         * Is not allowed: <code>/app/test/demo1</code> <br/>
         */
        HashMap<String, Map<String, HttpCookie>> shortesPath = new LinkedHashMap<>();
        List<HttpCookie> removeDuplicatesCookies = new ArrayList<>();
        for (HttpCookie cookie : cookies) {
            String path = cookie.getPath();
            String pathId = computeCookiePath(path);

            shortesPath.putIfAbsent(pathId, new LinkedHashMap<>());
            Map<String, HttpCookie> pathByName = shortesPath.get(pathId);
            pathByName.putIfAbsent(cookie.getName(), cookie);
            HttpCookie fromPathByName = pathByName.get(cookie.getName());
            String fromPathByName_path = null != fromPathByName.getPath() ? fromPathByName.getPath() : "";
            String cookie_path = null != cookie.getPath() ? cookie.getPath() : "";
            if (cookie_path.length() < fromPathByName_path.length()) {
                pathByName.put(cookie.getName(), cookie);
                removeDuplicatesCookies.add(fromPathByName);
            }
        }
        cookies.removeAll(removeDuplicatesCookies);
        cookies.clear();
        shortesPath.forEach((cookiePath, cookiesByPath) -> {
            cookiesByPath.forEach((cookieName, cookie) -> {
                cookie.setPath(null == cookiePath || cookiePath.isEmpty() ? pathForEmpty : cookiePath);
                cookies.add(cookie);
            });
        });

        /**
         * Now shorten path to a single segment.<br/>
         * eg. make <code>/app</code> from <code>/app/test/</code>.<br/>
         * This helps keeping JSESSIONS alive.<br/>
         */
        for (HttpCookie cookie : cookies) {
            if (null != cookie.getPath() && !cookie.getPath().isEmpty()) {
                String path = computeCookiePath(cookie.getPath());
                path = null != path ? path : pathForEmpty;
                cookie.setPath(path);
            }
        }
//        System.out.println("###################################" + cookies);
    }


    static String removeAuthorityFromUrl(String requestUrlString) {
        String requestUrl = requestUrlString;
        int protIx;
        int nextSep;
        if (-1 != (protIx = requestUrl.indexOf("://")) && -1 != (nextSep = requestUrl.indexOf("/", protIx + 3))) {
            requestUrl = requestUrl.substring(nextSep);
        }
        return requestUrl;
    }

    static String computeCookiePath(String path) {
        if (null == path)
            return path;
        boolean shortenCookiePath = true;
        String _path = path;
        if (shortenCookiePath) {
            int firstIndex = _path.indexOf("/");
            if (-1 != firstIndex) {
                int secondIndex = _path.indexOf("/", firstIndex + 1);
                if (-1 != secondIndex) {
                    _path = path.substring(0, secondIndex);
                }
            }
        }
        return _path;
    }

}
