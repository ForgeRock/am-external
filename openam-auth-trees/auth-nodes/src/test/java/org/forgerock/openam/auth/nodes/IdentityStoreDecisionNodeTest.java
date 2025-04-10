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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.ONE_TIME_PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.PASSWORD;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.TEST_UNIVERSAL_ID;
import static org.forgerock.openam.auth.nodes.TreeContextFactory.newTreeContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.ldap.ConnectionFactoryAuditWrapperFactory;
import org.forgerock.openam.ldap.LDAPAuthUtils;
import org.forgerock.openam.ldap.LDAPUtilException;
import org.forgerock.openam.ldap.ModuleState;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.sm.ServiceConfigManagerFactory;
import org.forgerock.opendj.ldap.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.common.IdRepoUtilityService;

@ExtendWith(MockitoExtension.class)
public class IdentityStoreDecisionNodeTest {

    private static final String TEST_REALM = "testRealm";
    private static final String TEST_USERNAME = "testUsername";
    private static final String TEST_PASSWORD = "password";
    private static final String TEST_ONE_TIME_PASSWORD = "978413";

    @Mock
    private ServiceConfigManagerFactory configManagerFactory;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private LegacyIdentityService identityService;
    @Mock
    private AMIdentity amIdentity;
    @Mock
    private IdentityStoreDecisionNode.Config serviceConfig;
    @Mock
    private LdapDecisionNode.Config ldapConfig;
    @Mock
    private LDAPAuthUtils ldapAuthUtils;
    @Mock
    private PasswordCallback oldPasswordCalback;
    @Mock
    private PasswordCallback newPasswordCalback;
    @Mock
    private PasswordCallback confirmPasswordCalback;
    @Mock
    private ConfirmationCallback confirmationCallback;
    @Mock
    private IdRepoUtilityService idRepoUtilityService;
    @Mock
    private Realm realm;
    @Mock
    private Secrets secrets;
    @Mock
    private ConnectionFactoryAuditWrapperFactory connectionFactoryAuditWrapperFactory;

    private JsonValue sharedState;
    private JsonValue secureState;
    private IdentityStoreDecisionNode identityStoreDecisionNode;

    @BeforeEach
    void setUp() throws NodeProcessException, LDAPUtilException {
        given(serviceConfig.getConfig(configManagerFactory, idRepoUtilityService)).willReturn(ldapConfig);
        given(ldapConfig.searchScope()).willReturn(LdapDecisionNode.SearchScope.OBJECT);
        given(ldapConfig.heartbeatTimeUnit()).willReturn(LdapDecisionNode.HeartbeatTimeUnit.SECONDS);
        given(coreWrapper.getLDAPAuthUtils(anySet(), anySet(), anyBoolean(), any(), anyString(), any()))
                .willReturn(ldapAuthUtils);

        sharedState = json(object(
                field(USERNAME, TEST_USERNAME),
                field(PASSWORD, TEST_PASSWORD),
                field(REALM, TEST_REALM)
        ));

        secureState = json(object(field(ONE_TIME_PASSWORD, TEST_ONE_TIME_PASSWORD)));
        identityStoreDecisionNode = new IdentityStoreDecisionNode(serviceConfig, realm, configManagerFactory,
                coreWrapper, identityService, idRepoUtilityService, secrets, connectionFactoryAuditWrapperFactory);
    }

