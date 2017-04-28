#!/bin/sh

echo UNDEPLOYING ENMASSE AND KAFKA FROM OPENSHIFT

# deleting Zookeeper and Kafka persistent volumes
oc login -u system:admin
oc delete pv kafkapv-a
oc delete pv kafkapv-b
oc delete pv kafkapv-c
oc delete pv kafkapv-d

# deleting entire project with related resources
oc login -u developer
oc delete project enmasse

# deleting the directory for hosting persistent volumes
rm -rf /tmp/kafka-a
rm -rf /tmp/kafka-b
rm -rf /tmp/kafka-c
rm -rf /tmp/kafka-d

echo ENMASSE AND KAFKA UNDEPLOYED FROM OPENSHIFT
