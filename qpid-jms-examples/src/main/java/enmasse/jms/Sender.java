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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
import java.util.Properties;

/**
 * Created by ppatiern on 13/03/17.
 */
public class Sender {

  private static final Logger LOG = LoggerFactory.getLogger(Sender.class);

  private static final String MESSAGING_HOST = "localhost";
  private static final int MESSAGING_PORT = 5672;
  private static final String KAFKA_TOPIC = "kafka.mytopic";
  private static final int MESSAGES_DELAY = 10;
  private static final int MESSAGES_COUNT = 50;

  private static final String FACTORY_LOOKUP = "myFactoryLookup";
  private static final String KAFKA_TOPIC_LOOKUP = "myKafkaTopicLookup";

  public static void main(String[] args) {

    Options options = new Options();
    options.addOption("a", true, "Messaging host");
    options.addOption("p", true, "Messaging port");
    options.addOption("t", true, "Kafka topic");
    options.addOption("c", true, "Number of messages to send");
    options.addOption("d", true, "Delay between messages");
    options.addOption("h", false, "Print this help");

    CommandLineParser parser = new DefaultParser();

    try {

      CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption("h")) {

        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("Sender", options);

      } else {

        String messagingHost = cmd.getOptionValue("a", MESSAGING_HOST);
        int messagingPort = Integer.parseInt(cmd.getOptionValue("p", String.valueOf(MESSAGING_PORT)));
        String kafkaTopic = cmd.getOptionValue("t", KAFKA_TOPIC);
        int messagesCount = Integer.parseInt(cmd.getOptionValue("c", String.valueOf(MESSAGES_COUNT)));
        int messagesDelay = Integer.parseInt(cmd.getOptionValue("d", String.valueOf(MESSAGES_DELAY)));

        Sender sender = new Sender();
        sender.run(messagingHost, messagingPort, kafkaTopic, messagesCount, messagesDelay);
      }

    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  private void run(String messagingHost, int messagingPort, String kafkaTopic, int messagesCount, int messagesDelay) {

    LOG.info("Starting sender : connecting to [{}:{}] topic [{}] msgs count/delay [{}/{}]",
      messagingHost, messagingPort, kafkaTopic, messagesCount, messagesDelay);

    try {

      Properties props = new Properties();
      props.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
      props.put("connectionfactory.myFactoryLookup", String.format("amqp://%s:%d", messagingHost, messagingPort));
      props.put("topic.myKafkaTopicLookup", kafkaTopic);

      Context context = new InitialContext(props);

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

      for (int i = 1; i <= messagesCount; i++) {
        TextMessage message = session.createTextMessage(String.format("Hello %d from JMS !", i));
        message.setJMSMessageID(String.valueOf(i));
        messageProducer.send(message, Message.DEFAULT_DELIVERY_MODE, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE,
          new MyCompletionLister());
        Thread.sleep(messagesDelay);
      }

      System.in.read();
      messageProducer.close();
      session.close();
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
  private class MyCompletionLister implements CompletionListener {
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
