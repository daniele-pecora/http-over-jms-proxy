package de.superfusion.transport.jms;

import org.apache.http.HttpHost;

import java.net.URI;
import java.util.List;

/**
 * Security:<br/>
 * Make sure <code>targetHost</code> or HTTP Header Parameter <code>Host</code> matches with setting {@link Config.Consumer#TARGET_URI()} (<code>de.superfusion.jms.TARGET_URI</code>)
 */
public class RequestValidator {

    private String cleanHostString(String hostString) {
        if (null == hostString)
            return hostString;
        return hostString.replaceAll("[/\\?]*$", "");
    }

    public void isTargetHostValid(HttpHost targetHost, HTTPMapper.SimplePlainHTTPRequest rq) throws InvalidHostException, InvalidTargetHostException, MissingHostHeaderException, InvalidTargetHostConfigurationException {
        String expectedHost = cleanHostString(Config.Consumer.TARGET_URI());
        String actualHost = targetHost.toString();
        if (!actualHost.startsWith(expectedHost)) {
            /**
             * Security:<br/>
             * Make sure <code>targetHost</code> or HTTP Header Parameter <code>Host</code> matches with setting {@link Config.Consumer#TARGET_URI()} (<code>de.superfusion.jms.TARGET_URI</code>)
             */
            throw new InvalidTargetHostException("Expected '" + expectedHost + "' but got '" + actualHost + "'");
        }

        String expectedHostExtracted;
        try {
            expectedHostExtracted = new URI(expectedHost).getAuthority();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InvalidTargetHostConfigurationException(e);
        }

        List<HTTPMapper.HeaderVar> headers = rq.headers;
        String host = null;
        for (HTTPMapper.HeaderVar headerVar : headers) {
            if (0 == headerVar.getName().compareToIgnoreCase("host")) {
                host = headerVar.getValue();
                /**
                 * For every host that has been found in HTTP Header
                 */
                if (null == host)
                    throw new MissingHostHeaderException();

                if (0 != host.compareToIgnoreCase(expectedHostExtracted))
                    throw new InvalidHostException();
            }
        }
    }

    public static class InvalidHostException extends Exception {
        public InvalidHostException() {
            super("Invalid host in header.");
        }
    }

    public static class MissingHostHeaderException extends Exception {
        public MissingHostHeaderException() {
            super("No host in header present.");
        }
    }

    public static class InvalidTargetHostException extends Exception {

        public InvalidTargetHostException(String message) {
            super("Invalid target host. " + message);
        }
    }

    public static class InvalidTargetHostConfigurationException extends Exception {
        public InvalidTargetHostConfigurationException(Exception e) {
            super("Invalid target host in configuration.", e);
        }
    }
}
