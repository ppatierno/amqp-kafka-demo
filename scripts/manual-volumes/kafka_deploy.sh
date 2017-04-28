#!/bin/sh

# creating the directories for Kafka/Zookeeper persistent volumes
bash create_host_dirs.sh

# creating Zookeeper and Kafka persistent volume (admin needed)
oc login -u system:admin
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/cluster-volumes.yaml

# starting to deploy EnMasse + Kafka (developer user)
oc login -u developer

echo Deploying Zookeeper ...
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/zookeeper-headless-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/zookeeper-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/zookeeper.yaml
echo ... done

echo Deploying Apache Kafka ...
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/kafka-headless-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/kafka-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/kafka.yaml
echo ... done
