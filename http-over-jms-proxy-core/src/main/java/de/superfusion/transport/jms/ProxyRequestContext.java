package de.superfusion.transport.jms;

import javax.servlet.http.HttpServletRequest;

class ProxyRequestContext {
    public final String servletPath;
    public final String contextPath;
    public final String pathInfo;
    public final String queryString;
    public final String requestURL;

    /**
     * @param requestURL  HttpServletRequest#getRequestURL()
     * @param servletPath HttpServletRequest#getServletPath()
     * @param contextPath HttpServletRequest#getContextPath()
     * @param pathInfo    HttpServletRequest#getPathInfo()
     * @param queryString HttpServletRequest#getQueryString
     * @see HttpServletRequest#getServletPath()
     * @see HttpServletRequest#getContextPath()
     */
    public ProxyRequestContext(String requestURL, String servletPath, String contextPath, String pathInfo, String queryString) {
        this.requestURL = requestURL;
        this.servletPath = servletPath;
        this.contextPath = contextPath;
        this.pathInfo = pathInfo;
        this.queryString = queryString;
    }
}
