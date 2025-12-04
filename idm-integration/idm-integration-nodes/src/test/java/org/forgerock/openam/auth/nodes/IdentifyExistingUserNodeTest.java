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
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.ResourceException.NOT_FOUND;
import static org.forgerock.json.resource.ResourceException.newResourceException;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.FALSE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.AbstractDecisionNode.TRUE_OUTCOME_ID;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.IdentifyExistingUserNode.IDM_IDPS;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_IDENTITY_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.DEFAULT_IDM_MAIL_ATTRIBUTE;
import static org.forgerock.openam.integration.idm.IdmIntegrationService.OBJECT_ATTRIBUTES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.stream.Stream;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.test.assertj.AssertJJsonValueAssert;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeUserIdentityProvider;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.integration.idm.IdmIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;

import com.iplanet.am.util.SystemPropertiesWrapper;
import com.sun.identity.idm.IdType;

@ExtendWith(MockitoExtension.class)
public class IdentifyExistingUserNodeTest {

    @Mock
    private IdentifyExistingUserNode.Config config;

    @Mock
    private Realm realm;

    @Mock
    private IdmIntegrationService idmIntegrationService;

    @Mock
    private NodeUserIdentityProvider identityProvider;

    @Mock
    private SystemPropertiesWrapper systemProperties;

    @InjectMocks
    private IdentifyExistingUserNode identifyExistingUserNode;

    private JsonValue transientState;

    public static Stream<Arguments> userData() {
        return Stream.of(
                Arguments.of(
                        json(object(
                                field("_id", "testId"),
                                field("userName", "test"),
                                field("mail", "test@test.com"))
                        ),
                        objectAttributeValues(),
                        TRUE_OUTCOME_ID,
                        json(object(
                                field("_id", "testId"),
                                field("aliasList", null)
                        )),
                        json(object())

                ),
                Arguments.of(
                        json(object(
                                        field("_id", "testId"),
                                        field("userName", "test2"),
                                        field("mail", "test@test.com"),
                                        field("aliasList", array("googleId"))
                                )
                        ),
                        objectAttributeValues(),
                        TRUE_OUTCOME_ID,
                        json(object(
                                        field("_id", "testId")
                                )
                        ),
                        json(object(
                                field("aliasList", array("googleId"))
                        ))
                ),
                Arguments.of(
                        json(object(
                                field("_id", "testId"),
                                field("userName", "test3"),
                                field("mail", "test@test.com"),
                                field("aliasList", array("googleId")))
                        ),
                        objectAttributeValues(),
                        TRUE_OUTCOME_ID,
                        json(object(
                            field("_id", "testId")
                        )),
                        json(object(
                                field("aliasList", array("googleId")))
                        )
                )
        );
    }

    private static JsonValue objectAttributeValues() {
        return json(object(field(OBJECT_ATTRIBUTES,
                object(
                        field("mail", "test@test.com")
                )
        )));
    }

    @BeforeEach
    void setUp() throws Exception {
        when(config.identityAttribute()).thenReturn(DEFAULT_IDM_MAIL_ATTRIBUTE);
        when(idmIntegrationService.getAttributeFromContext(any(), any())).thenCallRealMethod();
        lenient().when(systemProperties.getAsBoolean(
                        "org.forgerock.nodes.product.IdentifyExistingUserNode.alwaysSetUniversalId", true))
                .thenReturn(true);

        transientState = json(object());
        lenient().when(idmIntegrationService.storeListAttributeInState(eq(transientState), any(), any()))
                .thenCallRealMethod();

        lenient().when(config.identifier()).thenReturn(DEFAULT_IDM_IDENTITY_ATTRIBUTE);
    }


    @Test
    void shouldFailIfNoUserExist() throws Exception {
        whenGetIdmObject().thenThrow(newResourceException(NOT_FOUND));

        final Action process = identifyExistingUserNode.process(getContext(objectAttributeValues(), transientState));

        assertThat(process.outcome).isEqualTo(FALSE_OUTCOME_ID);

        assertThat(process.sharedState).isNull();
        assertThat(process.transientState).isNull();

        assertThat(process.universalId).isNotPresent();
        assertThat(process.identifiedIdentity).isNotPresent();
    }

    @ParameterizedTest
    @MethodSource("userData")
    public void shouldReturnExpectedOutcome(JsonValue user, JsonValue sharedState, String expectedOutcome,
                                            JsonValue expectedSharedState, JsonValue expectedTransientState)
            throws Exception {
        // given
        whenGetIdmObject().thenReturn(user);

        final String universalId = "uid=testId,ou=user,dc=openam,dc=forgerock,dc=org";
        when(identityProvider.getUniversalId(any(), any())).thenReturn(Optional.of(universalId));

        // when
        final Action process = identifyExistingUserNode.process(getContext(sharedState, transientState));

        // then
        assertThat(process.outcome).isEqualTo(expectedOutcome);
        assertIdAndAliasList(expectedSharedState, expectedTransientState, process);
        assertThatJson(process.sharedState).hasPath(USERNAME).isEqualTo(user.get("userName"));

        assertThat(process.universalId).isEqualTo(Optional.of(universalId));
        assertIdentifiedIdentity(user.get("userName"), process.identifiedIdentity);
    }

