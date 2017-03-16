#!/bin/sh

echo DEPLOYING ENMASSE AND KAFKA ON OPENSHIFT

# creating new project
oc new-project enmasse --description="Messaging as a Service" --display-name="EnMasse"

# creating the directory for Zookeeper persistent volume
if [ ! -d /tmp/zookeeper ]; then
    mkdir /tmp/zookeeper
    chmod 777 /tmp/zookeeper
else
    echo /tmp/zookeeper already exists !
fi

# creating the directory for Kafka persistent volume
if [ ! -d /tmp/kafka ]; then
    mkdir /tmp/kafka
    chmod 777 /tmp/kafka
else
    echo /tmp/kafka already exists !
fi

# creating Zookeeper and Kafka persistent volume (admin needed)
oc login -u system:admin
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-persisted/resources/zookeeper-volume.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-persisted/resources/kafka-volume.yaml

# starting to deploy EnMasse + Kafka (developer user)
oc login -u developer
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-persisted/resources/zookeeper-volume-claim.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-persisted/resources/kafka-volume-claim.yaml

echo Deploying Zookeeper ...
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-persisted/resources/zookeeper-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-persisted/resources/zookeeper.yaml
echo ... done

echo Deploying Apache Kafka ...
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-persisted/resources/kafka-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-persisted/resources/kafka.yaml
echo ... done

echo Deploying EnMasse with Kafka support ...
oc create sa enmasse-service-account -n $(oc project -q)
oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default
oc policy add-role-to-user edit system:serviceaccount:$(oc project -q):enmasse-service-account
oc process -f https://raw.githubusercontent.com/EnMasseProject/openshift-configuration/master/generated/enmasse-template-with-kafka.yaml | oc create -f -
echo ... done

echo ENMASSE AND KAFKA DEPLOYED ON OPENSHIFT
