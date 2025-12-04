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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DisplayUserNameNodeTest {

    @Mock
    IdmIntegrationService idmIntegrationService;
    @Mock
    private DisplayUserNameNode.Config config;
    @Mock
    private Realm realm;
    @InjectMocks
    private DisplayUserNameNode displayUserNameNode;

    @Test
    void shouldThrowExceptionIfFailedToRetrieveObject() throws Exception {
        when(config.userName()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        assertThatThrownBy(() -> displayUserNameNode.process(getContext(emptyList(), json(object())))
        ).isInstanceOf(NodeProcessException.class).hasMessageContaining("Failed to retrieve existing object");
    }

    @Test
    void shouldReturnUserNameFromSharedStateIfPresent() throws Exception {
        when(config.userName()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test")
                ))
        ));

        List<Callback> callbacks = displayUserNameNode.process(getContext(emptyList(), sharedState)).callbacks;

        assertThat(callbacks.size()).isEqualTo(1);
        TextOutputCallback resultCallback = (TextOutputCallback) callbacks.get(0);
        assertThat(resultCallback.getMessage()).isEqualTo("test");
    }

    @Test
    void shouldRetrieveUserNameIfNotPresentInSharedState() throws Exception {
        when(config.userName()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_MAIL_ATTRIBUTE, "test@gmail.com")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any()))
                .thenReturn(json(object(field(DEFAULT_IDM_IDENTITY_ATTRIBUTE, "test"))));

        List<Callback> callbacks = displayUserNameNode.process(getContext(emptyList(), sharedState)).callbacks;
        assertThat(callbacks.size()).isEqualTo(1);
        TextOutputCallback resultCallback = (TextOutputCallback) callbacks.get(0);
        assertThat(resultCallback.getMessage()).isEqualTo("test");
    }

    @Test
    void shouldFailIfUserNameNotPresentInRetrievedObject() throws Exception {
        when(config.userName()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_MAIL_ATTRIBUTE, "test@gmail.com")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any())).thenReturn(json(object()));

        assertThatThrownBy(() -> displayUserNameNode.process(getContext(emptyList(), sharedState)))
                .isInstanceOf(NodeProcessException.class).hasMessageContaining("Unable to find username to display");
    }

    @Test
    void shouldContinueIfCallbackPresent() throws Exception {

        Action action = displayUserNameNode.process(getContext(
                singletonList(new TextOutputCallback(TextOutputCallback.INFORMATION, "test")), json(object())));

        assertThat(action.outcome).isEqualTo("outcome");
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
