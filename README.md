# Bridging AMQP - Kafka (demo)

This demo shows how it's possible to use the [AMQP](http://www.amqp.org/) protocol for sending/receiving messages to/from [Apache Kafka](https://kafka.apache.org/) cluster.
It's based on using an [EnMasse](https://github.com/EnMasseProject) deployment which is a Maas (Messaging as a Service) platform running on OpenShift. This platform provides
the messaging infrastructure for connecting using different protocols like AMQP and MQTT and this specific demo also uses the [AMQP-Kafka bridge](https://github.com/EnMasseProject/amqp-kafka-bridge)
component in order to have a "gate" for accessing Apache Kafka topics through AMQP semantics. The Apache Kafka cluster is deployed using the [barnabas](https://github.com/EnMasseProject/barnabas)
project available under the same EnMasse umbrella.

More information about single deployed components are available at related projects repositories.

## Prerequisites

For this demo, you need the OpenShift client tools and for that you can download the [OpenShift Origin](https://github.com/openshift/origin/releases) ones.
Follow [this guide](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md) for setting up a local developer instance of OpenShift, for having an
accessible registry for Docker and starting the cluster locally.

## Setting up AMQP - Kafka

### Creating project

First, create a new project :

  oc new-project enmasse

### Deploying the Apache Kafka cluster

The Apache Kafka cluster deployment uses persistent volumes for storing Zookeeper and Kafka brokers data (i.e. logs, consumer offsets, ...).
For a local development, we can just use local drive for that but creating directory with read/write access permissions is needed.

  mkdir /tmp/zookeeper
  chmod 777 /tmp/zookeeper
  mkdir /tmp/kafka
  chmod 777 /tmp/kafka

In this way we have two different directories that will be used as persistent volumes by the OpenShift resources YAML files.

After making available above accessible directories, the persistent volumes need to be deployed by OpenShift administrator. In this case you can just login
as system admin on your local OpenShift cluster for doing that.

    oc login -u system:admin

The persistent volumes can be deployed in the following way :

    oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/resources/zookeeper-volume.yaml
    oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/resources/kafka-volume.yaml

After that you can return to be a "developer" user.

    oc login -u developer

In order to complete the persistent volumes actions, the claims need to be deployed.

    oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/resources/zookeeper-volume-claim.yaml
    oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/resources/kafka-volume-claim.yaml

After that, the Zookeeper service and deployment can be create.

    oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/resources/zookeeper-service.yaml
    oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/resources/zookeeper.yaml

Finally, the Kafka service and brokers deployment.

    oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/resources/kafka-service.yaml
    oc create -f https://raw.githubusercontent.com/EnMasseProject/barnabas/master/resources/kafka.yaml

Accessing the OpenShift console, the current deployment should be visible.

![Apache Kafka on OpenShift](./images/kafka_deployment.png)
