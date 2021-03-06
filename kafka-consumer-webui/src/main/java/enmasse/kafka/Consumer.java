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
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Set;

/**
 * Created by ppatiern on 08/03/17.
 */
public class Consumer {

  private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

  private static final String SEEK = "seek";
  private static final String SEEK_TO_BEGIN = "seektobegin";
  private static final String STATUS = "status";

  private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
  private static final String KAFKA_CONSUMER_GROUPID = "mygroup";
  private static final String KAFKA_CONSUMER_TOPIC = "kafka.mytopic";
  private static final String KAFKA_CONSUMER_AUTO_OFFSET_RESET = "earliest";

  private static Set<TopicPartition> assignedTopicPartitions;

  public static void main(String[] args) {

    String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
    LOG.info("KAFKA_BOOTSTRAP_SERVERS = {}", bootstrapServers);

    String groupId = System.getenv("KAFKA_CONSUMER_GROUPID");
    LOG.info("KAFKA_CONSUMER_GROUPID = {}", groupId);

    String topic = System.getenv("KAFKA_CONSUMER_TOPIC");
    LOG.info("KAFKA_CONSUMER_TOPIC = {}", topic);

    String autoOffsetReset = System.getenv("KAFKA_CONSUMER_AUTO_OFFSET_RESET");
    LOG.info("KAFKA_CONSUMER_AUTO_OFFSET_RESET = {}", autoOffsetReset);

    Vertx vertx = Vertx.vertx();

    Router router = Router.router(vertx);

    BridgeOptions options = new BridgeOptions();
    options
      .addOutboundPermitted(new PermittedOptions().setAddress("dashboard"))
      .addOutboundPermitted(new PermittedOptions().setAddress("status"))
      .addInboundPermitted(new PermittedOptions().setAddress("config"));

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
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

    KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config, String.class, String.class);
    consumer.handler(record -> {
      LOG.info("Received on topic={}, partition={}, offset={}, value={}",
        record.topic(), record.partition(), record.offset(), record.value());
      vertx.eventBus().publish("dashboard", record.value());
    });

    consumer.partitionsAssignedHandler(topicPartitions -> {
      assignedTopicPartitions = topicPartitions;
      TopicPartition topicPartition = assignedTopicPartitions.stream().findFirst().get();
      String status = String.format("Joined group = [%s], topic = [%s], partition = [%d]", groupId, topicPartition.getTopic(), topicPartition.getPartition());
      vertx.eventBus().publish("status", status);
    });

    consumer.subscribe(topic);

    vertx.eventBus().consumer("config", message -> {

      String body = message.body().toString();

      switch (body) {

        case SEEK_TO_BEGIN:

          consumer.seekToBeginning(assignedTopicPartitions);
          break;

        case SEEK:

          try {
            long offset = Long.valueOf(message.headers().get("offset"));
            assignedTopicPartitions.stream().forEach(topicPartition -> {
              consumer.seek(topicPartition, offset);
            });
          } catch (NumberFormatException e) {
            LOG.error("The specified offset isn't a number", e);
          }
          break;

        case STATUS:

          if (assignedTopicPartitions != null) {
            TopicPartition topicPartition = assignedTopicPartitions.stream().findFirst().get();
            String status = String.format("Joined group = [%s], topic = [%s], partition = [%d]", groupId, topicPartition.getTopic(), topicPartition.getPartition());
            vertx.eventBus().publish("status", status);
          } else {
            vertx.eventBus().publish("status", String.format("Joining group = [%s] ...", groupId));
          }
          break;
      }
    });
  }
}
