#! /bin/sh

cd ~/SDN/lab4/unicastdhcp
mvn clean install -DskipTests
onos-app localhost install! target/unicastdhcp-1.0-SNAPSHOT.oar

# cd ~/SDN/lab4/echoconfig
# mvn clean install -DskipTests
# onos-app localhost install! target/echoconfig-1.0-SNAPSHOT.oar