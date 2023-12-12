#!/bin/sh

curl -u onos:rocks -X POST -H "Content-Type: application/json" -d @"$2" http://localhost:8181/onos/v1/flows/"$1"
