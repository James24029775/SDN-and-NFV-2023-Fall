/*
 * Copyright 2023-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nctu.winlab.ProxyArp;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.MacAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Properties;

// My imports
import org.onlab.packet.ARP;
import org.onlab.packet.Ip4Address;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import static org.onlab.util.Tools.get;
import java.util.HashMap;
import java.util.Map;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true, service = { AppComponent.class }, property = {
        "someProperty=Some Default String Value",
})
public class AppComponent {
    /** Some configurable property. */
    private final Logger log = LoggerFactory.getLogger(getClass());
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    /** My variables. */

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    private ReactivePacketProcessor processor = new ReactivePacketProcessor();

    private ApplicationId appId;
    private MacAddress targetMac;
    private Ethernet ethPacket;
    private Boolean ifHitCpTable;
    private Boolean ifHitArpTable;

    Map<Ip4Address, MacAddress> arpTable = new HashMap<>();
    Map<Ip4Address, ConnectPoint> cpTable = new HashMap<>();

    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());

        appId = coreService.registerApplication("nctu.winlab.ProxyArp");
        packetService.addProcessor(processor, PacketProcessor.director(2));
        // requestIntercepts();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);

        // withdrawIntercepts();
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    /**
     * Whenever a packet goes into a controller, it will do the function.
     * Packet processor responsible for forwarding packets along their paths.
     */
    private class ReactivePacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            // Stop processing if the packet has been handled, since we
            // can't do any more to it.
            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPkt = pkt.parsed();

            // check if ethPkt is null
            if (ethPkt == null) {
                return;
            }

            // check if ethPkt's payload is arp
            if (ethPkt.getEtherType() != Ethernet.TYPE_ARP) {
                return;
            } 
            // else if (ethPkt.getEtherType() != Ethernet.TYPE_IPV4) {

            // }
            // // parse src ip
            // IPv4 ipv4Packet = (IPv4) ethPacket.getPayload();
            // Ip4Address srcIp = Ip4Address.valueOf(ipv4Packet.getSourceAddress());

            // parse mac(l2) related info
            DeviceId switchId = pkt.receivedFrom().deviceId();
            PortNumber switchPort = context.inPacket().receivedFrom().port();

            // parse arp(l3) related info
            ARP arpPacket = (ARP) ethPkt.getPayload();
            short opCode = arpPacket.getOpCode();
            MacAddress senderMac = MacAddress.valueOf(arpPacket.getSenderHardwareAddress());
            Ip4Address senderIp = Ip4Address.valueOf(arpPacket.getSenderProtocolAddress());
            Ip4Address targetIp = Ip4Address.valueOf(arpPacket.getTargetProtocolAddress());

            // 不要timeout，感覺紀錄request的mac-connectionPoint，屆時reply就將該封包forward這connectionPoint
            // ConnectPoint connectPoint = new ConnectPoint(switchId, switchPort);
            updateArpTable(senderIp, senderMac); // add the entry
            // updateCpTable(srcIp, connectPoint); // add the entry
            // log.info("FEFEFEFEFEFEFEFEFEFE" + srcIp + ": " +connectPoint);

            if (opCode == ARP.OP_REPLY) {
                log.info("RECV REPLY. Requested MAC = {}", senderMac);
                ifHitCpTable = cpTable.containsKey(targetIp);
                // ! Timeout problem still exist
                if (ifHitCpTable) {
                    /* If hit, then forward to the specific connection point. */
                    // connectPoint = cpTable.get(targetIp);
                    // log.info("=============================================ConnectPoint TABLE HIT. {}", connectPoint);
                    // sendPacket(connectPoint.deviceId(), connectPoint.port(), context.inPacket().parsed());
                }
            } else if (opCode == ARP.OP_REQUEST) {
                ifHitArpTable = arpTable.containsKey(targetIp);
                if (ifHitArpTable) {
                    /* If hit, then packetOut to the specific port. */
                    targetMac = arpTable.get(targetIp);
                    log.info("TABLE HIT. Requested MAC = {}", targetMac);
                    ethPacket = ARP.buildArpReply(targetIp, targetMac, ethPkt);
                    sendPacket(switchId, switchPort, ethPacket);
                } else {
                    /* If miss, then flood to the other ports. */
                    log.info("TABLE MISS. Send request to edge ports");
                    packetOut(context, PortNumber.FLOOD);
                }
            }

            // log.info("EEEEEEEEEEEEEEEEEEEENTRY");
            // for (Map.Entry<Ip4Address, ConnectPoint> entry : cpTable.entrySet()) {
            //     log.info(entry.getKey() + ": " + entry.getValue());
            // }
            
        }
    }

    private void sendPacket(DeviceId switchId, PortNumber switchPort, Ethernet ethPacket) {
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        builder.setOutput(switchPort);
        packetService.emit(new DefaultOutboundPacket(
                switchId,
                builder.build(),
                ByteBuffer.wrap(ethPacket.serialize())));
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
    }

    private void updateArpTable(Ip4Address ip, MacAddress mac) {
        arpTable.put(ip, mac);
    }

    private void updateCpTable(Ip4Address ip, ConnectPoint connectPoint) {
        cpTable.put(ip, connectPoint);
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    /**
     * Request packet in via packet service.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
        selector.matchEthType(Ethernet.TYPE_IPV6);
        packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }
}
