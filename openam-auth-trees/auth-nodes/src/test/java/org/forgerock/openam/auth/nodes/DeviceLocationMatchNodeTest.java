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
import static org.forgerock.openam.auth.nodes.DeviceProfile.DEVICE_PROFILE_CONTEXT_NAME;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.DeviceLocationMatchNode.Config;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.DevicePersistenceException;
import org.forgerock.openam.core.rest.devices.profile.DeviceProfilesDao;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.utils.JsonValueBuilder;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DeviceLocationMatchNodeTest {

    @Mock
    CoreWrapper coreWrapper;

    @Mock
    IdentityUtils identityUtils;

    @Mock
    AMIdentity amIdentity;

    @Mock
    Realm realm;

    @Mock
    Config config;

    @Mock
    DeviceProfilesDao deviceProfilesDao;

    @InjectMocks
    DeviceLocationMatchNode node;

    @BeforeMethod
    public void setup() throws IdRepoException, SSOException, DevicePersistenceException {
        node = null;
        initMocks(this);
        given(realm.asPath()).willReturn("/");
        given(coreWrapper.getIdentity(anyString())).willReturn(amIdentity);
        given(amIdentity.isExists()).willReturn(true);
        given(amIdentity.isActive()).willReturn(true);
        given(amIdentity.getName()).willReturn("bob");
        given(config.distance()).willReturn("100");
    }

    @Test
    public void testProcessContextWithinRange()
            throws NodeProcessException, DevicePersistenceException {

        JsonValue storedLocation = JsonValueBuilder.jsonValue().build();
        storedLocation.put("longitude", 49.164532);
        storedLocation.put("latitude", -123.177201);

        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("longitude", 49.164553);
        newLocation.put("latitude", -123.175012);

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("location", storedLocation);

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    public void testProcessContextWithExactMatch()
            throws NodeProcessException, DevicePersistenceException {

        JsonValue storedLocation = JsonValueBuilder.jsonValue().build();
        storedLocation.put("longitude", 49.164532);
        storedLocation.put("latitude", -123.177201);

        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("longitude", 49.164532);
        newLocation.put("latitude", -123.177201);

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("location", storedLocation);

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("true");
    }

    @Test
    public void testProcessContextWithOutOfRange()
            throws NodeProcessException, DevicePersistenceException {

        //Vancouver
        JsonValue storedLocation = JsonValueBuilder.jsonValue().build();
        storedLocation.put("longitude", 49.164532);
        storedLocation.put("latitude", -123.177201);

        //Winnipeg
        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("longitude", 49.878418);
        newLocation.put("latitude", -97.130854);

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("location", storedLocation);

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void testProcessDeviceNotFound()
            throws NodeProcessException, DevicePersistenceException {

        //Vancouver
        JsonValue storedLocation = JsonValueBuilder.jsonValue().build();
        storedLocation.put("longitude", 49.164532);
        storedLocation.put("latitude", -123.177201);

        //Winnipeg
        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("longitude", 49.164532);
        newLocation.put("latitude", -123.177201);

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier1");
        existing.put("location", storedLocation);

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier2");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("unknownDevice");
    }

    @Test
    public void testProcessLocationNotFound()
            throws NodeProcessException, DevicePersistenceException {

        //Winnipeg
        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("longitude", 49.164532);
        newLocation.put("latitude", -123.177201);

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier1");

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier2");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("unknownDevice");
    }

    @Test(expectedExceptions = NodeProcessException.class,
            expectedExceptionsMessageRegExp = "Device Profile Collector Node to collect "
                    + "device attribute is required: location")
    public void testProcessLocationNotInContext()
            throws NodeProcessException {

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier");

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        node.process(getContext(sharedState, transientState, emptyList()));

    }

    @Test(expectedExceptions = NodeProcessException.class,
            expectedExceptionsMessageRegExp = "org.forgerock.openam.auth.node.api.NodeProcessException: "
                    + "User does not exist or inactive")
    public void testProcessContextNoUserInContext()
            throws NodeProcessException {

        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("longitude", 49.164532);
        newLocation.put("latitude", -123.177201);

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier2");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(
                field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));


        JsonValue transientState = json(object());

        // When
        node.process(getContext(sharedState, transientState, emptyList(), Optional.empty()));

    }

    @Test
    public void testProcessStoreAttributeInvalidJson()
            throws NodeProcessException, DevicePersistenceException {

        //Winnipeg
        JsonValue newLocation = JsonValueBuilder.jsonValue().build();
        newLocation.put("longitude", 49.878418);
        newLocation.put("latitude", -97.130854);

        JsonValue existing = JsonValueBuilder.jsonValue().build();
        existing.put("identifier", "testIdentifier");
        existing.put("location", "invalid");

        JsonValue collectedAttributes = JsonValueBuilder.jsonValue().build();
        collectedAttributes.put("identifier", "testIdentifier");
        collectedAttributes.put("location", newLocation);

        JsonValue sharedState = json(object(field(DEVICE_PROFILE_CONTEXT_NAME, collectedAttributes)));

        JsonValue transientState = json(object());

        // When
        given(deviceProfilesDao.getDeviceProfiles(anyString(), anyString()))
                .willReturn(Collections.singletonList(existing));
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        //Then
        assertThat(result.outcome).isEqualTo("unknownDevice");
    }


    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return getContext(sharedState, transientState, callbacks, Optional.of("bob"));
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks, Optional<String> universalId) {
        return new TreeContext(sharedState, transientState, new Builder().build(), callbacks, universalId);
    }

}