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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
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

  private static final int DEFAULT_COUNT = 50;
  private static final int DELIVERY_MODE = DeliveryMode.NON_PERSISTENT;

  public static void main(String[] args) {

    int count = DEFAULT_COUNT;

    try {

      Context context = new InitialContext();

      ConnectionFactory factory = (ConnectionFactory) context.lookup("myFactoryLookup");
      Destination topic = (Destination) context.lookup("myTopicLookup");

      Connection connection = factory.createConnection();
      connection.start();

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageProducer messageProducer = session.createProducer(topic);

      for (int i = 1; i <= count; i++) {
        TextMessage message = session.createTextMessage(String.format("Hello %d from JMS !", i));
        messageProducer.send(message, DELIVERY_MODE, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
      }

      connection.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
