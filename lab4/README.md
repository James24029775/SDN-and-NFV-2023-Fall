
* build the env
    - (localhost) ./startServer.sh to start ONOS process
    - (localhost) ./buildTopology.sh to build the topology
    - (localhost) ./buildAndInstallAndActivate.sh to load the APP

* how to make a host have a dynamic IP
    - (localhost) ./uploadConfig.sh to let ONOS know DHCP server's location
    - (mininet) h1 dhclient -v h1-eth0

* how to listen a host on mininet
    - (mininet) xterm h1
    - (h1 term) tcpdump -vvv -s 15000 'port 67 or port 68'

* some tools
    - (localhost) ./deactivateAndUninstall.sh to unload the APP
    - (localhost) onos localhost intents
    - (localhost) onos localhost remove-intent nctu.winlab.unicastdhcp 0x1
