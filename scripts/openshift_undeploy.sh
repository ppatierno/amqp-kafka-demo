#!/bin/sh

echo UNDEPLOYING ENMASSE AND KAFKA FROM OPENSHIFT

# deleting Zookeeper and Kafka persistent volumes
oc login -u system:admin
oc delete pv zookeeperpv
oc delete pv kafkapv

# deleting entire project with related resources
oc login -u developer
oc delete project enmasse

# deleting the directory for hosting persistent volumes
rm -rf /tmp/zookeeper
rm -rf /tmp/kafka

echo ENMASSE AND KAFKA UNDEPLOYED FROM OPENSHIFT
