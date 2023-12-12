#! /bin/sh

cd ~/lab3/bridge-018-"$1"
mvn clean install -DskipTests
onos-app localhost install! target/bridge-018-1.0-SNAPSHOT.oar
