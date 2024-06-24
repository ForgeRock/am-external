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
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.ACCEPT_DATE;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_ACCEPTED;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_VERSION;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;

import javax.security.auth.callback.Callback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.authentication.callbacks.TermsAndConditionsCallback;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.forgerock.openam.integration.idm.TermsAndConditionsConfig;
import org.mockito.Mock;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class AcceptTermsAndConditionsNodeTest {

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private Realm realm;

    private AcceptTermsAndConditionsNode acceptTermsAndConditionsNode;

    @BeforeTest
    public void setUp() throws Exception {
        openMocks(this);

        when(idmIntegrationService.getActiveTerms(any(), any())).thenReturn(activeTerms());
        acceptTermsAndConditionsNode = new AcceptTermsAndConditionsNode(realm, idmIntegrationService);
    }

    @Test
    public void shouldReturnTermsCallbackWithActiveTerms() throws Exception {
        TreeContext context = getContext(emptyList(), json(object()));
        Action action = acceptTermsAndConditionsNode.process(context);

        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        TermsAndConditionsCallback callback = (TermsAndConditionsCallback) action.callbacks.get(0);
        assertThat(callback.getVersion()).isEqualTo(activeTerms().getVersion());
        assertThat(callback.getTerms()).isEqualTo(activeTerms().getTerms());
        assertThat(callback.getCreateDate()).isEqualTo(activeTerms().getCreateDate());
        assertThat(context.sharedState.isDefined(TERMS_VERSION)).isTrue();
        assertThat(context.sharedState.get(TERMS_VERSION).asString()).isEqualTo(activeTerms().getVersion());
    }

    @Test(expectedExceptions = NodeProcessException.class)
    public void shouldThrowExceptionIfFailsToRetrieveTerms() throws Exception {
        when(idmIntegrationService.getActiveTerms(any(), any())).thenThrow(newResourceException(BAD_REQUEST));

        acceptTermsAndConditionsNode.process(getContext(emptyList(), json(object())));
    }

    @Test
    public void shouldAddAcceptedTermsToSharedState() throws Exception {
        JsonValue sharedState = json(object(field(TERMS_VERSION, activeTerms().getVersion())));
        TermsAndConditionsCallback callback = new TermsAndConditionsCallback(activeTerms().getVersion(),
                activeTerms().getTerms(), activeTerms().getCreateDate());
        callback.setAccept(true);

        Action action = acceptTermsAndConditionsNode.process(getContext(singletonList(callback), sharedState));

        assertThat(action.outcome).isEqualTo("outcome");
        assertThat(action.sharedState).isNotEmpty();
        assertThat(action.sharedState.isDefined(TERMS_ACCEPTED)).isTrue();
        assertThat(action.sharedState.get(TERMS_ACCEPTED).isDefined(TERMS_VERSION)).isTrue();
        assertThat(action.sharedState.get(TERMS_ACCEPTED).get(TERMS_VERSION).asString())
                .isEqualTo(activeTerms().getVersion());
        assertThat(action.sharedState.get(TERMS_ACCEPTED).isDefined(ACCEPT_DATE)).isTrue();
    }

    @Test
    public void shouldReturnCallbackIfTermsNotAccepted() throws Exception {
        TermsAndConditionsCallback callback = new TermsAndConditionsCallback(activeTerms().getVersion(),
                activeTerms().getTerms(), activeTerms().getCreateDate());

        TreeContext context = getContext(singletonList(callback), json(object()));
        Action action = acceptTermsAndConditionsNode.process(context);

        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        TermsAndConditionsCallback resultCallback = (TermsAndConditionsCallback) action.callbacks.get(0);
        assertThat(callback.getVersion()).isEqualTo(resultCallback.getVersion());
        assertThat(callback.getTerms()).isEqualTo(callback.getTerms());
        assertThat(callback.getCreateDate()).isEqualTo(callback.getCreateDate());
        assertThat(callback.getAccept()).isEqualTo(callback.getAccept());
        assertThat(context.sharedState.isDefined(TERMS_VERSION)).isTrue();
        assertThat(context.sharedState.get(TERMS_VERSION).asString()).isEqualTo(activeTerms().getVersion());
    }

    @Test
    public void shouldNotOverwriteExistingSharedStateObjects() throws Exception {
        JsonValue sharedState = json(object(
                field(USERNAME, "test"),
                field(TERMS_VERSION, activeTerms().getVersion()),
                field(OBJECT_ATTRIBUTES, object(
                        field("mail", "test@gmail.com"),
                        field("preferences", object(
                                field("updates", true)
                        )))
                )
        ));
        TermsAndConditionsCallback callback = new TermsAndConditionsCallback(activeTerms().getVersion(),
                activeTerms().getTerms(), activeTerms().getCreateDate());
        callback.setAccept(true);

        Action action = acceptTermsAndConditionsNode.process(getContext(singletonList(callback), sharedState));

        assertThat(action.outcome).isEqualTo("outcome");
        assertThat(action.sharedState).isNotEmpty();
        assertThat(action.sharedState.get(USERNAME).asString()).isEqualTo("test");
        assertThat(action.sharedState.isDefined(OBJECT_ATTRIBUTES)).isTrue();
        assertThat(action.sharedState.get(TERMS_VERSION).asString()).isEqualTo(activeTerms().getVersion());
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).size()).isEqualTo(2);
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("mail").asString()).isEqualTo("test@gmail.com");
        assertThat(action.sharedState.get(OBJECT_ATTRIBUTES).get("preferences")
                .isEqualTo(json(object((field("updates", true)))))).isTrue();
        assertThat(action.sharedState.get(TERMS_ACCEPTED).isDefined(TERMS_VERSION)).isTrue();
        assertThat(action.sharedState.get(TERMS_ACCEPTED).get(TERMS_VERSION).asString())
                .isEqualTo(activeTerms().getVersion());
        assertThat(action.sharedState.get(TERMS_ACCEPTED).isDefined(ACCEPT_DATE)).isTrue();
    }

    private TreeContext getContext(List<? extends Callback> callbacks, JsonValue sharedState) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState,
                new ExternalRequestContext.Builder().build(), callbacks);
    }

    private static TermsAndConditionsConfig activeTerms() {
        JsonValue terms = json(object(
                field("version", "1.0"),
                field("terms", "active terms version 1.0"),
                field("createDate", "2019-05-07T20:46:06.796Z")
        ));
        return OBJECT_MAPPER.convertValue(terms.asMap(), TermsAndConditionsConfig.class);
    }

}
