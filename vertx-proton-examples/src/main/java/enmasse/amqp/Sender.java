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
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by ppatiern on 13/03/17.
 */
public class Sender {

  private static final Logger LOG = LoggerFactory.getLogger(Sender.class);

  private static final String MESSAGING_HOST = "172.30.233.55";
  private static final int MESSAGING_PORT = 5672;
  private static final String KAFKA_TOPIC = "kafka.mytopic";
  private static final int DELAY = 10;
  private static final int DEFAULT_COUNT = 50;

  private ProtonConnection connection;
  private ProtonSender sender;
  private int count = 0;

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    Sender sender = new Sender();
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

        this.sender = this.connection.createSender(KAFKA_TOPIC);
        this.sender.open();

        vertx.setPeriodic(DELAY, t -> {

          if (this.connection.isDisconnected()) {
            vertx.cancelTimer(t);
          } else {

            if (++count <= DEFAULT_COUNT) {

              Message message = ProtonHelper.message(KAFKA_TOPIC,
                String.format("Hello %d from Vert.x Proton [%s] !", count, connection.getContainer()));

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

      if (sender.isOpen())
        sender.close();
      connection.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
