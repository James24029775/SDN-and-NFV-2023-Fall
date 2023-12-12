#!/bin/sh

curl -u onos:rocks -X DELETE -H 'Accept: application/json' http://localhost:8181/onos/v1/flows/"$1"/"$2"