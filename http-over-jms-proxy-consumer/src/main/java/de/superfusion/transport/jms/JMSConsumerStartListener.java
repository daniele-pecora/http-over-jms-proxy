package de.superfusion.transport.jms;

import javax.jms.JMSException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Starts up and registers multiple jms consumers to process incoming jms messages containing tunneled http requests.<br/>
 *
 * @author daniele
 */
public class JMSConsumerStartListener implements ServletContextListener {

    private static class JMSConsumerThread {
        private long interval = 500;
        private String instanceId;
        private Thread thread;
        private boolean cancelled;
        private Runnable runnable = () -> {
            JMSMessageClientConsumer jmsMessageClientConsumer;
            try {
                jmsMessageClientConsumer = new JMSMessageClientConsumer(this.instanceId);
                jmsMessageClientConsumer.start();
                System.out.println(String.format("Starting consumer ID: %1$s", this.instanceId));
                while (true) {
                    if (__stopRunnable(jmsMessageClientConsumer)) {
                        break;
                    }
                    try {
                        Thread.sleep(this.interval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (__stopRunnable(jmsMessageClientConsumer)) {
                        break;
                    }
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }
        };

        private boolean __stopRunnable(JMSMessageClientConsumer jmsMessageClientConsumer) {
            if (this.isCancelled() || null == this.thread || this.thread.isInterrupted()) {
                jmsMessageClientConsumer.stop();
                this.stop();
            }
            return true;
        }

        private boolean isCancelled() {
            return this.cancelled;
        }

        private JMSConsumerThread() {
            instanceId = String.valueOf(UUID.randomUUID());
            thread = new Thread(runnable);
        }

        @Override
        protected void finalize() throws Throwable {
            stop();
            super.finalize();
        }

        void start() {
            this.thread.start();
        }

        void stop() {
            this.cancelled = true;
            System.out.println(String.format("Stopping consumer ID: %1$s", this.instanceId));
            if (null != this.thread) {
                try {
                    this.thread.interrupt();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<JMSConsumerThread> threads = new ArrayList<>();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        for (int i = 0; i < Config.Consumer.CONSUMER_AMOUNT(); i++) {
            threads.add(new JMSConsumerThread());
        }
        this.threads.forEach(JMSConsumerThread::start);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        killThreads();
    }

    private void killThreads() {
        this.threads.forEach(JMSConsumerThread::stop);
        this.threads.clear();
    }

    @Override
    protected void finalize() throws Throwable {
        killThreads();
        super.finalize();
    }
}
