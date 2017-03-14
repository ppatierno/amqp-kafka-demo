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

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Created by ppatiern on 13/03/17.
 */
public class Receiver {

  private static final Logger LOG = LoggerFactory.getLogger(Receiver.class);

  private static final String FACTORY_LOOKUP = "myFactoryLookup";
  private static final String KAFKA_TOPIC_LOOKUP = "myKafkaReceiveTopicLookup";

  public static void main(String[] args) {

    LOG.info("Starting receiver ...");

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

      MessageConsumer messageConsumer = session.createConsumer(topic);

      messageConsumer.setMessageListener(message -> {

        try {

          if (message instanceof BytesMessage) {

            BytesMessage bytesMessage = (BytesMessage) message;
            byte[] data = new byte[(int) bytesMessage.getBodyLength()];
            bytesMessage.readBytes(data);
            LOG.info("Message received {}", new String(data));

          } else if (message instanceof TextMessage) {

            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();
            LOG.info("Message received {}", text);
          }

        } catch (JMSException jmsEx) {
          jmsEx.printStackTrace();
        }

      });

      System.in.read();
      messageConsumer.close();
      session.close();
      connection.close();

    } catch (Exception e) {

      LOG.error("Caught exception, exiting", e);
      e.printStackTrace();
      System.exit(1);
    }
  }
}
