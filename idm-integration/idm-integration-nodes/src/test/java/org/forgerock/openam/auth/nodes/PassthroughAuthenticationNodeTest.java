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
 * Copyright 2021-2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.forgerock.http.protocol.Status.UNAUTHORIZED;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.auth.nodes.PassthroughAuthenticationNode.NodeOutcome.AUTHENTICATED;
import static org.forgerock.openam.auth.nodes.PassthroughAuthenticationNode.NodeOutcome.FAILED;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_PASSWORD_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.PermanentException;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PassthroughAuthenticationNodeTest {
    @Mock
    private PassthroughAuthenticationNode.Config config;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private Realm realm;

    @Captor
    private ArgumentCaptor<JsonValue> termsCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> patchCaptor;

    private PassthroughAuthenticationNode node;

    @BeforeMethod
    private void setUp() throws Exception {
        initMocks(this);

        when(config.systemEndpoint()).thenReturn("fooconnector");
        when(config.objectType()).thenReturn("__ACCOUNT__");
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.passwordAttribute()).thenReturn(DEFAULT_PASSWORD_ATTRIBUTE);

        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        when(idmIntegrationService.getUsernameFromContext(any())).thenCallRealMethod();

        node = new PassthroughAuthenticationNode(config, realm, idmIntegrationService);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldFailIfConnectorNotLoaded() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field(DEFAULT_PASSWORD_ATTRIBUTE, "pass")
                ))
        ));

        when(idmIntegrationService.passthroughAuth(any(), any(), any(String.class), any(String.class),
                any(String.class), any(String.class)))
                .thenThrow(newResourceException(NOT_FOUND));

        node.process(getContext(Collections.emptyList(), sharedState));
    }

    @Test
    public void shouldFailIfCredentialsAreBad() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field(DEFAULT_PASSWORD_ATTRIBUTE, "pass")
                ))
        ));

        when(idmIntegrationService.passthroughAuth(any(), any(), any(String.class), any(String.class),
                any(String.class), any(String.class)))
                .thenThrow(new PermanentException(UNAUTHORIZED.getCode(), "Invalid Credentials",
                        new Exception()));

        Action action = node.process(getContext(Collections.emptyList(), sharedState));

        assertThat(action.outcome).isEqualTo(FAILED.toString());
    }

    @Test
    public void shouldSucceedIfCredentialsAreGood() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field(DEFAULT_PASSWORD_ATTRIBUTE, "pass")
                ))
        ));

        when(idmIntegrationService.passthroughAuth(any(), any(), any(String.class), any(String.class),
                any(String.class), any(String.class)))
                .thenReturn(true);

        Action action = node.process(getContext(Collections.emptyList(), sharedState));

        assertThat(action.outcome).isEqualTo(AUTHENTICATED.toString());
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
