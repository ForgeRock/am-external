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
 * Copyright 2023-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes;

import static com.sun.identity.idm.IdType.USER;
import static java.util.Collections.emptyList;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.LdapDecisionNode.HeartbeatTimeUnit.MINUTES;
import static org.forgerock.openam.auth.nodes.LdapDecisionNode.SearchScope.SUBTREE;
import static org.forgerock.openam.ldap.ModuleState.SUCCESS;
import static org.forgerock.openam.ldap.ModuleState.USER_NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.ldap.ConnectionFactoryAuditWrapperFactory;
import org.forgerock.openam.ldap.LDAPAuthUtils;
import org.forgerock.openam.ldap.LDAPUtilException;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.test.extensions.LoggerExtension;
import org.forgerock.opendj.ldap.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.service.AMAccountLockoutTrees;
import com.sun.identity.idm.IdType;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Unit tests for {@link LdapDecisionNode}.
 */
@ExtendWith(MockitoExtension.class)
public class LdapDecisionNodeTest {

    @RegisterExtension
    LoggerExtension loggerExtension = new LoggerExtension(LdapDecisionNode.class);

    @Mock
    private LdapDecisionNode.Config config;

    @Mock
    private CoreWrapper coreWrapper;

    @Mock
    private LegacyIdentityService identityService;

    @Mock
    private LDAPAuthUtils ldapAuthUtils;

    @Mock
    private Realm realm;

    @Mock
    private Secrets secrets;
    @Mock
    private ConnectionFactoryAuditWrapperFactory connectionFactoryAuditWrapperFactory;
    @Mock
    private AMAccountLockoutTrees.Factory accountLockoutTreesFactory;
    private LdapDecisionNode node;

    @BeforeEach
    void setUp() throws Exception {
        given(config.searchScope()).willReturn(SUBTREE);
        given(config.heartbeatTimeUnit()).willReturn(MINUTES);
        given(coreWrapper.getLDAPAuthUtils(anySet(), anySet(),
                anyBoolean(), any(), anyString(), any())).willReturn(ldapAuthUtils);
        node = new LdapDecisionNode(config, realm, coreWrapper, identityService, secrets,
                connectionFactoryAuditWrapperFactory, accountLockoutTreesFactory);
    }

    @Test
    void testProcessAddsIdentifiedIdentityOfExistingUser() throws Exception {
        //Given
        JsonValue sharedState = json(object(field("username", "testUsername@example.com"),
                field("password", "password"),
                field("realm", "testRealm")));
        given(ldapAuthUtils.getUserId()).willReturn("testUser");
        given(ldapAuthUtils.getState()).willReturn(SUCCESS);

        //When
        Action result = node.process(getContext(sharedState));

        // Then
        Assertions.assertThat(result.identifiedIdentity).isPresent();
        IdentifiedIdentity idid = result.identifiedIdentity.get();
        Assertions.assertThat(idid.getUsername()).isEqualTo("testUser");
        Assertions.assertThat(idid.getIdentityType()).isEqualTo(IdType.USER);
    }

    @Test
    void testProcessDoesNotAddIdentifiedIdentityOfNonExistentUser() throws Exception {
        //Given
        JsonValue sharedState = json(object(field("username", "testUsername@example.com"),
                field("password", "password"),
                field("realm", "testRealm")));
        given(ldapAuthUtils.getUserId()).willReturn("testUser");
        given(ldapAuthUtils.getState()).willReturn(USER_NOT_FOUND);

        //When
        Action result = node.process(getContext(sharedState));

        // Then
        Assertions.assertThat(result.identifiedIdentity).isEmpty();
    }

    @Test
    void shouldUpdateSharedStateWithCorrectUsername() throws Exception {

        //Given
        JsonValue sharedState = json(object(field("username", "testUsername@example.com"),
                field("password", "password"),
                field("realm", "testRealm")));
        given(ldapAuthUtils.getUserId()).willReturn("testUser");
        given(ldapAuthUtils.getState()).willReturn(SUCCESS);

        //When
        Action result = node.process(getContext(sharedState));

        //Then
        verify(ldapAuthUtils).setConnectionFactoryAuditWrapperFactory(connectionFactoryAuditWrapperFactory);
        verify(identityService, only()).getUniversalId("testUser", "testRealm", USER);
        assertThat(result.sharedState).isObject().contains(USERNAME, "testUser");
    }

    @Test
    void shouldSetCorrectUniversalIdOnAction() throws Exception {

        //Given
        JsonValue sharedState = json(object(field("username", "testUsername@example.com"),
                field("password", "password"),
                field("realm", "testRealm")));
        given(ldapAuthUtils.getUserId()).willReturn("testUser");
        given(ldapAuthUtils.getState()).willReturn(SUCCESS);
        given(identityService.getUniversalId("testUser", "testRealm", USER))
                .willReturn(Optional.of("uid=testUsername@example.com,ou=people,dc=openam,dc=forgerock,dc=org"));

        //When
        Action result = node.process(getContext(sharedState));

        //Then
        verify(ldapAuthUtils).setConnectionFactoryAuditWrapperFactory(connectionFactoryAuditWrapperFactory);
        Assertions.assertThat(result.universalId.get())
                .isEqualTo("uid=testUsername@example.com,ou=people,dc=openam,dc=forgerock,dc=org");
    }

    @Test
    void shouldLogErrorWhenLdapUtilThrowsErrors() throws NodeProcessException, LDAPUtilException {
        // given
        doThrow(new LDAPUtilException("test LDAP utils exception", ResultCode.OTHER))
                .when(ldapAuthUtils).authenticateUser(any(), any(), eq(accountLockoutTreesFactory));
        JsonValue sharedState = json(object(field("username", "testUsername@example.com"),
                field("password", "password"),
                field("realm", "testRealm")));

        // when
        node.process(getContext(sharedState));

        // then
        Assertions.assertThat(loggerExtension.getErrors(ILoggingEvent::getFormattedMessage))
                .contains("LDAPUtilException: test LDAP utils exception");
        Assertions.assertThat(loggerExtension.getDebug(e -> {
                    if (e.getThrowableProxy() != null) {
                        return e.getThrowableProxy().getMessage();
                    } else {
                        return null;
                    }
                }))
                .contains("test LDAP utils exception");
    }

    @Test
    void shouldLogInfoWhenLdapUtilThrowsInvalidCredentialError() throws NodeProcessException, LDAPUtilException {
        // given
        doThrow(new LDAPUtilException("test LDAP utils exception", ResultCode.INVALID_CREDENTIALS))
                .when(ldapAuthUtils).authenticateUser(any(), any(), eq(accountLockoutTreesFactory));
        JsonValue sharedState = json(object(field("username", "testUsername@example.com"),
                field("password", "password"),
                field("realm", "testRealm")));

        // when
        node.process(getContext(sharedState));

        // then
        Assertions.assertThat(loggerExtension.getInfo(ILoggingEvent::getFormattedMessage))
                .contains("LDAPUtilException: test LDAP utils exception");
        Assertions.assertThat(loggerExtension.getDebug(e -> {
                    if (e.getThrowableProxy() != null) {
                        return e.getThrowableProxy().getMessage();
                    } else {
                        return null;
                    }
                }))
                .contains("test LDAP utils exception");
    }

    private TreeContext getContext(JsonValue sharedState) {
        return new TreeContext(sharedState,
                new ExternalRequestContext.Builder().build(), emptyList(), Optional.empty());
    }
}
