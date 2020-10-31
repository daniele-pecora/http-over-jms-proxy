package de.superfusion.transport.jms;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;

import javax.jms.DeliveryMode;
import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class Config {
    static class Logging {
        static final String PARAM_LOG = "de.superfusion.transport.jms.log";
        static final String PARAM_MATCH_REQUEST_URL = "de.superfusion.transport.jms.log.MATCH_REQUEST_URL";
        static final String PARAM_MATCH_TARGET_URL = "de.superfusion.transport.jms.log.MATCH_TARGET_URL";
        static final String PARAM_MATCH_TARGET_URL_COOKIE_ONLY = "de.superfusion.transport.jms.log.MATCH_TARGET_URL.cookieOnly";

        static boolean matchUrl(String targetUrl, String regex) {
            boolean match = false;
            if (null != targetUrl && null != regex && !regex.isEmpty()) {
                if (targetUrl.matches(regex)) {
                    match = true;
                }
            }
            return match;
        }

        static boolean isLogActive() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_LOG, ""));
        }

        public static boolean isLog(String targetUrl, String requestUrl) {
            return isLogTargetURL(targetUrl) && isLogRequestURL(requestUrl);
        }

        public static boolean isLogRequestURL(String requestUrl) {
            return isLogActive() && matchUrl(requestUrl, System_getProperty(PARAM_MATCH_REQUEST_URL, ""));
        }

        public static boolean isLogTargetURL(String targetUrl) {
            return isLogActive() && matchUrl(targetUrl, System_getProperty(PARAM_MATCH_TARGET_URL, ""));
        }

        public static boolean isLogCookiesOnly() {
            return isLogActive() &&
                    0 == "true".compareToIgnoreCase(System_getProperty(PARAM_MATCH_TARGET_URL_COOKIE_ONLY, ""));
        }

    }

    static class Proxy {
        static final String PARAM_PREFIX = "de.superfusion.transport.jms.PROXY";
        static final String PARAM_REDUCE_COOKIE_PATH_TO_1_CHILD = PARAM_PREFIX + ".reduceCookiePathToFirstChild";
        static final String PARAM_SEND_ONLY_PREFIXED_COOKIE = PARAM_PREFIX + ".sendOnlyPrefixedCookie";

        /**
         * Reduce the cookie path to the first path segment.<br/>
         * <code>/app/test/style</code> to <code>/app/test</code>.<br/>
         * This solves some issues with the <code>JSESSONID</code>
         */
        static final boolean REDUCE_COOKIE_PATH_TO_1_CHILD() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_REDUCE_COOKIE_PATH_TO_1_CHILD, ""));
        }

        static final boolean SEND_ONLY_PREFIXED_COOKIE() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_SEND_ONLY_PREFIXED_COOKIE, ""));
        }
    }

    static class Consumer {

        static final String PARAM_IGNORE_TARGET_SSL = "de.superfusion.jms.IGNORE_TARGET_SSL";
        static final String PARAM_TARGET_URI = "de.superfusion.jms.TARGET_URI";
        static final String PARAM_TARGET_URI_doPreserveHost = "de.superfusion.jms.TARGET_URI.doPreserveHost";
        static final String PARAM_TARGET_URI_doPreserveCookies = "de.superfusion.jms.TARGET_URI.doPreserveCookies";
        static final String PARAM_TARGET_URI_preserveCookiePath = "de.superfusion.jms.TARGET_URI.preserveCookiePath";
        static final String PARAM_TARGET_URI_doSendUrlFragment = "de.superfusion.jms.TARGET_URI_doSendUrlFragment";
        static final String PARAM_CONSUMER_AMOUNT = "de.superfusion.jms.CONSUMER_AMOUNT";
        static final String PARAM_CONSUMER_IS_PROXY = "de.superfusion.jms.CONSUMER_IS_PROXY";

        private static int getIntValue(String property, int defaultValue) {
            try {
                return Integer.parseInt(System_getProperty(property, String.valueOf(defaultValue)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return defaultValue;
        }

        /**
         * Ignore SSL certificates when connecting to target url
         */
        static final boolean IGNORE_TARGET_SSL() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_IGNORE_TARGET_SSL, ""));
        }

        /**
         * The only knowledge about the target that will process the requests relays by the consumer.<br/>
         * This will cause the request to be redirected through jms without any modification.<br/>
         */
        static final boolean CONSUMER_IS_PROXY() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_CONSUMER_IS_PROXY, ""));
        }

        /**
         * Default <code>targetUri</code>.<br/>
         * When set then any value from the wrapped JMS message will be ignored.<br/>
         * The only knowledge about the target uri to be grab the content from relays by the consumer.<br/>
         */
        static final String TARGET_URI() {
            return System_getProperty(PARAM_TARGET_URI, "");
        }

        static final boolean TARGET_URI_doPreserveHost() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_TARGET_URI_doPreserveHost, ""));
        }

        static final boolean TARGET_URI_doPreserveCookies() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_TARGET_URI_doPreserveCookies, ""));
        }

        static final boolean TARGET_URI_preserveCookiePath() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_TARGET_URI_preserveCookiePath, ""));
        }

        static final boolean TARGET_URI_doSendUrlFragment() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_TARGET_URI_doSendUrlFragment, ""));
        }

        /**
         * Default <code>1</code>.<br/>
         * Set the amount of consumer to initialize.<br/>
         */
        static final int CONSUMER_AMOUNT() {
            return getIntValue(PARAM_CONSUMER_AMOUNT, 1);
        }

    }

    static class Producer {
        static final String PARAM_DELIVERY_MODE = "de.superfusion.jms.DELIVERY_MODE";
        static final String PARAM_REQUEST_MESSAGE_QUEUE_NAME = "de.superfusion.jms.REQUEST_MESSAGE_QUEUE_NAME";
        static final String PARAM_BROKER_DESTINATION = "de.superfusion.jms.BROKER_DESTINATION";
        static final String PARAM_CONSUMER_IS_PROXY = "de.superfusion.jms.CONSUMER_IS_PROXY";


        private static int resolveDeliverMode(String property, int defaultValue) {
            int DELIVERY_MODE = defaultValue;
            String DELIVERY_MODE_STRING = System_getProperty(PARAM_DELIVERY_MODE, String.valueOf(DELIVERY_MODE));
            if (0 == String.valueOf(DeliveryMode.NON_PERSISTENT).compareTo(DELIVERY_MODE_STRING)
                    || 0 == String.valueOf("NON_PERSISTENT").compareToIgnoreCase(DELIVERY_MODE_STRING)) {
                DELIVERY_MODE = DeliveryMode.NON_PERSISTENT;
            } else if (0 == String.valueOf(DeliveryMode.PERSISTENT).compareTo(DELIVERY_MODE_STRING)
                    || 0 == String.valueOf("PERSISTENT").compareToIgnoreCase(DELIVERY_MODE_STRING)) {
                DELIVERY_MODE = DeliveryMode.PERSISTENT;
            }
            return DELIVERY_MODE;
        }


        /**
         * Either <code>DeliveryMode.NON_PERSISTENT</code> or <code>DeliveryMode.PERSISTENT</code>.
         * <br/>Default is <code>DeliveryMode.NON_PERSISTENT</code>
         */
        static int DELIVERY_MODE() {
            return resolveDeliverMode(PARAM_DELIVERY_MODE, DeliveryMode.NON_PERSISTENT);
        }

        static final String REQUEST_MESSAGE_QUEUE_NAME() {
            return System_getProperty(PARAM_REQUEST_MESSAGE_QUEUE_NAME,
                    "de.superfusion.transport.jms.default.QUEUE?consumer.exclusive=false"/**see:http://activemq.apache.org/exclusive-consumer.html*/);
        }

        /**
         * Example: 192.168.11.166:61616
         */
        private static final String BROKER_DESTINATION() {
            return System_getProperty(PARAM_BROKER_DESTINATION, "");
        }

        /**
         * Default transport is <code>tcp</code>
         */
        static final String DEFAULT_TRANSPORT = "tcp";

        static String resolveBrokerDestination() {
            String brokerDestination = BROKER_DESTINATION();
            try {
                if (!brokerDestination.contains("://")) {
                    throw new Exception("[WARN] Will use default transport protocol '" + DEFAULT_TRANSPORT + "'. Missing transport protocol in broker destination: " + brokerDestination);
                }
                /**
                 * is valid uri?
                 * has protocol?
                 */
                URI uri = new URI(brokerDestination);
            } catch (Exception e) {
                e.printStackTrace();
                brokerDestination = String.format("%1$s://%2$s", DEFAULT_TRANSPORT, brokerDestination);
            }
            return brokerDestination;
        }

        /**
         * The only knowledge about the target that will process the requests relays by the consumer.<br/>
         * This will cause the request to be redirected through jms without any modification.<br/>
         */
        static final boolean CONSUMER_IS_PROXY() {
            return 0 == "true".compareToIgnoreCase(System_getProperty(PARAM_CONSUMER_IS_PROXY, ""));
        }

    }

    /**
     * Don't set this in production
     */
    static final String PARAM_RELOAD_CONF_INTERVAL_SEC = "de.superfusion.jms.RELOAD_CONF_INTERVAL_SEC";
    private static Config INSTANCE;
    private Configuration config;
    private ReloadingFileBasedConfigurationBuilder<FileBasedConfiguration> configBuilder;

    private Config() {
        config = load();
    }

    Configuration config() {
        try {
            this.config = configBuilder.getConfiguration();
            /**
             * config contains all properties read from the file
             */
        } catch (ConfigurationException cex) {
            /**
             * loading of the configuration file failed
             */
            cex.printStackTrace();
        }
        return config;
    }

    static String System_getProperty(String key, String defaultValue) {
        return Config.getInstance().config().getString(key, System.getProperty(key, defaultValue));
    }

    Configuration load() {
        Parameters params = new Parameters();
        /**
         * Read data from this file
         */
        String configDir = System.getProperty("HTTP2JMS_CONF", "");
        File propertiesFile = new File(configDir + "/jms-proxy", "jms-proxy.properties");
        System.out.println("**********************************************************************");
        System.out.println("Loading config from : " + propertiesFile);
        System.out.println("**********************************************************************");
        ReloadingFileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new ReloadingFileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased()
                                .setFile(propertiesFile));

        this.configBuilder = builder;

        long triggerPeriod = -1;
        try {
            triggerPeriod = Long.parseLong(System.getProperty(PARAM_RELOAD_CONF_INTERVAL_SEC, "-1"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (triggerPeriod > -1) {
            PeriodicReloadingTrigger trigger = new PeriodicReloadingTrigger(builder.getReloadingController(),
                    null, triggerPeriod, TimeUnit.SECONDS);
            trigger.start();
        }

        return config();
    }

    public static Config getInstance() {
        Config getInstance = INSTANCE;
        if (null == INSTANCE) {
            synchronized (Config.class) {
                getInstance = INSTANCE;
                if (null == INSTANCE) {
                    getInstance = INSTANCE = new Config();

                }
            }
        }
        return getInstance;
    }
}
