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
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Created by ppatiern on 08/03/17.
 */
public class Consumer {

  private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

  private static final String KAFKA_BOOTSTRAP_SERVERS = "172.30.149.195:9092";

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();

    Properties config = new Properties();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "mygroup");
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config, String.class, String.class);
    consumer.handler(record -> {
      LOG.info("Received on topic={}, partition={}, offset={}, value={}",
        record.topic(), record.partition(), record.offset(), record.value());
    });
    consumer.subscribe("kafka.mytopic");
  }
}
