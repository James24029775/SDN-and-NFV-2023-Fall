/*
 * Copyright 2020-present Open Networking Foundation
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

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.ElementId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

public class HostConfig extends Config<ApplicationId> {

    public static final String HOST = "hostLocation";

    @Override
    public boolean isValid() {
        return hasOnlyFields(HOST);
    }

    public FilteredConnectPoint hostLocation() {
        String hostLocation = get(HOST, null);
        String[] locationTuple = hostLocation.split("/");
        ElementId switchId = DeviceId.deviceId(locationTuple[0]);
        PortNumber switchPort = PortNumber.fromString(locationTuple[1]);
        ConnectPoint connectPoint = new ConnectPoint(switchId, switchPort);
        FilteredConnectPoint filteredConnectPoint = new FilteredConnectPoint(connectPoint);

        return filteredConnectPoint;
    }
}
