package de.superfusion.transport.jms;

import javax.servlet.ServletConfig;

public class ProxyConfig {
    /**
     * A boolean parameter name to enable logging of input and target URLs to the servlet log.
     */
    public static final String P_LOG = "log";

    /**
     * A boolean parameter name to enable forwarding of the client IP
     */
    public static final String P_FORWARDEDFOR = "forwardip";

    /**
     * A boolean parameter name to keep HOST parameter as-is
     */
    public static final String P_PRESERVEHOST = "preserveHost";

    /**
     * A boolean parameter name to keep COOKIES as-is
     */
    public static final String P_PRESERVECOOKIES = "preserveCookies";

    /**
     * A boolean parameter name to have auto-handle redirects
     */
    public static final String P_HANDLEREDIRECTS = "http.protocol.handle-redirects"; // ClientPNames.HANDLE_REDIRECTS

    /**
     * A integer parameter name to set the socket connection timeout (millis)
     */
    public static final String P_CONNECTTIMEOUT = "http.socket.timeout"; // CoreConnectionPNames.SO_TIMEOUT

    /**
     * A integer parameter name to set the socket read timeout (millis)
     */
    public static final String P_READTIMEOUT = "http.read.timeout";

    /**
     * A boolean parameter to set if self signer SSL certs should be accepted or not
     */
    public static final String P_IGNORE_SSL_CERT = "ignoreSSLCerts";

    /**
     * A boolean parameter name to keep the path of COOKIES as-is.
     * This should be used if you set the proxy to a global path like '/*'.<br/>
     */
    public static final String P_PRESERVECOOKIES_PATH = "preserveCookiePath";

    /**
     * The parameter name for the target (destination) URI to proxy to.
     */
    protected static final String P_TARGET_URI = "targetUri";

    private ServletConfig servletConfig;
    /* MISC */
    /**
     * User agents shouldn't send the url fragment but what if it does?
     */
    private boolean doSendUrlFragment = true;
    private boolean doLog = false;
    private boolean doForwardIP = true;
    private boolean doPreserveHost = false;
    private boolean doPreserveCookies = false;
    private boolean doHandleRedirects = false;
    private int connectTimeout = -1;
    private int readTimeout = -1;
    private boolean ignoreSSLCerts;
    private boolean preserveCookiePath;
    private String targetUri;

    public ProxyConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
        initConfig();
    }

    /**
     * Reads a configuration parameter. By default it reads servlet init parameters but
     * it can be overridden.
     */
    protected String getConfigParam(String key) {
        String valueFromWebXML = this.servletConfig.getInitParameter(key);
        return Config.System_getProperty("de.superfusion.transport.jms.PROXY." + key, valueFromWebXML);
    }

    private String getConfigParamString(String key, String defaultValue) {
        String val = getConfigParam(key);
        if (null == val) {
            return defaultValue;
        }
        return val;
    }

    private int getConfigParamInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getConfigParam(key));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    private boolean getConfigParamBoolean(String key, boolean defaultValue) {
        if (0 == "true".compareToIgnoreCase(String.valueOf(getConfigParam(key)))) {
            return true;
        } else if (0 == "false".compareToIgnoreCase(String.valueOf(getConfigParam(key)))) {
            return false;
        }
        return defaultValue;
    }

    private void initConfig() {
        String doLogStr = getConfigParam(P_LOG);
        if (doLogStr != null) {
            this.doLog = Boolean.parseBoolean(doLogStr);
        }

        String doForwardIPString = getConfigParam(P_FORWARDEDFOR);
        if (doForwardIPString != null) {
            this.doForwardIP = Boolean.parseBoolean(doForwardIPString);
        }

        String preserveHostString = getConfigParam(P_PRESERVEHOST);
        if (preserveHostString != null) {
            this.doPreserveHost = Boolean.parseBoolean(preserveHostString);
        }

        String preserveCookiesString = getConfigParam(P_PRESERVECOOKIES);
        if (preserveCookiesString != null) {
            this.doPreserveCookies = Boolean.parseBoolean(preserveCookiesString);
        }

        String handleRedirectsString = getConfigParam(P_HANDLEREDIRECTS);
        if (handleRedirectsString != null) {
            this.doHandleRedirects = Boolean.parseBoolean(handleRedirectsString);
        }

        String connectTimeoutString = getConfigParam(P_CONNECTTIMEOUT);
        if (connectTimeoutString != null) {
            this.connectTimeout = Integer.parseInt(connectTimeoutString);
        }

        String readTimeoutString = getConfigParam(P_READTIMEOUT);
        if (readTimeoutString != null) {
            this.readTimeout = Integer.parseInt(readTimeoutString);
        }

        String ignoreSSLCertString = getConfigParam(P_IGNORE_SSL_CERT);
        if (ignoreSSLCertString != null) {
            this.ignoreSSLCerts = Boolean.parseBoolean(ignoreSSLCertString);
        }

        String preserveCookiesPathString = getConfigParam(P_PRESERVECOOKIES_PATH);
        if (preserveCookiesPathString != null) {
            this.preserveCookiePath = Boolean.parseBoolean(preserveCookiesPathString);
        }
        String targetUriString = getConfigParam(P_TARGET_URI);
        if (doForwardIPString != null) {
            this.targetUri = targetUriString;
        }

    }

    public boolean isDoLog() {
        return getConfigParamBoolean(P_LOG, doLog);
    }

    public boolean isDoForwardIP() {
        return getConfigParamBoolean(P_FORWARDEDFOR, doForwardIP);
    }

    public boolean isDoSendUrlFragment() {
        return doSendUrlFragment;
    }

    public boolean isDoPreserveHost() {
        return getConfigParamBoolean(P_PRESERVEHOST, doPreserveHost);
    }

    public boolean isDoPreserveCookies() {
        return getConfigParamBoolean(P_PRESERVECOOKIES, doPreserveCookies);
    }

    public boolean isDoHandleRedirects() {
        return getConfigParamBoolean(P_HANDLEREDIRECTS, doHandleRedirects);
    }

    public int getConnectTimeout() {
        return getConfigParamInt(P_CONNECTTIMEOUT, connectTimeout);
    }

    public int getReadTimeout() {
        return getConfigParamInt(P_READTIMEOUT, readTimeout);
    }

    public boolean isIgnoreSSLCerts() {
        return getConfigParamBoolean(P_IGNORE_SSL_CERT, ignoreSSLCerts);
    }

    public boolean isPreserveCookiePath() {
        return getConfigParamBoolean(P_PRESERVECOOKIES_PATH, preserveCookiePath);
    }

    public String getTargetUri() {
        return getConfigParamString(P_TARGET_URI, targetUri);
    }
}
