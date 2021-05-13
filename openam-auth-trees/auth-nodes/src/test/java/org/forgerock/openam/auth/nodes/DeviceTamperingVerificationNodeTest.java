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

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.DeviceTamperingVerificationNode.Config;
import org.forgerock.openam.utils.JsonValueBuilder;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DeviceTamperingVerificationNodeTest {

    @Mock
    Config config;

    @InjectMocks
    DeviceTamperingVerificationNode node;

    @BeforeMethod
    public void setup() {
        node = null;
        initMocks(this);
        given(config.score()).willReturn("0.5"); //min score to pass
    }

    @Test
    public void testProcessWithJailBroken()
            throws NodeProcessException {

        JsonValue profile = JsonValueBuilder.jsonValue().build();
        JsonValue platform = JsonValueBuilder.jsonValue().build();
        platform.put("jailBreakScore", 0.8);
        profile.put("platform", platform);

        JsonValue deviceAttributes = JsonValueBuilder.jsonValue().build();
        deviceAttributes.put("identifier", "testIdentifier");
        deviceAttributes.put("metadata", profile);

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceAttributes)));

        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void testProcessWithNotJailBroken()
            throws NodeProcessException {

        JsonValue profile = JsonValueBuilder.jsonValue().build();
        JsonValue platform = JsonValueBuilder.jsonValue().build();
        platform.put("jailBreakScore", 0);
        profile.put("platform", platform);

        JsonValue deviceAttributes = JsonValueBuilder.jsonValue().build();
        deviceAttributes.put("identifier", "testIdentifier");
        deviceAttributes.put("metadata", profile);

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceAttributes)));

        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    public void testProcessWithExactMatch()
            throws NodeProcessException {

        JsonValue profile = JsonValueBuilder.jsonValue().build();
        JsonValue platform = JsonValueBuilder.jsonValue().build();
        platform.put("jailBreakScore", 0.5);
        profile.put("platform", platform);

        JsonValue deviceAttributes = JsonValueBuilder.jsonValue().build();
        deviceAttributes.put("identifier", "testIdentifier");
        deviceAttributes.put("metadata", profile);

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceAttributes)));

        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void testProcessNoProfileCollected()
            throws NodeProcessException {

        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        // When
        node.process(getContext(sharedState, transientState, emptyList()));

    }

    @Test
    public void testProcessWithInvalidMetadata()
            throws NodeProcessException {

        JsonValue profile = JsonValueBuilder.jsonValue().build();
        JsonValue platform = JsonValueBuilder.jsonValue().build();
        platform.put("invalid", 0.5);
        profile.put("platform", platform);

        JsonValue deviceAttributes = JsonValueBuilder.jsonValue().build();
        deviceAttributes.put("identifier", "testIdentifier");
        deviceAttributes.put("metadata", profile);

        JsonValue sharedState = json(object(field(USERNAME, "bob"),
                field(DEVICE_PROFILE_CONTEXT_NAME, deviceAttributes)));

        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("false");
    }


    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.empty());
    }

}