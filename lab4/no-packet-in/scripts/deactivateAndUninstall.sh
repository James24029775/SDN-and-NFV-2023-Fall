#! /bin/sh

cd ~/SDN/lab4/no-packet-in
onos-app localhost deactivate nctu.winlab.unicastdhcp
onos-app localhost uninstall nctu.winlab.unicastdhcp

# cd ~/SDN/lab4/echoconfig
# onos-app localhost deactivate nctu.winlab.echoconfig
# onos-app localhost uninstall nctu.winlab.echoconfig