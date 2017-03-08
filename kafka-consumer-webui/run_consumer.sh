#!/bin/sh

if [ -z "$KAFKA_BOOTSTRAP_SERVERS" ]; then
    if [ -n "$KAFKA_SERVICE_HOST" ]; then
        export KAFKA_BOOTSTRAP_SERVERS=$KAFKA_SERVICE_HOST:$KAFKA_SERVICE_PORT
    else
        echo "ERROR: Kafka bootstrap servers not configured"
    fi
fi

exec java -Dvertx.cacheDirBase=/tmp -Dvertx.disableDnsResolver=true -jar /kafka-consumer-webui-1.0-SNAPSHOT.jar
