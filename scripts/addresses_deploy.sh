#!/bin/sh

echo DEPLOYING ADDRESSES
curl -X PUT -H "content-type: application/json" --data-binary @../addresses/addresses.json http://$(oc get service -o jsonpath='{.spec.clusterIP}' address-controller):8080/v3/address
echo ADDRESSES DEPLOYED