    @Test
    void processSuccessfulLogin() throws NodeProcessException {

        given(ldapAuthUtils.getState()).willReturn(ModuleState.SUCCESS);
        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));
        assertThat(String.valueOf(LdapDecisionNode.LdapOutcome.TRUE)).isEqualTo(action.outcome);
    }

    @Test
    void incorrectUsernameResultsInLoginFailed() throws NodeProcessException {

        given(ldapAuthUtils.getState()).willReturn(ModuleState.USER_NOT_FOUND);
        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));
        assertThat(String.valueOf(LdapDecisionNode.LdapOutcome.FALSE)).isEqualTo(action.outcome);
    }

    @Test
    void incorrectPasswordResultsInLoginFailed() throws NodeProcessException, LDAPUtilException {

        LDAPUtilException credInvalid = new LDAPUtilException("Invalid credentials.", ResultCode.INVALID_CREDENTIALS);

        doThrow(credInvalid).when(ldapAuthUtils).authenticateUser(anyString(), anyString());

        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));

        assertThat(action.outcome)
                .withFailMessage("Outcome was not correct")
                .isEqualTo(String.valueOf(LdapDecisionNode.LdapOutcome.FALSE));
    }

    @Test
    void passwordIsDueToExpireResultsInPasswordChangeFlow() throws NodeProcessException {

        given(ldapAuthUtils.getState()).willReturn(ModuleState.PASSWORD_EXPIRING);
        identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));

        assertThat(sharedState.get("LdapFlowState").toString())
                .withFailMessage("Context does not contain change password flow state")
                .isEqualTo("PASSWORD_CHANGE");
    }

    @Test
    void forceChangeOnAddResultsInPasswordChangeFlow() throws NodeProcessException {

        given(ldapAuthUtils.getState()).willReturn(ModuleState.CHANGE_AFTER_RESET);
        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));

        assertThat(action.sharedState.get("lastModuleState").toString())
                .withFailMessage("Context does not contain change password flow state")
                .isEqualTo("changeAfterReset");

    }

    @Test
    void accountLockoutResultsInLockedOutcomeWhenCorrectCreds() throws NodeProcessException {
        given(ldapAuthUtils.getState()).willReturn(ModuleState.ACCOUNT_LOCKED);
        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));

        assertThat(action.outcome)
                .withFailMessage("Outcome was not correct")
                .isEqualTo(String.valueOf(LdapDecisionNode.LdapOutcome.LOCKED));
        assertThat(action.lockoutMessage).isEqualTo("User Locked Out.");
        assertThat(action.errorMessage).isEqualTo("User Locked Out.");
    }

    @Test
    void accountLockoutResultsInLockedOutcomeWhenIncorrectCreds() throws Exception {
        doThrow(new LDAPUtilException("Invalid credentials.", ResultCode.INVALID_CREDENTIALS))
                .when(ldapAuthUtils).authenticateUser(anyString(), anyString());
        given(ldapAuthUtils.getUserAttributeValues()).willReturn(new HashMap<>() {{
            put("inetuserstatus", Set.of("Inactive"));
        }});
        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));

        assertThat(action.outcome)
                .withFailMessage("Outcome was not correct")
                .isEqualTo(String.valueOf(LdapDecisionNode.LdapOutcome.LOCKED));
        assertThat(action.lockoutMessage).isEqualTo("User Locked Out.");
        assertThat(action.errorMessage).isNull();
    }

    @Test
    void passwordExpiredWithGracePeriodResultsInChangePasswordCallbacks() throws NodeProcessException {

        given(ldapAuthUtils.getState()).willReturn(ModuleState.GRACE_LOGINS);
        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));

        assertThat(action.callbacks)
                .withFailMessage("Callbacks do not contain expired password message")
                .satisfies(callbacks -> {
                    assertThat(callbacks.stream()
                            .filter((c) -> c instanceof TextOutputCallback)
                            .anyMatch((it) -> "YOUR PASSWORD HAS EXPIRED AND YOU HAVE 0 GRACE LOGINS REMAINING."
                                    .equals(((TextOutputCallback) it).getMessage())))
                            .isTrue();
                });
    }

    @Test
    void passwordResetResultsInPasswordChangeFlow() throws NodeProcessException {

        given(ldapAuthUtils.getState()).willReturn(ModuleState.PASSWORD_RESET_STATE);
        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));

        assertThat(action.sharedState.get("lastModuleState").toString())
                .withFailMessage("Context does not contain change password flow state")
                .isEqualTo("changeAfterReset");

    }

    @Test
    void passwordChangeWithNullUserDnThrowsNodeProcessException() {

        //given
        given(confirmationCallback.getSelectedIndex()).willReturn(0);
        given(oldPasswordCalback.getPassword()).willReturn("password".toCharArray());
        given(newPasswordCalback.getPassword()).willReturn("newPassword".toCharArray());
        given(confirmPasswordCalback.getPassword()).willReturn("newPassword".toCharArray());

        // when
        assertThatThrownBy(
                () -> identityStoreDecisionNode.process(newTreeContext(sharedState, secureState).copyWithCallbacks(
                        List.of(oldPasswordCalback, newPasswordCalback, confirmPasswordCalback, confirmationCallback))))
                //then
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Invalid LDAP node configuration.");
    }

    @Test
    void shouldUseUniversalIdForUsername() throws NodeProcessException {
        // Given
        given(ldapAuthUtils.getState()).willReturn(ModuleState.SUCCESS);
        given(ldapAuthUtils.getUserId()).willReturn(TEST_UNIVERSAL_ID);
        given(serviceConfig.useUniversalIdForUsername()).willReturn(true);

        // When
        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));

        // Then
        assertThat(action.sharedState.get(USERNAME).asString()).isEqualTo(TEST_UNIVERSAL_ID);
    }

    @Test
    void shouldNotUseUniversalIdForUsername() throws NodeProcessException {
        // Given
        given(ldapAuthUtils.getState()).willReturn(ModuleState.SUCCESS);
        given(ldapAuthUtils.getUserId()).willReturn(TEST_UNIVERSAL_ID);
        given(serviceConfig.useUniversalIdForUsername()).willReturn(false);

        // When
        Action action = identityStoreDecisionNode.process(newTreeContext(sharedState, secureState));

        // Then
        assertThat(action.sharedState.get(USERNAME).asString()).isEqualTo(TEST_USERNAME);
    }

}
