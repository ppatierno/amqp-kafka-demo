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


package enmasse.amqp;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonSender;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ppatiern on 13/03/17.
 */
public class Sender {

  private static final Logger LOG = LoggerFactory.getLogger(Sender.class);

  private static final String MESSAGING_HOST = "localhost";
  private static final int MESSAGING_PORT = 5672;
  private static final String AMQP_ADDRESS = "kafka.mytopic";
  private static final int MESSAGES_DELAY = 10;
  private static final int MESSAGES_COUNT = 50;

  private ProtonConnection connection;
  private ProtonSender sender;
  private int count = 0;

  public static void main(String[] args) {

    Options options = new Options();
    options.addOption("h", true, "Messaging host");
    options.addOption("p", true, "Messaging port");
    options.addOption("a", true, "AMQP address");
    options.addOption("c", true, "Number of messages to send");
    options.addOption("d", true, "Delay between messages");
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
        String amqpAddress = cmd.getOptionValue("a", AMQP_ADDRESS);
        int messagesCount = Integer.parseInt(cmd.getOptionValue("c", String.valueOf(MESSAGES_COUNT)));
        int messagesDelay = Integer.parseInt(cmd.getOptionValue("d", String.valueOf(MESSAGES_DELAY)));

        Vertx vertx = Vertx.vertx();

        LOG.info("Starting sender : connecting to [{}:{}] address [{}] msgs count/delay [{}/{}]",
          messagingHost, messagingPort, amqpAddress, messagesCount, messagesDelay);

        Sender sender = new Sender();
        sender.run(vertx, messagingHost, messagingPort, amqpAddress, messagesCount, messagesDelay);

        vertx.close();
      }

    } catch (ParseException e) {
      e.printStackTrace();
    }

  }

  private void run(Vertx vertx, String messagingHost, int messagingPort, String amqpAddress, int messagesCount, int messagesDelay) {

    ProtonClient client = ProtonClient.create(vertx);

    client.connect(messagingHost, messagingPort, done -> {

      if (done.succeeded()) {

        this.connection = done.result();
        this.connection.open();

        LOG.info("Connected as {}", this.connection.getContainer());

        this.sender = this.connection.createSender(amqpAddress);
        this.sender.open();

        vertx.setPeriodic(messagesDelay, t -> {

          if (this.connection.isDisconnected()) {
            vertx.cancelTimer(t);
          } else {

            if (++count <= messagesCount) {

              Message message = ProtonHelper.message(amqpAddress,
                String.format("Hello %d from Vert.x Proton [%s] ! (on %s)", count, connection.getContainer(), Instant.now()));

              Map<Symbol, Object> map = new HashMap<>();
              map.put(Symbol.getSymbol("count"), count);
              ApplicationProperties applicationProperties = new ApplicationProperties(map);
              message.setApplicationProperties(applicationProperties);

              sender.send(message, delivery -> {

                LOG.info("Message delivered {}", delivery.getRemoteState());
                if (delivery.getRemoteState() instanceof Rejected) {
                  Rejected rejected = (Rejected) delivery.getRemoteState();
                  LOG.info("... but rejected {} {}", rejected.getError().getCondition(), rejected.getError().getDescription());
                }
              });

            } else {
              vertx.cancelTimer(t);
            }
          }

        });

      } else {
        LOG.info("Error on connection {}", done.cause());
      }
    });

    try {
      System.in.read();

      if (this.sender.isOpen())
        this.sender.close();
      this.connection.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
