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
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.DeviceProfileCallback;
import org.forgerock.openam.utils.JsonValueBuilder;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test for Device Profile Collector
 */
public class DeviceProfileCollectorNodeTest {

    public static final String COLLECTING_DEVICE_PROFILE = "Collecting device profile";
    @Mock
    DeviceProfileCollectorNode.Config config;

    @Mock
    LocaleSelector localeSelector;

    @InjectMocks
    DeviceProfileCollectorNode node;

    @BeforeMethod
    public void setup() {
        node = null;
        initMocks(this);
        given(config.deviceLocation()).willReturn(true);
        given(config.deviceMetadata()).willReturn(true);
        given(config.maximumSize()).willReturn("3");
        given(config.message()).willReturn(Map.of(Locale.ENGLISH, COLLECTING_DEVICE_PROFILE));
        when(localeSelector.getBestLocale(any(), any())).thenReturn(Locale.ENGLISH);
    }

    @Test
    public void testProcessWithNoCallback()
            throws NodeProcessException {
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isNull();
        assertThat(result.callbacks).hasSize(1);
        assertThat(result.callbacks.get(0)).isInstanceOf(DeviceProfileCallback.class);
        assertThat(((DeviceProfileCallback) result.callbacks.get(0)).isLocation()).isTrue();
        assertThat(((DeviceProfileCallback) result.callbacks.get(0)).isMetadata()).isTrue();
        assertThat(((DeviceProfileCallback) result.callbacks.get(0)).getMessage()).isEqualTo(COLLECTING_DEVICE_PROFILE);

    }

    @Test
    public void testProcessWithCallback()
            throws NodeProcessException {
        JsonValue sharedState = json(object(field(USERNAME, "bob")));
        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, COLLECTING_DEVICE_PROFILE);
        JsonValue profile = JsonValueBuilder.jsonValue().build();
        profile.put("identifier", "testIdentifier1");
        profile.put("metadata", "{\"test\":\"value\"}");
        profile.put("location", "{\"latitude\":123, \"longitude\":123}");
        callback.setValue(profile.toString());

        // When
        Action result = node
                .process(getContext(sharedState, transientState, singletonList(callback)));

        //Then
        assertThat(result.outcome).isEqualTo("outcome");
        assertThat(result.callbacks).isEmpty();
        assertThat(result.sharedState.isDefined(DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME)).isTrue();
        assertThat(result.sharedState.get(DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME)
                .isDefined(DeviceProfile.METADATA_ATTRIBUTE_NAME)).isTrue();
        assertThat(result.sharedState.get(DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME)
                .isDefined(DeviceProfile.LOCATION_ATTRIBUTE_NAME)).isTrue();
        assertThat(result.sharedState.get(DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME)
                .isDefined(DeviceProfile.IDENTIFIER_ATTRIBUTE_NAME)).isTrue();

    }

    @Test(expectedExceptions = NodeProcessException.class,
            expectedExceptionsMessageRegExp = "Device Identifier is not captured")
    public void testNoIdentifier() throws NodeProcessException {
        JsonValue sharedState = json(object(field(USERNAME, "bob")));
        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, COLLECTING_DEVICE_PROFILE);
        JsonValue profile = JsonValueBuilder.jsonValue().build();
        profile.put("metadata", "{\"test\":\"value\"}");
        profile.put("location", "{\"latitude\":123, \"longitude\":123}");
        callback.setValue(profile.toString());

        // When
        node.process(getContext(sharedState, transientState, singletonList(callback)));

    }

    @Test(expectedExceptions = NodeProcessException.class,
            expectedExceptionsMessageRegExp = "Captured data exceed maximum accepted size")
    public void testExceedSize() throws NodeProcessException {

        given(config.maximumSize()).willReturn("0.01"); //around 30 bytes
        JsonValue sharedState = json(object(field(USERNAME, "bob")));
        JsonValue transientState = json(object());

        DeviceProfileCallback callback = new DeviceProfileCallback(true, true, COLLECTING_DEVICE_PROFILE);
        JsonValue profile = JsonValueBuilder.jsonValue().build();
        profile.put("identifier", "testIdentifier1");
        profile.put("metadata", "{\"test\":\"value\"}");
        profile.put("location", "{\"latitude\":123, \"longitude\":123}");
        callback.setValue(profile.toString());

        // When
        node.process(getContext(sharedState, transientState, singletonList(callback)));

    }


    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, Optional.empty());
    }

}