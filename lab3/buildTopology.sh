#!/bin/sh

sudo mn --custom=project3_topo_312581018-"$1".py --topo=topo_312581018 --controller=remote,127.0.0.1:6653 --switch=ovs,protocols=OpenFlow14
sudo mn -c
