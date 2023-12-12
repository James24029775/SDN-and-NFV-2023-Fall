from mininet.topo import Topo

class MyTopo ( Topo ):
    def __init__( self ):
        Topo.__init__( self )
        
        self.numLayer = 2
        self.numChild = 3
        self.switchCnt = 0
        self.hostCnt = 0
        
        # Add root switches
        root = self.addSwitch( self.naming('s') )
        self.expansion(root, self.numLayer-1)
            
        
    def expansion(self, node, layerTh):
        if layerTh == 0:
            for _ in range(self.numChild):
                self.hostCnt += 1
                child = self.addHost(self.naming('h'))
                self.addLink( node, child )
            return
        
        for _ in range(self.numChild):
            self.switchCnt += 1
            child = self.addSwitch(self.naming('s'))
            self.addLink( node, child )
            self.expansion(child, layerTh-1)
            
    def naming(self, device):
        if device == 's':
            return device+str(self.switchCnt)
        elif device == 'h':
            return device+str(self.hostCnt)
    
topos = { 'topo': MyTopo }
    