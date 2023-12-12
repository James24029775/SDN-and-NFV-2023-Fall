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
package nctu.winlab.bridge;

import org.onlab.packet.Ethernet;
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
import java.util.Dictionary;
import java.util.Properties;

// My imports
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
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
    public static final int DEFAULT_IPV4_PRIORITY = 1;
    public static final int DEFAULT_PRIORITY = 30;
    public static final int DEFAULT_TIMEOUT = 30;

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

    Map<DeviceId, Map<MacAddress, PortNumber>> macTable = new HashMap<>();

    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());

        appId = coreService.registerApplication("nctu.winlab.bridge");
        packetService.addProcessor(processor, PacketProcessor.director(2));
        requestIntercepts();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);

        withdrawIntercepts();
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
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

            if (ethPkt == null) {
                return;
            }

            MacAddress sourceMac = ethPkt.getSourceMAC();
            MacAddress destinationMac = ethPkt.getDestinationMAC();
            DeviceId switchId = pkt.receivedFrom().deviceId();
            PortNumber inPort = context.inPacket().receivedFrom().port();

            /** Update ONOS mac table */
            log.info("Add an entry to the port table of `{}`. MAC address: `{}` => Port: `{}`.", switchId, sourceMac,
                    inPort);
            updateMacTable(switchId, sourceMac, inPort);

            /** If Query is hit, install a flow rule, else flood the packet */
            Boolean ifHitTable = macTable.containsKey(switchId)
                    && macTable.get(switchId).containsKey(destinationMac);
            if (ifHitTable) {
                log.info("MAC address `{}` is matched on `{}`. Install a flow rule.", destinationMac, switchId);
                PortNumber toPort = macTable.get(switchId).get(destinationMac);
                installFlowRule(context, toPort);
                packetOut(context, toPort);
            } else {
                log.info("MAC address `{}` is missed on `{}`. Flood the packet.", destinationMac, switchId);
                packetOut(context, PortNumber.FLOOD);
            }
        }
    }

    private void updateMacTable(DeviceId switchId, MacAddress sourceMac, PortNumber inPort) {
        macTable.computeIfAbsent(switchId, k -> new HashMap<>()).put(sourceMac, inPort);
    }

    // Sends a packet out the specified port.
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    // Install a flow rule to a switch.
    private void installFlowRule(PacketContext context, PortNumber portNumber) {
        InboundPacket pkt = context.inPacket();
        Ethernet ethPkt = pkt.parsed();

        if (ethPkt == null) {
            return;
        }

        MacAddress sourceMac = ethPkt.getSourceMAC();
        MacAddress destinationMac = ethPkt.getDestinationMAC();
        DeviceId switchId = pkt.receivedFrom().deviceId(); // The one sending pktIn to the controller.
        /**
         * FlowObjectiveService
         */
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment treatment;
        selector.matchEthSrc(MacAddress.valueOf(sourceMac.toString()))
                .matchEthDst(MacAddress.valueOf(destinationMac.toString()));
        treatment = context.treatmentBuilder().setOutput(portNumber).build();
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selector.build())
                .withTreatment(treatment)
                .withPriority(DEFAULT_PRIORITY)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makeTemporary(DEFAULT_TIMEOUT)
                .add();
        flowObjectiveService.forward(switchId, forwardingObjective);
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
