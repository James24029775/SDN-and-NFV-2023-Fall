#!/bin/sh

sudo mn --controller=remote,127.0.0.1:6653 --switch=ovs,protocols=OpenFlow14
sudo mn -c