    @ParameterizedTest
    @MethodSource("userData")
    public void shouldReturnExistingUniversalIdWhenAlwaysSetUniversalIdFlagIsFalse(
            JsonValue user, JsonValue sharedState, String expectedOutcome, JsonValue expectedSharedState,
            JsonValue expectedTransientState)
            throws Exception {
        // given
        lenient().when(systemProperties.getAsBoolean(
                        "org.forgerock.nodes.product.IdentifyExistingUserNode.alwaysSetUniversalId", true))
                .thenReturn(false);
        final String existingUniversalId = "uid=existingTestId,ou=user,dc=openam,dc=forgerock,dc=org";

        whenGetIdmObject().thenReturn(user);

        // when
        final Action process = identifyExistingUserNode.process(
                getContext(sharedState, transientState, existingUniversalId));

        // then
        assertThat(process.outcome).isEqualTo(expectedOutcome);
        assertIdAndAliasList(expectedSharedState, expectedTransientState, process);
        assertThatJson(process.sharedState).hasPath(USERNAME).isEqualTo(user.get("userName"));

        assertThat(process.universalId).isEqualTo(Optional.of(existingUniversalId));
        assertIdentifiedIdentity(user.get("userName"), process.identifiedIdentity);
    }

    @ParameterizedTest
    @MethodSource("userData")
    public void shouldGetUniversalIdFromUsernameInGlobalStateWhenAlwaysSetUniversalIdFlagIsFalse(
            JsonValue user, JsonValue sharedState, String expectedOutcome, JsonValue expectedSharedState,
            JsonValue expectedTransientState)
            throws Exception {
        // given
        lenient().when(systemProperties.getAsBoolean(
                        "org.forgerock.nodes.product.IdentifyExistingUserNode.alwaysSetUniversalId", true))
                .thenReturn(false);

        final String universalId = "uid=testId,ou=user,dc=openam,dc=forgerock,dc=org";
        when(identityProvider.getUniversalId(any())).thenReturn(Optional.of(universalId));

        whenGetIdmObject().thenReturn(user);

        // when
        final Action process = identifyExistingUserNode.process(getContext(sharedState, transientState));

        // then
        assertThat(process.outcome).isEqualTo(expectedOutcome);
        assertIdAndAliasList(expectedSharedState, expectedTransientState, process);
        assertThatJson(process.sharedState).hasPath(USERNAME).isEqualTo(user.get("userName"));

        assertThat(process.universalId).isEqualTo(Optional.of(universalId));
        assertIdentifiedIdentity(user.get("userName"), process.identifiedIdentity);
    }

    private static void assertIdentifiedIdentity(final JsonValue username,
                                                 final Optional<IdentifiedIdentity> identifiedIdentity) {
        assertThat(identifiedIdentity).isPresent();
        IdentifiedIdentity idid = identifiedIdentity.get();
        assertThat(idid.getUsername()).isEqualTo(username.asString());
        assertThat(idid.getIdentityType()).isEqualTo(IdType.USER);
    }

    /*
        Some customers have an {@link org.forgerock.openam.auth.nodes.IdentifyExistingUserNode.Config.identifier} which
        is not in the IDM response, in this case some of the details are still set in the shared state.
     */
    @ParameterizedTest
    @MethodSource("userData")
    public void shouldSetStateAndIdentifierIfIdentifierNotFoundInResponse(
            JsonValue user, JsonValue sharedState, String expectedOutcome, JsonValue expectedSharedState,
            JsonValue expectedTransientState)
            throws Exception {
        // given
        when(config.identifier()).thenReturn("DOES_NOT_EXIST_IN_IDM_RESPONSE");
        whenGetIdmObject().thenReturn(user);

        // when
        final Action process = identifyExistingUserNode.process(getContext(sharedState, transientState));

        // then
        assertThat(process.outcome).isEqualTo(expectedOutcome);
        assertIdAndAliasList(expectedSharedState, expectedTransientState, process);
        assertThatJson(process.sharedState).doesNotContain(USERNAME);

        assertThat(process.universalId).isNotPresent();
        assertThat(process.identifiedIdentity).isNotPresent();
    }

    private void assertIdAndAliasList(final JsonValue expectedSharedState, final JsonValue expectedTransientState, final Action process) {
        assertThatJson(process.sharedState).hasPath("_id")
                .isEqualTo(expectedSharedState.get("_id"));
        // currently the node updates the transient state with the IDM_IDPS attribute directly and doesn't set this on
        // the action which I think still works to update the state
        assertThatJson(transientState.get("objectAttributes").get(IDM_IDPS))
                .isEqualTo(expectedTransientState.get(IDM_IDPS));
    }

    private OngoingStubbing<JsonValue> whenGetIdmObject() throws ResourceException {
        return when(idmIntegrationService.getObject(any(), any(), any(), any(), any(), any()));
    }

    private static AssertJJsonValueAssert.AbstractJsonValueAssert assertThatJson(final JsonValue jsonValue) {
        return AssertJJsonValueAssert.assertThat(jsonValue);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState) {
        return getContext(sharedState, transientState, null);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, String universalId) {
        return new TreeContext(TreeContext.DEFAULT_IDM_IDENTITY_RESOURCE, sharedState, transientState,
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.ofNullable(universalId));
    }

}
