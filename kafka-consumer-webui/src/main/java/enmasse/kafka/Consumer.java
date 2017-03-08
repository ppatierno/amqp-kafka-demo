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

package enmasse.kafka;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Properties;

/**
 * Created by ppatiern on 08/03/17.
 */
public class Consumer {

  private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

  private static final String KAFKA_BOOTSTRAP_SERVERS = "172.30.149.195:9092";

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    Router router = Router.router(vertx);

    BridgeOptions options = new BridgeOptions();
    options.setOutboundPermitted(Collections.singletonList(new PermittedOptions().setAddress("dashboard")));
    router.route("/eventbus/*").handler(SockJSHandler.create(vertx).bridge(options));
    router.route().handler(StaticHandler.create().setCachingEnabled(false));

    HttpServer httpServer = vertx.createHttpServer();
    httpServer
      .requestHandler(router::accept)
      .listen(8080, done -> {

        if (done.succeeded()) {
          LOG.info("HTTP server started on port {}", done.result().actualPort());
        } else {
          LOG.error("HTTP server not started", done.cause());
        }
      });

    Properties config = new Properties();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "mygroup");
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config, String.class, String.class);
    consumer.handler(record -> {
      LOG.info("Received on topic={}, partition={}, offset={}, value={}",
        record.topic(), record.partition(), record.offset(), record.value());
      vertx.eventBus().publish("dashboard", record.value());
    });
    consumer.subscribe("kafka.mytopic");
  }
}
