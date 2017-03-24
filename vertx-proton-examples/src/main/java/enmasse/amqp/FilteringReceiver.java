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
import io.vertx.proton.ProtonReceiver;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.DescribedType;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ppatiern on 24/03/17.
 */
public class FilteringReceiver {

  private static final Logger LOG = LoggerFactory.getLogger(Receiver.class);

  private static final String MESSAGING_HOST = "localhost";
  private static final int MESSAGING_PORT = 5672;
  private static final String QUEUE = "myqueue";

  private ProtonConnection connection;
  private ProtonReceiver receiver;

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
        String queue = cmd.getOptionValue("q", QUEUE);

        Vertx vertx = Vertx.vertx();

        FilteringReceiver receiver = new FilteringReceiver();
        receiver.run(vertx, messagingHost, messagingPort, queue);

        vertx.close();
      }

    } catch (ParseException e) {
      e.printStackTrace();
    }
  }

  private void run(Vertx vertx, String messagingHost, int messagingPort, String queue) {

    LOG.info("Starting receiver : connecting to [{}:{}] queue [{}]",
      messagingHost, messagingPort, queue);

    ProtonClient client = ProtonClient.create(vertx);

    client.connect(messagingHost, messagingPort, done -> {

      if (done.succeeded()) {

        this.connection = done.result();
        this.connection.open();

        LOG.info("Connected as {}", this.connection.getContainer());

        this.receiver = this.connection.createReceiver(queue);

        Map<Symbol, DescribedType> map = new HashMap<>();
        map.put(Symbol.valueOf("jms-selector"), new AmqpJmsSelectorFilter("count % 2 = 0"));
        Source source = (Source) this.receiver.getSource();
        source.setFilter(map);

        this.receiver.handler((delivery, message) -> {

          Section section = message.getBody();

          if (section instanceof Data) {
            Binary data = ((Data)section).getValue();
            LOG.info("Message received {}", new String(data.getArray()));
          } else if (section instanceof AmqpValue) {
            String text = (String) ((AmqpValue)section).getValue();
            LOG.info("Message received {}", text);
          } else {
            LOG.info("Message received but can't decode it");
          }

          delivery.disposition(Accepted.getInstance(), true);

        }).open();

      } else {
        LOG.info("Error on connection {}", done.cause());
      }

    });

    try {
      System.in.read();

      if (this.receiver.isOpen())
        this.receiver.close();
      this.connection.close();

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public class AmqpJmsSelectorFilter implements DescribedType {

    private final String selector;

    public AmqpJmsSelectorFilter(String selector) {
      this.selector = selector;
    }

    @Override
    public Object getDescriptor() {
      return Symbol.valueOf("apache.org:selector-filter:string");
    }

    @Override
    public Object getDescribed() {
      return this.selector;
    }

    @Override
    public String toString() {
      return "AmqpJmsSelectorType{" + selector + "}";
    }
  }
}
