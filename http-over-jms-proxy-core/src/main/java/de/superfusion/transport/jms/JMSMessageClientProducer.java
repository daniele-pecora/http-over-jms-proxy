package de.superfusion.transport.jms;


import javax.jms.*;
import java.util.Random;
import java.util.UUID;

/**
 * This sender is used to send the http requests as jms messages to the jms broker.<br/>
 *
 * @author daniele
 */
public class JMSMessageClientProducer {

    private String instanceId;
    private final JMSAbstractClient.JMSSessionClientProducer jmsSessionClient;
    private final MessageProducer requestProducer;

    public JMSMessageClientProducer() throws JMSException {
        this(null);
    }

    /**
     * @param instanceId Any ID that will be used to recognize this instance.<br/>
     *                   Mostly used for logging purposes.<br/>
     *                   May be <code>null</code> or empty , then a random UUID will be created.<br/>
     * @throws JMSException
     */
    public JMSMessageClientProducer(final String instanceId) throws JMSException {
        String _instanceId = instanceId;
        if (null == _instanceId || String.valueOf(_instanceId).trim().length() < 1) {
            try {
                _instanceId = String.valueOf(UUID.randomUUID());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.instanceId = _instanceId;
        jmsSessionClient = new JMSAbstractClient.JMSSessionClientProducer();
        requestProducer = jmsSessionClient.getProducer();
    }

    private String prependInstanceID(String string) {
        return "[" + this.instanceId + "] " + string;
    }

    public String send(String message) throws JMSException {
        return receiveText(sendMessageSync(message));
    }

    private Message sendMessageSync(String text) throws JMSException {
        /**
         * Create a temporary queue that this client will listen for responses on then create a consumer<br/>
         * that consumes message from this temporary queue...for a real application a client should reuse<br/>
         * the same temp queue for each message to the server...one temp queue per client
         */
        Destination answerDestination = jmsSessionClient.createTemporaryQueue();

        /**
         * Now create the actual message you want to send
         */
        TextMessage txtMessage = jmsSessionClient.createTextMessage();
        txtMessage.setText(text);

        /**
         * Set the reply to field to the temp queue you created above, this is the queue the server will respond to
         */
        txtMessage.setJMSReplyTo(answerDestination);
        /**
         * Set a correlation ID so when you get a response you know which sent message the response is for<br/>
         * If there is never more than one outstanding message to the server then the<br/>
         * same correlation ID can be used for all the messages...if there is more than one outstanding<br/>
         * message to the server you would presumably want to associate the correlation ID with this<br/>
         * message somehow...a Map works good<br/>
         */
        String correlationId = this.createRandomString();
        txtMessage.setJMSCorrelationID(correlationId);

        MessageConsumer responseConsumer = jmsSessionClient.createConsumer(answerDestination);

        requestProducer.send(txtMessage);

        Message responseMessage = responseConsumer.receive();

        return responseMessage;
    }

    private String createRandomString() {
        Random random = new Random(System.currentTimeMillis());
        long randomLong = random.nextLong();
        return Long.toHexString(randomLong);
    }

    private String receiveText(Message answer) throws JMSException {
        if (null == answer)
            return null;
        TextMessage textMessage = (TextMessage) answer;
        return textMessage.getText();
    }

}
