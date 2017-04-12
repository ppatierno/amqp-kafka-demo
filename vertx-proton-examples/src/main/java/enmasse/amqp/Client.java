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
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.AmqpError;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by ppatiern on 27/03/17.
 */
public class Client {

  private static final Logger LOG = LoggerFactory.getLogger(Client.class);

  private static final String MESSAGING_HOST = "localhost";
  private static final int MESSAGING_PORT = 5672;

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
        helpFormatter.printHelp("Client", options);

      } else {

        String messagingHost = cmd.getOptionValue("h", MESSAGING_HOST);
        int messagingPort = Integer.parseInt(cmd.getOptionValue("p", String.valueOf(MESSAGING_PORT)));

        Vertx vertx = Vertx.vertx();

        LOG.info("Starting client : connecting to [{}:{}]", messagingHost, messagingPort);

        Client client = new Client();
        client.run(vertx, messagingHost, messagingPort);

        vertx.close();
      }

    } catch (ParseException e) {
      e.printStackTrace();
    }

  }

  public void run(Vertx vertx, String messagingHost, int messagingPort) {

    ProtonClient client = ProtonClient.create(vertx);

    client.connect(messagingHost, messagingPort, done -> {

      if (done.succeeded()) {

        ProtonConnection connection = done.result();
        connection.open();

        LOG.info("Connected as {}", connection.getContainer());

        // attache a link with "dynamic" for the temporary replyTo address
        ProtonReceiver receiver = connection.createReceiver(null);
        Source source =  (Source) receiver.getSource();
        source.setDynamic(true);

        receiver
          .openHandler(ar -> {

            if (ar.succeeded()) {

              ProtonSender sender = connection.createSender("request");
              sender.open();

              // send a request specyfing as replyTo the "dynamic" address
              Message message = ProtonHelper.message("request", "MyRequest");
              message.setReplyTo(receiver.getRemoteSource().getAddress());
              sender.send(message, delivery -> {

                LOG.info("Request delivered {}", delivery.getRemoteState());
              });

            }

          })
          .handler((delivery, message) -> {

            Section section = message.getBody();

            if (section instanceof Data) {
              Binary data = ((Data)section).getValue();
              LOG.info("Response received '{}'", new String(data.getArray()));
            } else if (section instanceof AmqpValue) {
              String text = (String) ((AmqpValue)section).getValue();
              LOG.info("Response received '{}'", text);
            } else {
              LOG.info("Response received but can't decode it");
              Rejected rejected = new Rejected();
              rejected.setError(new ErrorCondition(AmqpError.DECODE_ERROR, "decoding error"));
              delivery.disposition(rejected, true);
              return;
            }

            delivery.disposition(Accepted.getInstance(), true);

          });

        receiver.open();

      } else {
        LOG.info("Error on connection {}", done.cause());
      }

    });

    try {
      System.in.read();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
