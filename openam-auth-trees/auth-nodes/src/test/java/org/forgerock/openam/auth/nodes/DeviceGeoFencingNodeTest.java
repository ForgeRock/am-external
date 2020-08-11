/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes;


import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.DeviceGeoFencingNode.Config;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.utils.JsonValueBuilder;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.IdRepoException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DeviceGeoFencingNodeTest {

    @Mock
    Config config;

    @InjectMocks
    DeviceGeoFencingNode node;

    @BeforeMethod
    public void setup() throws IdRepoException, SSOException, DevicePersistenceException {
        node = null;
        initMocks(this);
        given(config.distance()).willReturn("100");
    }

    @Test
    public void testProcessContextWithTrustLocation()
            throws NodeProcessException {

        Set<String> locations = new HashSet<>();
        locations.add("-123.177201,49.164532");
        given(config.locations()).willReturn(locations);

        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("latitude", -123.175012);
        newLocation.put("longitude", 49.164553);

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    public void testProcessContextWithNonTrustLocation()
            throws NodeProcessException {

        Set<String> locations = new HashSet<>();
        locations.add("-97.130854,49.164532");
        given(config.locations()).willReturn(locations);

        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("latitude", -123.175012);
        newLocation.put("longitude", 49.164553);

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void testProcessContextWithOneInRangeOneIsNot()
            throws NodeProcessException {

        Set<String> locations = new HashSet<>();
        locations.add("-97.130854,49.164532");
        locations.add("-123.177201,49.164532");
        given(config.locations()).willReturn(locations);

        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("latitude", -123.175012);
        newLocation.put("longitude", 49.164553);

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("true");
    }


    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.empty());
    }

}