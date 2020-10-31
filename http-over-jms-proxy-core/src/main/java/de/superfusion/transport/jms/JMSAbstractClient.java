package de.superfusion.transport.jms;

import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

/**
 * Provides Classes for jms producer and jms consumer clients.<br/>
 *
 * @author daniele
 */
public class JMSAbstractClient {
    /**
     * Base representation of a jms client.<br/>
     *
     * @author daniele
     */
    public abstract static class JMSSessionClient {

        protected Connection connection;
        protected Session session;
        protected Queue requestQueue;

        protected JMSSessionClient() throws JMSException {
            String brokerHost = Config.Producer.resolveBrokerDestination();
            final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerHost);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            requestQueue = session.createQueue(Config.Producer.REQUEST_MESSAGE_QUEUE_NAME());
        }

        public void close() {
            try {
                session.unsubscribe(requestQueue.getQueueName());
            } catch (JMSException e) {
                e.printStackTrace();
            }
            try {
                session.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
            try {
                connection.stop();
            } catch (JMSException e) {
                e.printStackTrace();
            }
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Wraps a message producer.<br/>
     *
     * @author daniele
     */
    public static class JMSSessionClientProducer extends JMSSessionClient {

        private MessageProducer requestProducer;

        public JMSSessionClientProducer() throws JMSException {
            super();
        }

        /**
         * Provides the underlying message producer
         *
         * @return
         * @throws JMSException
         */
        public MessageProducer getProducer() throws JMSException {
            if (null == requestProducer) {
                requestProducer = session.createProducer(requestQueue);
                requestProducer.setDeliveryMode(Config.Producer.DELIVERY_MODE());
            }
            return requestProducer;
        }

        public Queue createTemporaryQueue() throws JMSException {
            return session.createTemporaryQueue();
        }

        public TextMessage createTextMessage() throws JMSException {
            return session.createTextMessage();
        }

        public MessageConsumer createConsumer(Destination answerDestination) throws JMSException {
            return session.createConsumer(answerDestination);
        }
    }

    /**
     * Wraps a message consumer.<br/>
     *
     * @author daniele
     */
    public static class JMSSessionClientConsumer extends JMSSessionClient {

        private static final long serialVersionUID = -1441681320797844546L;

        public JMSSessionClientConsumer() throws JMSException {
            super();
        }

        /**
         * Provides the underlying message consumer
         *
         * @return
         * @throws JMSException
         */
        public MessageConsumer getConsumer() throws JMSException {
            MessageConsumer requestConsumer = session.createConsumer(requestQueue);
            return requestConsumer;
        }

        public MessageProducer createProducer(Destination replyTo) throws JMSException {
            MessageProducer messageProducer = session.createProducer(replyTo);
            messageProducer.setDeliveryMode(Config.Producer.DELIVERY_MODE());
            return messageProducer;
        }

        public TextMessage createTextMessage(String text) throws JMSException {
            return session.createTextMessage(text);
        }
    }
}
