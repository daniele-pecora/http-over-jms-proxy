package de.superfusion.transport.jms;

public class ProxyCookiePrefixer {

    public final String prefix;

    public ProxyCookiePrefixer() {
        this.prefix = "!ProxyCookiePrefixer!";
    }

    public ProxyCookiePrefixer(String prefix) {
        this.prefix = prefix;
    }

    public String getCookieNamePrefix(String cookieName) {
        return this.prefix + "" + cookieName;
    }
}
