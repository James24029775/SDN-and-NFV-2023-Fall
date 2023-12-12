#!/bin/sh

cd ~/SDN/lab5/topo
# sudo mn --custom=topo.py --topo=topo --controller=remote,127.0.0.1:6653 --switch=ovs,protocols=OpenFlow14
sudo mn  --controller=remote,127.0.0.1:6653 --topo=tree,depth=3,fanout=3 --switch=ovs,protocols=OpenFlow14
sudo mn -c