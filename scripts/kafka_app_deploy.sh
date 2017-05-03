#!/bin/sh

echo DEPLOYING KAFKA WEB UI APPLICATION ON OPENSHIFT

oc create -f ../kafka-consumer-webui/target/fabric8/kafka-consumer-webui-configmap.yml

oc create -f ../kafka-consumer-webui/target/fabric8/kafka-consumer-webui-svc.yml
oc create -f ../kafka-consumer-webui/target/fabric8/kafka-consumer-webui-deployment.yml
oc create -f ../kafka-consumer-webui/target/fabric8/kafka-consumer-webui-route.yml

echo KAFKA WEB UI APPLICATION DEPLOYED ON OPENSHIFT
