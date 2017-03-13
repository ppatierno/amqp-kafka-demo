/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.CompletionListener;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Created by ppatiern on 13/03/17.
 */
public class Sender {

  private static final Logger LOG = LoggerFactory.getLogger(Sender.class);

  private static final String FACTORY_LOOKUP = "myFactoryLookup";
  private static final String KAFKA_TOPIC_LOOKUP = "myKafkaTopicLookup";
  private static final int DEFAULT_COUNT = 50;

  public static void main(String[] args) {

    int count = DEFAULT_COUNT;

    try {

      Context context = new InitialContext();

      ConnectionFactory factory = (ConnectionFactory) context.lookup(FACTORY_LOOKUP);
      Destination topic = (Destination) context.lookup(KAFKA_TOPIC_LOOKUP);

      Connection connection = factory.createConnection();
      connection.setExceptionListener(e -> {
        LOG.error("Connection exception, exiting", e);
        e.printStackTrace();
        System.exit(1);
      });
      connection.start();

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageProducer messageProducer = session.createProducer(topic);

      for (int i = 1; i <= count; i++) {
        TextMessage message = session.createTextMessage(String.format("Hello %d from JMS !", i));
        message.setJMSMessageID(String.valueOf(i));
        messageProducer.send(message, Message.DEFAULT_DELIVERY_MODE, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE,
          new MyCompletionLister());
      }

      System.in.read();
      connection.close();

    } catch (Exception e) {

      LOG.error("Caught exception, exiting", e);
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Listener class for completion on sending messages
   */
  private static class MyCompletionLister implements CompletionListener {
    @Override
    public void onCompletion(Message message) {
      try {
        LOG.info("Message sent {}", message.getJMSMessageID());
      } catch (JMSException jmsEx) {
        jmsEx.printStackTrace();
      }
    }

    @Override
    public void onException(Message message, Exception e) {
      try {
      LOG.error("Exception on message {}", message.getJMSMessageID(), e);
      } catch (JMSException jmsEx) {
        jmsEx.printStackTrace();
      }
    }
  }
}
