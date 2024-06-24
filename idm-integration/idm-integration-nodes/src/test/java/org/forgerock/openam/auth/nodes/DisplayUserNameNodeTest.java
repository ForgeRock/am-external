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
 * Copyright 2019-2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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

import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DisplayUserNameNodeTest {

    @Mock
    private DisplayUserNameNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    IdmIntegrationService idmIntegrationService;

    private DisplayUserNameNode displayUserNameNode;


    @BeforeMethod
    public void setUp() throws Exception {
        initMocks(this);

        when(config.userName()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();

        displayUserNameNode = new DisplayUserNameNode(realm, config, idmIntegrationService);
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailedToRetrieveObject() throws Exception {

        when(idmIntegrationService.getObject(any(), any(), any(), any(String.class), any()))
                .thenThrow(newResourceException(BAD_REQUEST));

        displayUserNameNode.process(getContext(emptyList(), json(object())));
    }

    @Test
    public void shouldReturnUserNameFromSharedStateIfPresent() throws Exception {
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
    public void shouldRetrieveUserNameIfNotPresentInSharedState() throws Exception {
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

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldFailIfUserNameNotPresentInRetrievedObject() throws Exception {
        JsonValue sharedState = json(object(
                field(OBJECT_ATTRIBUTES, object(
                        field(DEFAULT_IDM_MAIL_ATTRIBUTE, "test@gmail.com")
                ))
        ));

        when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any())).thenReturn(json(object()));

        displayUserNameNode.process(getContext(emptyList(), sharedState));
    }

    @Test
    public void shouldContinueIfCallbackPresent() throws Exception {

        Action action = displayUserNameNode.process(getContext(
                singletonList(new TextOutputCallback(TextOutputCallback.INFORMATION, "test")), json(object())));

        assertThat(action.outcome).isEqualTo("outcome");
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }
}
