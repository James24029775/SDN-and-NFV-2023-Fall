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
package nctu.winlab.unicastdhcp;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// My imports
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.PointToPointIntent;
// import org.onosproject.net.packet.PacketService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.UDP;
import org.onlab.packet.TpPort;
// import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.config.NetworkConfigEvent;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigListener;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true, service = { AppComponent.class }, property = {
        "someProperty=Some Default String Value",
})
public class AppComponent {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;

    /** My variables */
    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    // @Reference(cardinality = ReferenceCardinality.MANDATORY)
    // protected PacketService packetService;

    private FilteredConnectPoint ingressPoint, egressPoint;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private IntentService intentService;

    /** Instantiate the following two objects, listener and factory. */
    private final DhcpConfigListener cfgListener = new DhcpConfigListener();
    private final ConfigFactory<ApplicationId, DhcpConfig> factory = new ConfigFactory<ApplicationId, DhcpConfig>(
            APP_SUBJECT_FACTORY, DhcpConfig.class, "UnicastDhcpConfig") {
        @Override
        public DhcpConfig createConfig() {
            return new DhcpConfig();
        }
    };

    private final ConfigFactory<ApplicationId, HostConfig> hostFactory = new ConfigFactory<ApplicationId, HostConfig>(
            APP_SUBJECT_FACTORY, HostConfig.class, "HostConfig") {
        @Override
        public HostConfig createConfig() {
            return new HostConfig();
        }
    };

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("nctu.winlab.unicastdhcp");
        /**
         * Register the listener and factory to controller. If the listener catches the
         * specific events.
         */
        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(factory);
        cfgService.registerConfigFactory(hostFactory);

        /** Intercepts dhcp packet, and send them to controllers. */
        requestIntercepts();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        withdrawIntercepts();
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(factory);
        cfgService.unregisterConfigFactory(hostFactory);
        log.info("Stopped");
    }

    /**
     * Request packet in via packet service.
     */
    private void requestIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        /* PacketPriority.REACTIVE is 5, and PacketPriority.CONTROL is 40000 */
        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
        // packetService.requestPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /**
     * Cancel request for packet in via packet service.
     */
    private void withdrawIntercepts() {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_UDP)
                .matchUdpDst(TpPort.tpPort(UDP.DHCP_SERVER_PORT))
                .matchUdpSrc(TpPort.tpPort(UDP.DHCP_CLIENT_PORT));
        // packetService.cancelPackets(selector.build(), PacketPriority.REACTIVE, appId);
    }

    /* Create an event listener to catch config of dhcp server. */
    private class DhcpConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            if ((event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED)) {
                if (event.configClass().equals(DhcpConfig.class)) {
                    DhcpConfig config = cfgService.getConfig(appId, DhcpConfig.class);

                    // check whether config is NULL
                    if (config == null) {
                        log.info("config is Null!");
                        return;
                    }
                    egressPoint = config.serverLocation(); // get egress point
                }

                if (event.configClass().equals(HostConfig.class)) {
                    HostConfig config = cfgService.getConfig(appId, HostConfig.class);

                    // check whether config is NULL
                    if (config == null) {
                        log.info("config is Null!");
                        return;
                    }
                    ingressPoint = config.hostLocation(); // get ingress point
                }

                if (ingressPoint == null) {
                    log.info("ingressPoint is Null!");
                    return;
                }
                if (egressPoint == null) {
                    log.info("egressPoint is Null!");
                    return;
                }

                log.info("DHCP server is connected to `{}`, port `{}`", ingressPoint.connectPoint().deviceId(),
                        ingressPoint.connectPoint().port());
                log.info("H2 is connected to `{}`, port `{}`", egressPoint.connectPoint().deviceId(),
                        egressPoint.connectPoint().port());

                TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
                selector.matchEthType(Ethernet.TYPE_IPV4)
                        .matchIPProtocol(IPv4.PROTOCOL_UDP);

                // create to/from intent
                PointToPointIntent toIntent = PointToPointIntent.builder()
                        .appId(appId)
                        .selector(selector.build())
                        .filteredIngressPoint(ingressPoint)
                        .filteredEgressPoint(egressPoint)
                        .priority(50000)
                        .build();

                PointToPointIntent fromIntent = PointToPointIntent.builder()
                        .appId(appId)
                        .selector(selector.build())
                        .filteredIngressPoint(egressPoint)
                        .filteredEgressPoint(ingressPoint)
                        .priority(50000)
                        .build();

                if (toIntent == null) {
                    log.info("toIntent is Null!");
                    return;
                }

                if (fromIntent == null) {
                    log.info("fromIntent is Null!");
                    return;
                }

                // send intent
                intentService.submit(toIntent);
                log.info("Intent `{}`, port `{}` => `{}`, port `{}` is submitted.",
                        ingressPoint.connectPoint().deviceId(),
                        ingressPoint.connectPoint().port(), egressPoint.connectPoint().deviceId(),
                        egressPoint.connectPoint().port());

                intentService.submit(fromIntent);
                log.info("Intent `{}`, port `{}` => `{}`, port `{}` is submitted.",
                        egressPoint.connectPoint().deviceId(),
                        egressPoint.connectPoint().port(), ingressPoint.connectPoint().deviceId(),
                        ingressPoint.connectPoint().port());

            }
        }
    }
}
