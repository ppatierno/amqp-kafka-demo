#!/bin/sh

# creating the directory for the hostpath-provisioner
if [ ! -d /tmp/hostpath-provisioner ]; then
    mkdir /tmp/hostpath-provisioner
    chmod 777 /tmp/hostpath-provisioner
else
    echo /tmp/hostpath-provisioner already exists !
fi
