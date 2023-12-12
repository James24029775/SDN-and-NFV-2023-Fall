#! /bin/sh

cd ~/SDN/lab5/ProxyArp
mvn clean install -DskipTests
onos-app localhost install! target/ProxyArp-1.0-SNAPSHOT.oar
