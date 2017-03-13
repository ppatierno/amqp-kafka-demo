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
import org.apache.qpid.proton.message.Message;

import java.io.IOException;

/**
 * Created by ppatiern on 13/03/17.
 */
public class Sender {

  private static final String MESSAGING_HOST = "172.30.233.55";
  private static final int MESSAGING_PORT = 5672;
  private static final String KAFKA_TOPIC = "kafka.mytopic";
  private static final int PERIODIC_DELAY = 10;
  private static final int PERIODIC_MAX_MESSAGE = 50;

  private static ProtonConnection connection;
  private static ProtonSender sender;
  private static int count = 0;

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    ProtonClient client = ProtonClient.create(vertx);

    client.connect(MESSAGING_HOST, MESSAGING_PORT, done -> {

      if (done.succeeded()) {

        connection = done.result();
        connection.open();

        sender = connection.createSender(KAFKA_TOPIC);
        sender.open();

        vertx.setPeriodic(PERIODIC_DELAY, t -> {

          if (connection.isDisconnected()) {
            vertx.cancelTimer(t);
          } else {

            if (++count <= PERIODIC_MAX_MESSAGE) {
              Message message = ProtonHelper.message(KAFKA_TOPIC, "Periodic message [" + count + "] from " + connection.getContainer());
              sender.send(message);
            } else {
              vertx.cancelTimer(t);
            }
          }

        });
      }
    });

    try {
      System.in.read();

      if (sender.isOpen())
        sender.close();

      connection.close();
      vertx.close();

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}