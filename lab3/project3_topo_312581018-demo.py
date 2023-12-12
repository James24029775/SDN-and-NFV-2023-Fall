from mininet.topo import Topo

class Project3_Topo_312581018 ( Topo ):
    def __init__( self ):
        Topo.__init__( self )
        
        # Add hosts
        h1 = self.addHost( 'h1' )
        h2 = self.addHost( 'h2' )
        h3 = self.addHost( 'h3' )
        h4 = self.addHost( 'h4' )
        
        # Add switches
        s1 = self.addSwitch( 's1' )
        s2 = self.addSwitch( 's2' )
        s3 = self.addSwitch( 's3' )
        s4 = self.addSwitch( 's4' )
        s5 = self.addSwitch( 's5' )
        s6 = self.addSwitch( 's6' )
        s7 = self.addSwitch( 's7' )
        s8 = self.addSwitch( 's8' )
        s9 = self.addSwitch( 's9' )
        
        # Add links
        self.addLink( h1, s1 )
        self.addLink( h2, s3 )
        self.addLink( h3, s7 )
        self.addLink( h4, s9 )

        self.addLink( s2, s1 )
        self.addLink( s2, s3 )

        self.addLink( s8, s7 )
        self.addLink( s8, s9 )
        
        self.addLink( s5, s2 )
        self.addLink( s5, s4 )
        self.addLink( s5, s6 )
        self.addLink( s5, s8 )
    
topos = { 'topo_312581018': Project3_Topo_312581018 }
    