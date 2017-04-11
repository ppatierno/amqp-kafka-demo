#!/bin/sh

# creating the directories for Kafka/Zookeeper persistent volumes
if [ ! -d /tmp/kafka-a ]; then
    mkdir /tmp/kafka-a
    chmod 777 /tmp/kafka-a
else
    echo /tmp/kafka-a already exists !
fi

if [ ! -d /tmp/kafka-b ]; then
    mkdir /tmp/kafka-b
    chmod 777 /tmp/kafka-b
else
    echo /tmp/kafka-b already exists !
fi

if [ ! -d /tmp/kafka-c ]; then
    mkdir /tmp/kafka-c
    chmod 777 /tmp/kafka-c
else
    echo /tmp/kafka-c already exists !
fi

if [ ! -d /tmp/kafka-d ]; then
    mkdir /tmp/kafka-d
    chmod 777 /tmp/kafka-d
else
    echo /tmp/kafka-d already exists !
fi
