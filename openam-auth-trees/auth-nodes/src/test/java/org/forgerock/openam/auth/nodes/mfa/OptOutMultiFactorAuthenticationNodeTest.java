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

package org.forgerock.openam.auth.nodes.mfa;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.MFA_METHOD;
import static org.forgerock.openam.auth.nodes.mfa.MultiFactorConstants.PUSH_METHOD;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Optional;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.devices.DeviceSettings;
import org.forgerock.openam.core.rest.devices.services.AuthenticatorDeviceServiceFactory;
import org.forgerock.openam.core.rest.devices.services.SkipSetting;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.idm.AMIdentity;

public class OptOutMultiFactorAuthenticationNodeTest {

    @Mock
    OptOutMultiFactorAuthenticationNode.Config config;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private IdentityUtils identityUtils;

    OptOutMultiFactorAuthenticationNode node;

    @BeforeMethod
    public void setup() throws NodeProcessException {
        initMocks(this);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void processThrowExceptionIfUserNameNotPresentInSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object());
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        // When
        node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        // throw exception
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void processThrowExceptionIfMFAMethodNotPresentInSharedState() throws Exception {
        // Given
        JsonValue sharedState = json(object(field(USERNAME, "rod")));
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        // When
        node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        // throw exception
    }

    @Test
    public void processShouldSkipIfUserAttributeIsSetAsSkippable() throws Exception {
        // Given
        JsonValue sharedState = json(object(
                field(USERNAME, "rod"),
                field(REALM, "root"),
                field(MFA_METHOD, PUSH_METHOD))
        );
        JsonValue transientState = json(object());

        whenNodeConfigHasDefaultValues();

        MultiFactorNodeDelegate<?> multiFactorNodeDelegate =
                new MultiFactorNodeDelegate(mock(AuthenticatorDeviceServiceFactory.class));
        doReturn(mock(DeviceSettings.class))
                .when(node).getDeviceSettings(anyString(), anyString(), anyString());
        doReturn(multiFactorNodeDelegate)
                .when(node).getMultiFactorNodeDelegate(anyString());
        doReturn(mock(AMIdentity.class))
                .when(node).getIdentityFromIdentifier(any());
        doReturn(SkipSetting.SKIPPABLE)
                .when(node).shouldSkip(any(), anyString());

        // When
        Action result = node.process(getContext(sharedState, transientState, emptyList()));

        // Then
        assertThat(result.sharedState).isNullOrEmpty();
        assertThat(result.transientState).isNullOrEmpty();
        assertThat(result.outcome).isEqualTo("outcome");
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState,
            List<? extends Callback> callbacks) {
        return new TreeContext(
                sharedState,
                transientState,
                new ExternalRequestContext.Builder().build(),
                callbacks,
                Optional.empty()
        );
    }

    private void whenNodeConfigHasDefaultValues() {
        config = mock(OptOutMultiFactorAuthenticationNode.Config.class);
        node = spy(new OptOutMultiFactorAuthenticationNode(coreWrapper, identityUtils));
    }

}
