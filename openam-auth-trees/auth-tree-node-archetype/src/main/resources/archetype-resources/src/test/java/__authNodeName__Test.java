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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package ${package};

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.InputState;
import org.forgerock.openam.auth.node.api.OutputState;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtilsWrapper;

@ExtendWith(MockitoExtension.class)
class ${authNodeName}Test {

    @Mock
    private Realm mockRealm;
    @Mock
    private IdUtilsWrapper idUtils;
    @Mock
    private AMIdentity userIdentity;
    @Mock
    private AMIdentity groupIdentity;

    private ${authNodeName} node;
    private ${authNodeName}.Config config;
    private JsonValue sharedState;
    private JsonValue transientState;
    private final String username = "demo";
    private final String password = "password123";

    @BeforeEach
    void setup() {
        config = new ${authNodeName}.Config() { };
        node = new ${authNodeName}(config, mockRealm, idUtils);
        sharedState = json(object());
        transientState = json(object());
    }

    @Test
    void shouldGoToTrueWhenGroupMatchesInProcessMethod() throws Exception {
        // given
        ExternalRequestContext requestContext = createRequestContext(
                config.usernameHeader(), username, config.passwordHeader(), password);
        TreeContext context = createTreeContext(sharedState, transientState, requestContext);

        when(idUtils.getIdentity(username, null)).thenReturn(userIdentity);
        when(userIdentity.isExists()).thenReturn(true);
        when(userIdentity.isActive()).thenReturn(true);
        when(userIdentity.getMemberships(IdType.GROUP)).thenReturn(Set.of(groupIdentity));
        when(groupIdentity.getName()).thenReturn(config.groupName().get());

        // when
        Action action = node.process(context);
        JsonValue auditEntry = node.getAuditEntryDetail();

        // then
        assertThat(action.outcome).isEqualTo("true");
        assertThat(sharedState.get(USERNAME).asString()).isEqualTo(username);
        assertThat(transientState.get(PASSWORD).asString()).isEqualTo(password);
        assertThat(auditEntry.get("username").asString()).isEqualTo(username);
    }

    @Test
    void shouldGoToTrueWhenGroupMatchesDnInProcessMethod() throws Exception {
        // given
        ExternalRequestContext requestContext = createRequestContext(
                config.usernameHeader(), username, config.passwordHeader(), password);
        TreeContext context = createTreeContext(sharedState, transientState, requestContext);

        when(idUtils.getIdentity(username, null)).thenReturn(userIdentity);
        when(userIdentity.isExists()).thenReturn(true);
        when(userIdentity.isActive()).thenReturn(true);
        when(userIdentity.getMemberships(IdType.GROUP)).thenReturn(Set.of(groupIdentity));
        when(groupIdentity.getName()).thenReturn("uid=" + config.groupName().get() + ",ou=groups,dc=example,dc=com");

        // when
        Action action = node.process(context);
        JsonValue auditEntry = node.getAuditEntryDetail();

        // then
        assertThat(action.outcome).isEqualTo("true");
        assertThat(sharedState.get(USERNAME).asString()).isEqualTo(username);
        assertThat(transientState.get(PASSWORD).asString()).isEqualTo(password);
        assertThat(auditEntry.get("username").asString()).isEqualTo(username);
    }

    @Test
    void shouldGoToFalseWhenGroupDoesNotMatchInProcessMethod() throws Exception {
        // given
        ExternalRequestContext requestContext = createRequestContext(
                config.usernameHeader(), username, config.passwordHeader(), password);
        TreeContext context = createTreeContext(sharedState, transientState, requestContext);

        when(idUtils.getIdentity(username, null)).thenReturn(userIdentity);
        when(userIdentity.isExists()).thenReturn(true);
        when(userIdentity.isActive()).thenReturn(true);
        when(userIdentity.getMemberships(IdType.GROUP)).thenReturn(Set.of(groupIdentity));
        when(groupIdentity.getName()).thenReturn("anotherGroupName");

        // when
        Action action = node.process(context);
        JsonValue auditEntry = node.getAuditEntryDetail();

        // then
        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.keys()).doesNotContain(USERNAME);
        assertThat(transientState.keys()).doesNotContain(PASSWORD);
        assertThat(auditEntry.keys()).doesNotContain("username");
    }

    @Test
    void shouldGoToTrueWhenUsernameAndPasswordAlreadyDefinedInProcessMethod() {
        // given
        ExternalRequestContext requestContext = createRequestContext();
        sharedState = json(object(field(USERNAME, username)));
        transientState = json(object(field(PASSWORD, password)));
        TreeContext context = createTreeContext(sharedState, transientState, requestContext);

        // when
        Action action = node.process(context);
        JsonValue auditEntry = node.getAuditEntryDetail();

        // then
        assertThat(action.outcome).isEqualTo("true");
        assertThat(sharedState.get(USERNAME).asString()).isEqualTo(username);
        assertThat(transientState.get(PASSWORD).asString()).isEqualTo(password);
        assertThat(auditEntry.get("username").asString()).isEqualTo(username);
    }

    @Test
    void shouldGoToFalseWhenUsernameAndPasswordMissingFromHeadersInProcessMethod() {
        // given
        ExternalRequestContext requestContext = createRequestContext();
        TreeContext context = createTreeContext(sharedState, transientState, requestContext);

        // when
        Action action = node.process(context);
        JsonValue auditEntry = node.getAuditEntryDetail();

        // then
        assertThat(action.outcome).isEqualTo("false");
        assertThat(sharedState.keys()).doesNotContain(USERNAME);
        assertThat(transientState.keys()).doesNotContain(PASSWORD);
        assertThat(auditEntry.keys()).doesNotContain("username");
    }

    @Test
    void shouldReturnUsernameAndPasswordWhenGetInputs() {
        // when
        InputState[] inputs = node.getInputs();

        // then
        assertThat(inputs[0].name).isEqualTo("username");
        assertThat(inputs[0].required).isFalse();
        assertThat(inputs[1].name).isEqualTo("password");
        assertThat(inputs[1].required).isFalse();
    }

    @Test
    void shouldReturnUsernameAndPasswordWhenGetOutputs() {
        // when
        OutputState[] inputs = node.getOutputs();

        // then
        assertThat(inputs[0].name).isEqualTo("username");
        assertThat(inputs[0].outcomes)
                .containsExactlyInAnyOrderEntriesOf(Map.of("true", true, "false", false));
        assertThat(inputs[1].name).isEqualTo("password");
        assertThat(inputs[1].outcomes).containsExactlyInAnyOrderEntriesOf(Map.of("true", true, "false", false));
    }

    private ExternalRequestContext createRequestContext(String... keysOrValues) {
        ListMultimap<String, String> headers = ArrayListMultimap.create();
        for (int i = 0; i < keysOrValues.length; i += 2) {
            headers.put(keysOrValues[i], keysOrValues[i + 1]);
        }
        return new ExternalRequestContext.Builder().headers(headers).build();
    }

    private TreeContext createTreeContext(JsonValue sharedState, JsonValue transientState,
            ExternalRequestContext requestContext) {
        return new TreeContext(sharedState, transientState, json(object()),
                requestContext, List.of(), false, Optional.empty());
    }
}
