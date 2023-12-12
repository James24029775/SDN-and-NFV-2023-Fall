#! /bin/sh

cd ~/lab3/bridge-018-"$1"
onos-app localhost deactivate nctu.winlab.bridge
onos-app localhost uninstall nctu.winlab.bridge