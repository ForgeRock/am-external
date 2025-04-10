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
 * Copyright 2025 ForgeRock AS.
 */
/*
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.forgerock.http.protocol.Status.UNAUTHORIZED;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.nodes.PassthroughAuthenticationNode.NodeOutcome.AUTHENTICATED;
import static org.forgerock.openam.auth.nodes.PassthroughAuthenticationNode.NodeOutcome.FAILED;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_PASSWORD_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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

    @InjectMocks
    private PassthroughAuthenticationNode node;

    @BeforeEach
    void setUp() throws Exception {
        when(config.systemEndpoint()).thenReturn("fooconnector");
        when(config.objectType()).thenReturn("__ACCOUNT__");
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.passwordAttribute()).thenReturn(DEFAULT_PASSWORD_ATTRIBUTE);

        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
    }

    @Test
    void shouldFailIfConnectorNotLoaded() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"),
                        field(DEFAULT_PASSWORD_ATTRIBUTE, "pass")
                ))
        ));

        when(idmIntegrationService.passthroughAuth(any(), any(), any(String.class), any(String.class),
                any(String.class), any(String.class)))
                .thenThrow(newResourceException(NOT_FOUND));

        assertThatThrownBy(() -> node.process(getContext(Collections.emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("org.forgerock.json.resource.NotFoundException: Not Found");
    }

    @Test
    void shouldFailIfCredentialsAreBad() throws Exception {
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
    void shouldSucceedIfCredentialsAreGood() throws Exception {
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
