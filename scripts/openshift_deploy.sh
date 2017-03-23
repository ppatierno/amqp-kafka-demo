#!/bin/sh

echo DEPLOYING ENMASSE AND KAFKA ON OPENSHIFT

# creating new project
oc new-project enmasse --description="Messaging as a Service" --display-name="EnMasse"

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

echo Deploying EnMasse with Kafka support ...
oc create sa enmasse-service-account -n $(oc project -q)
oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default
oc policy add-role-to-user edit system:serviceaccount:$(oc project -q):enmasse-service-account
oc process -f https://raw.githubusercontent.com/EnMasseProject/enmasse/master/generated/enmasse-template-with-kafka.yaml | oc create -f -
echo ... done

echo ENMASSE AND KAFKA DEPLOYED ON OPENSHIFT
