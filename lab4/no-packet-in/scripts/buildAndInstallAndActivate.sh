#! /bin/sh

cd ~/SDN/lab4/no-packet-in
mvn clean install -DskipTests
onos-app localhost install! target/no-packet-in-1.0-SNAPSHOT.oar

# cd ~/SDN/lab4/echoconfig
# mvn clean install -DskipTests
# onos-app localhost install! target/echoconfig-1.0-SNAPSHOT.oar