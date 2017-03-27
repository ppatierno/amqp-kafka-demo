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
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;

/**
 * Created by ppatiern on 13/03/17.
 */
public class Server implements MessageListener, CompletionListener {

  private static final Logger LOG = LoggerFactory.getLogger(Server.class);

  private static final String MESSAGING_HOST = "localhost";
  private static final int MESSAGING_PORT = 5672;

  private static final String FACTORY_LOOKUP = "myFactoryLookup";
  private static final String DESTINATION_LOOKUP = "myDestinationLookup";

  private Session session;
  private MessageProducer replyProducer;

  public static void main(String[] args) {

    Options options = new Options();
    options.addOption("h", true, "Messaging host");
    options.addOption("p", true, "Messaging port");
    options.addOption("u", false, "Print this help");

    CommandLineParser parser = new DefaultParser();

    try {

      CommandLine cmd = parser.parse(options, args);

      if (cmd.hasOption("u")) {

        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("Sender", options);

      } else {

        String messagingHost = cmd.getOptionValue("h", MESSAGING_HOST);
        int messagingPort = Integer.parseInt(cmd.getOptionValue("p", String.valueOf(MESSAGING_PORT)));

        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        props.put("connectionfactory.myFactoryLookup", String.format("amqp://%s:%d", messagingHost, messagingPort));
        props.put("queue.myDestinationLookup", "myqueue");

        LOG.info("Starting server : connecting to [{}:{}]", messagingHost, messagingPort);

        Server sender = new Server();
        sender.run(props);
      }

    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  private void run(Properties props) {

    try {

      Context context = new InitialContext(props);

      ConnectionFactory factory = (ConnectionFactory) context.lookup(FACTORY_LOOKUP);
      Destination requestQueue = (Destination) context.lookup(DESTINATION_LOOKUP);

      Connection connection = factory.createConnection();
      connection.setExceptionListener(e -> {
        LOG.error("Connection exception, exiting", e);
        e.printStackTrace();
        System.exit(1);
      });
      connection.start();

      this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

      // create a producer for sending response
      this.replyProducer = this.session.createProducer(null);

      // create consumer on the requests queue
      MessageConsumer requestConsumer = this.session.createConsumer(requestQueue);
      requestConsumer.setMessageListener(this);

      System.in.read();
      connection.close();

    } catch (Exception e) {

      LOG.error("Caught exception, exiting", e);
      e.printStackTrace();
      System.exit(1);
    }
  }

  @Override
  public void onMessage(Message request) {

    try {
      LOG.info("Received request '{}'", ((TextMessage) request).getText());

      // get the replyTo from the request as response destination
      Destination replyDestination = request.getJMSReplyTo();

      // create and send response
      TextMessage replyMessage = session.createTextMessage("MyResponse");

      replyProducer.send(replyDestination, replyMessage, this);

    } catch (JMSException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void onCompletion(Message request) {
    try {
      LOG.info("Response sent '{}'", ((TextMessage)request).getText());
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
