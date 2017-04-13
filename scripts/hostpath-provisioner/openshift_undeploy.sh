#!/bin/sh

echo UNDEPLOYING ENMASSE AND KAFKA FROM OPENSHIFT

# deleting entire project with related resources
oc login -u developer
oc delete project enmasse

# deleting the directory for hosting persistent volumes
rm -rf /tmp/hostpath-provisioner

echo ENMASSE AND KAFKA UNDEPLOYED FROM OPENSHIFT
