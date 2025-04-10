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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.BAD_REQUEST;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.ACCEPT_DATE;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_ACCEPTED;
import static org.forgerock.openam.auth.nodes.AcceptTermsAndConditionsNode.TERMS_VERSION;
import static org.forgerock.openam.auth.nodes.utils.IdmIntegrationNodeUtils.OBJECT_MAPPER;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AcceptTermsAndConditionsNodeTest {

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private Realm realm;

    @InjectMocks
    private AcceptTermsAndConditionsNode acceptTermsAndConditionsNode;

    private static TermsAndConditionsConfig activeTerms() {
        JsonValue terms = json(object(
                field("version", "1.0"),
                field("terms", "active terms version 1.0"),
                field("createDate", "2019-05-07T20:46:06.796Z")
        ));
        return OBJECT_MAPPER.convertValue(terms.asMap(), TermsAndConditionsConfig.class);
    }

    @Test
    void shouldReturnTermsCallbackWithActiveTerms() throws Exception {
        // given
        when(idmIntegrationService.getActiveTerms(any(), any())).thenReturn(activeTerms());
        TreeContext context = getContext(emptyList(), json(object()));

        // when
        Action action = acceptTermsAndConditionsNode.process(context);

        // then
        assertThat(action.callbacks).isNotEmpty();
        assertThat(action.callbacks.size()).isEqualTo(1);
        TermsAndConditionsCallback callback = (TermsAndConditionsCallback) action.callbacks.get(0);
        assertThat(callback.getVersion()).isEqualTo(activeTerms().getVersion());
        assertThat(callback.getTerms()).isEqualTo(activeTerms().getTerms());
        assertThat(callback.getCreateDate()).isEqualTo(activeTerms().getCreateDate());
        assertThat(context.sharedState.isDefined(TERMS_VERSION)).isTrue();
        assertThat(context.sharedState.get(TERMS_VERSION).asString()).isEqualTo(activeTerms().getVersion());
    }

    @Test
    void shouldThrowExceptionIfFailsToRetrieveTerms() throws Exception {
        // given
        when(idmIntegrationService.getActiveTerms(any(), any())).thenThrow(newResourceException(BAD_REQUEST));

        // when & then
        assertThatThrownBy(() -> acceptTermsAndConditionsNode.process(getContext(emptyList(), json(object()))))
                .isInstanceOf(NodeProcessException.class);
    }

    @Test
    void shouldAddAcceptedTermsToSharedState() throws Exception {
        // given
        JsonValue sharedState = json(object(field(TERMS_VERSION, activeTerms().getVersion())));
        TermsAndConditionsCallback callback = new TermsAndConditionsCallback(activeTerms().getVersion(),
                activeTerms().getTerms(), activeTerms().getCreateDate());
        callback.setAccept(true);

        //when
        Action action = acceptTermsAndConditionsNode.process(getContext(singletonList(callback), sharedState));

        //then
        assertThat(action.outcome).isEqualTo("outcome");
        assertThat(action.sharedState).isNotEmpty();
        assertThat(action.sharedState.isDefined(TERMS_ACCEPTED)).isTrue();
        assertThat(action.sharedState.get(TERMS_ACCEPTED).isDefined(TERMS_VERSION)).isTrue();
        assertThat(action.sharedState.get(TERMS_ACCEPTED).get(TERMS_VERSION).asString())
                .isEqualTo(activeTerms().getVersion());
        assertThat(action.sharedState.get(TERMS_ACCEPTED).isDefined(ACCEPT_DATE)).isTrue();
    }

    @Test
    void shouldReturnCallbackIfTermsNotAccepted() throws Exception {
        // given
        when(idmIntegrationService.getActiveTerms(any(), any())).thenReturn(activeTerms());
        TermsAndConditionsCallback callback = new TermsAndConditionsCallback(activeTerms().getVersion(),
                activeTerms().getTerms(), activeTerms().getCreateDate());

        TreeContext context = getContext(singletonList(callback), json(object()));

        // when
        Action action = acceptTermsAndConditionsNode.process(context);

        // then
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
    void shouldNotOverwriteExistingSharedStateObjects() throws Exception {
        // given
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

        // when
        Action action = acceptTermsAndConditionsNode.process(getContext(singletonList(callback), sharedState));

        // then
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

}
