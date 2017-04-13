#!/bin/sh

echo DEPLOYING ENMASSE AND KAFKA ON OPENSHIFT

# creating new project
oc new-project enmasse --description="Messaging as a Service" --display-name="EnMasse"

# deploy Kafka/Zookeeper
bash kafka_deploy.sh

echo Deploying EnMasse with Kafka support ...
oc create sa enmasse-service-account -n $(oc project -q)
oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default
oc policy add-role-to-user edit system:serviceaccount:$(oc project -q):enmasse-service-account
oc process -f https://raw.githubusercontent.com/EnMasseProject/enmasse/master/generated/enmasse-template-with-kafka.yaml | oc create -f -
echo ... done

echo ENMASSE AND KAFKA DEPLOYED ON OPENSHIFT
