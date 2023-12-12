#!/bin/sh

sudo mn --custom="$1" --topo="$2" --controller=remote,ip=127.0.0.1,port=6653 --switch=ovs,protocols=OpenFlow14
sudo mn -c