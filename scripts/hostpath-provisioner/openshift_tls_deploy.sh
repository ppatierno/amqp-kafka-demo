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

oc secret new qdrouterd-certs ../certs/server-cert.pem ../certs/server-key.pem
oc secret add serviceaccount/default secrets/qdrouterd-certs --for=mount
oc secret new mqtt-certs ../certs/server-cert.pem ../certs/server-key.pem
oc secret add serviceaccount/default secrets/mqtt-certs --for=mount

oc process -f https://raw.githubusercontent.com/EnMasseProject/enmasse/master/generated/tls-enmasse-template-with-kafka.yaml | oc create -f -
echo ... done

echo ENMASSE AND KAFKA DEPLOYED ON OPENSHIFT
