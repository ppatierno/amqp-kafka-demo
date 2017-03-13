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
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by ppatiern on 13/03/17.
 */
public class Receiver {

  private static final Logger LOG = LoggerFactory.getLogger(Receiver.class);

  private static final String MESSAGING_HOST = "172.30.233.55";
  private static final int MESSAGING_PORT = 5672;
  private static final String KAFKA_TOPIC = "kafka.mytopic";
  private static final String KAFKA_GROUP_ID = "mygroup";

  private ProtonConnection connection;
  private ProtonReceiver receiver;

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    Receiver sender = new Receiver();
    sender.run(vertx);

    vertx.close();
  }

  private void run(Vertx vertx) {

    ProtonClient client = ProtonClient.create(vertx);

    client.connect(MESSAGING_HOST, MESSAGING_PORT, done -> {

      if (done.succeeded()) {

        this.connection = done.result();
        this.connection.open();

        LOG.info("Connected as {}", this.connection.getContainer());

        this.receiver = this.connection.createReceiver(String.format("%s/group.id/%s", KAFKA_TOPIC, KAFKA_GROUP_ID));

        this.receiver.handler((delivery, message) -> {

          Section section = message.getBody();

          if (section instanceof Data) {
            Binary data = ((Data)section).getValue();
            LOG.info("Message received {}", new String(data.getArray()));
          } else if (section instanceof AmqpValue) {
            String text = (String) ((AmqpValue)section).getValue();
            LOG.info("Message received {}", text);
          }

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
}
