#!/bin/sh

# creating the directories for the hostpath-provisioner
bash create_host_dirs.sh

# deploying the hostpath-provisioner, permissions and storage class for that
oc login -u system:admin
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/hostpath-provisioner/serviceaccount.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/hostpath-provisioner/clusterrole.yaml
oc adm policy add-scc-to-user hostmount-anyuid system:serviceaccount:$(oc project -q):hostpath-provisioner
oc adm policy add-cluster-role-to-user hostpath-provisioner-runner system:serviceaccount:$(oc project -q):hostpath-provisioner
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/hostpath-provisioner/storageclass.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/hostpath-provisioner/hostpath-provisioner.yaml

# starting to deploy EnMasse + Kafka (developer user)
oc login -u developer

echo Deploying Zookeeper ...
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/zookeeper-headless-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/zookeeper-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/hostpath-provisioner/zookeeper.yaml
echo ... done

echo Deploying Apache Kafka ...
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/kafka-headless-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/kafka-service.yaml
oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/kafka-statefulsets/resources/hostpath-provisioner/kafka.yaml
echo ... done
