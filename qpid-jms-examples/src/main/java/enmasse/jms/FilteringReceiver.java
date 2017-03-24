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
import java.util.Properties;

/**
 * Created by ppatiern on 24/03/17.
 */
public class FilteringReceiver {

  private static final Logger LOG = LoggerFactory.getLogger(FilteringReceiver.class);

  private static final String MESSAGING_HOST = "localhost";
  private static final int MESSAGING_PORT = 5672;
  private static final String QUEUE = "myqueue";

  private static final String FACTORY_LOOKUP = "myFactoryLookup";
  private static final String QUEUE_LOOKUP = "myQueueLookup";

  public static void main(String[] args) {

    Options options = new Options();
    options.addOption("a", true, "Messaging host");
    options.addOption("p", true, "Messaging port");
    options.addOption("q", true, "Queue");
    options.addOption("h", false, "Print this help");

    CommandLineParser parser = new DefaultParser();

    try {
      CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption("h")) {

        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("Receiver", options);

      } else {

        String messagingHost = cmd.getOptionValue("a", MESSAGING_HOST);
        int messagingPort = Integer.parseInt(cmd.getOptionValue("p", String.valueOf(MESSAGING_PORT)));
        String queue = cmd.getOptionValue("t", QUEUE);

        FilteringReceiver receiver = new FilteringReceiver();
        receiver.run(messagingHost, messagingPort, queue);
      }

    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  private void run(String messagingHost, int messagingPort, String queue) {

    LOG.info("Starting receiver : connecting to [{}:{}] queue [{}]",
      messagingHost, messagingPort, queue);

    try {

      Properties props = new Properties();
      props.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
      props.put("connectionfactory.myFactoryLookup", String.format("amqp://%s:%d", messagingHost, messagingPort));
      props.put("queue.myQueueLookup", queue);

      Context context = new InitialContext(props);

      ConnectionFactory factory = (ConnectionFactory) context.lookup(FACTORY_LOOKUP);
      Destination destination = (Destination) context.lookup(QUEUE_LOOKUP);

      Connection connection = factory.createConnection();
      connection.setExceptionListener(e -> {
        LOG.error("Connection exception, exiting", e);
        e.printStackTrace();
        System.exit(1);
      });
      connection.start();

      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      MessageConsumer messageConsumer = session.createConsumer(destination, "count % 2 = 0");

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
