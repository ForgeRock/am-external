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
 * Copyright 2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.ThrowableAssert;
import org.forgerock.openam.auth.nodes.LdapDecisionNode.LdapConnectionMode;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.sm.AnnotatedServiceRegistry;
import org.forgerock.openam.sm.ServiceConfigException;
import org.forgerock.openam.sm.ServiceErrorException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;

import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;

/**
 * Unit tests for {@link LdapDecisionNode}.
 */
@RunWith(JUnitParamsRunner.class)
public class LdapDecisionNodeConfigValidatorTest {

    private static final boolean MTLS_ON = true;
    private static final boolean MTLS_OFF = false;
    private static final Boolean MTLS_MISSING = null;
    private static final LdapConnectionMode MISSING_CONNECTION_MODE = null;
    private static final LdapConnectionMode LDAP_CONNECTION_MODE = LdapConnectionMode.LDAP;
    private static final LdapConnectionMode LDAPS_CONNECTION_MODE = LdapConnectionMode.LDAPS;
    private static final String MISSING_SECRET = null;
    private static final String BLANK_SECRET = "";
    private static final String INCLUDED_SECRET = "included-secret";
    private static final String USER_DN_MISSING = null;
    private static final String PASSWORD_MISSING = null;
    private static final String USER_DN_EMPTY = "";
    private static final String PASSWORD_EMPTY = "";
    private static final String USER_DN_INCLUDED = "includedUserDn";
    private static final String PASSWORD_INCLUDED = "includedUserPassword";
    private static final boolean EXPECT_FAILURE = false;
    private static final boolean EXPECT_SUCCESS = true;
    public static final List<String> CONFIG_PATH = List.of("LdapDecisionNode", "abc-123-xyz-890");

    @Mock
    private LdapDecisionNode.Config config;
    @Mock
    private AnnotatedServiceRegistry serviceRegistry;
    @Mock
    private Realm realm;
    private LdapDecisionNode.ConfigValidator configValidator;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setup() {
        configValidator = new LdapDecisionNode.ConfigValidator(serviceRegistry);
    }

    @NamedParameters("noExistingConfigDataMtlsEnabledAdminDnAndAdminPassword")
    private static Object[] noExistingConfigDataMtlsEnabledAdminDnAndAdminPassword() {
        return new Object[][]{
                {USER_DN_MISSING, PASSWORD_MISSING},
                {USER_DN_EMPTY, PASSWORD_EMPTY},
                {USER_DN_INCLUDED, PASSWORD_INCLUDED},
        };
    }

    @Test
    @Parameters(named = "noExistingConfigDataMtlsEnabledAdminDnAndAdminPassword")
    public void testValidatorWithNoExistingConfigurationAndMtlsEnabledShouldntCareAboutAdminUsernameAndPassword(
            String userName, String password)
            throws SMSException, SSOException {
        given(serviceRegistry.getRealmInstance(eq(LdapDecisionNode.Config.class), any(), any()))
                .willReturn(Optional.empty());
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put("mtlsEnabled", Set.of("true"));
        attributes.put("ldapConnectionMode", Set.of(LdapConnectionMode.LDAPS.name()));
        attributes.put("mtlsSecretLabel", Set.of("abcd"));

        if (userName != null) {
            attributes.put("adminDn", Set.of(userName));
        }
        if (password != null) {
            attributes.put("adminPassword", Set.of(password));
        }
        configValidator.validate(realm, CONFIG_PATH, attributes);
    }

    @NamedParameters("noExistingConfigDataAndMtlsDisabledConnectionModeAndSecretId")
    private static Object[] noExistingConfigDataAndMtlsDisabledConnectionModeAndSecretId() {
        return new Object[][]{
                {BLANK_SECRET},
                {MISSING_SECRET},
                {INCLUDED_SECRET},
        };
    }

    @Test
    @Parameters(named = "noExistingConfigDataAndMtlsDisabledConnectionModeAndSecretId")
    public void testValidatorWithNoExistingConfigurationAndMtlsDisabledShouldntCareAboutLdapConnectionModeOrSecretId(
            String secretId) throws SMSException, SSOException {
        given(serviceRegistry.getRealmInstance(eq(LdapDecisionNode.Config.class), any(), any()))
                .willReturn(Optional.empty());
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put("mtlsEnabled", Set.of("false"));
        attributes.put("ldapConnectionMode", Set.of(LdapConnectionMode.LDAP.name()));
        attributes.put("adminDn", Set.of("any"));
        attributes.put("adminPassword", Set.of("any"));
        if (secretId != null) {
            attributes.put("mtlsSecretLabel", Set.of(secretId));

        }
        configValidator.validate(realm, CONFIG_PATH, attributes);
    }

    @NamedParameters("noExistingConfigDataMtlsDisabled")
    private static Object[] noExistingConfigDataMtlsDisabled() {
        return new Object[][]{
                {USER_DN_MISSING, PASSWORD_MISSING, EXPECT_FAILURE},
                {USER_DN_MISSING, PASSWORD_EMPTY, EXPECT_FAILURE},
                {USER_DN_MISSING, PASSWORD_INCLUDED, EXPECT_FAILURE},
                {USER_DN_EMPTY, PASSWORD_EMPTY, EXPECT_FAILURE},
                {USER_DN_EMPTY, PASSWORD_MISSING, EXPECT_FAILURE},
                {USER_DN_EMPTY, PASSWORD_INCLUDED, EXPECT_FAILURE},
                {USER_DN_INCLUDED, PASSWORD_MISSING, EXPECT_FAILURE},
                {USER_DN_INCLUDED, PASSWORD_EMPTY, EXPECT_FAILURE},
                {USER_DN_INCLUDED, PASSWORD_INCLUDED, EXPECT_SUCCESS},
        };
    }

    @Test
    @Parameters(named = "noExistingConfigDataMtlsDisabled")
    public void testValidatorWithNoExistingConfigurationAndMtlsDisabled(String userName, String password,
            boolean expectSuccess) throws Throwable {
        given(serviceRegistry.getRealmInstance(eq(LdapDecisionNode.Config.class), any(), any()))
                .willReturn(Optional.empty());
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put("mtlsEnabled", Set.of("false"));
        attributes.put("ldapConnectionMode", Set.of(LdapConnectionMode.LDAP.name()));
        if (userName != null) {
            attributes.put("adminDn", Set.of(userName));
        }
        if (password != null) {
            attributes.put("adminPassword", Set.of(password));
        }
        ThrowableAssert.ThrowingCallable methodCall = () -> configValidator.validate(realm,
                CONFIG_PATH, attributes);
        if (expectSuccess) {
            methodCall.call();
        } else {
            assertThatThrownBy(methodCall).isInstanceOf(ServiceConfigException.class);
        }
    }

    @NamedParameters("noExistingConfigDataMtlsEnabled")
    private static Object[] noExistingConfigDataMtlsEnabled() {
        return new Object[][]{
                {LDAP_CONNECTION_MODE, BLANK_SECRET, EXPECT_FAILURE},
                {LDAPS_CONNECTION_MODE, BLANK_SECRET, EXPECT_FAILURE},
                {LDAP_CONNECTION_MODE, MISSING_SECRET, EXPECT_FAILURE},
                {LDAPS_CONNECTION_MODE, MISSING_SECRET, EXPECT_FAILURE},
                {LDAP_CONNECTION_MODE, INCLUDED_SECRET, EXPECT_FAILURE},
                {LDAPS_CONNECTION_MODE, INCLUDED_SECRET, EXPECT_SUCCESS},
        };
    }

    @Test
    @Parameters(named = "noExistingConfigDataMtlsEnabled")
    public void testValidatorWithNoExistingConfigurationAndMtlsEnabled(LdapConnectionMode ldapConnectionMode,
            String mtlsSecretLabel, boolean expectSuccess) throws Throwable {
        given(serviceRegistry.getRealmInstance(eq(LdapDecisionNode.Config.class), any(), any()))
                .willReturn(Optional.empty());
        Map<String, Set<String>> attributes = new HashMap<>();
        attributes.put("mtlsEnabled", Set.of("true"));
        if (ldapConnectionMode != null) {
            attributes.put("ldapConnectionMode", Set.of(ldapConnectionMode.name()));
        }
        if (mtlsSecretLabel != null) {
            attributes.put("mtlsSecretLabel", Set.of(mtlsSecretLabel));
        }
        ThrowableAssert.ThrowingCallable methodCall = () -> configValidator.validate(realm,
                CONFIG_PATH, attributes);
        if (expectSuccess) {
            methodCall.call();
        } else {
            assertThatThrownBy(methodCall).isInstanceOf(ServiceConfigException.class);
        }
    }

    @NamedParameters("existingConfigDataMtlsEnabled")
    private static Object[] existingConfigDataMtlsEnabled() {
        return new Object[][]{
                {MTLS_OFF, USER_DN_MISSING, PASSWORD_MISSING, MISSING_CONNECTION_MODE, MISSING_SECRET, EXPECT_FAILURE},
                {MTLS_ON, USER_DN_MISSING, PASSWORD_MISSING, MISSING_CONNECTION_MODE, MISSING_SECRET, EXPECT_SUCCESS},
                {MTLS_OFF, USER_DN_INCLUDED, PASSWORD_INCLUDED, LDAP_CONNECTION_MODE, BLANK_SECRET, EXPECT_SUCCESS},
                {MTLS_MISSING, USER_DN_INCLUDED, PASSWORD_INCLUDED, LDAP_CONNECTION_MODE, BLANK_SECRET, EXPECT_FAILURE},
        };
    }

    public void setupMtlsEnabledConfigMock() {
        given(config.mtlsEnabled()).willReturn(true);
        given(config.mtlsSecretLabel()).willReturn(Optional.of("abc"));
        given(config.adminDn()).willReturn(Optional.empty());
        given(config.adminPassword()).willReturn(Optional.empty());
        given(config.ldapConnectionMode()).willReturn(LdapConnectionMode.LDAPS);

    }

    @Test
    @Parameters(named = "existingConfigDataMtlsEnabled")
    public void testValidatorWithExistingConfigurationAndMtlsEnabled(Boolean mtlsEnabled, String userName,
            String password, LdapConnectionMode ldapConnectionMode, String mtlsSecretLabel, boolean expectSuccess)
            throws Throwable {
        setupMtlsEnabledConfigMock();
        given(config.mtlsEnabled()).willReturn(true);
        given(serviceRegistry.getRealmInstance(eq(LdapDecisionNode.Config.class), any(), any()))
                .willReturn(Optional.of(config));
        Map<String, Set<String>> attributes = new HashMap<>();
        if (mtlsEnabled != null) {
            attributes.put("mtlsEnabled", Set.of(mtlsEnabled.toString()));
        }
        if (userName != null) {
            attributes.put("adminDn", Set.of(userName));
        }
        if (password != null) {
            attributes.put("adminPassword", Set.of(password));
        }
        if (ldapConnectionMode != null) {
            attributes.put("ldapConnectionMode", Set.of(ldapConnectionMode.name()));
        }
        if (mtlsSecretLabel != null) {
            attributes.put("mtlsSecretLabel", Set.of(mtlsSecretLabel));
        }
        ThrowableAssert.ThrowingCallable methodCall = () -> configValidator.validate(realm,
                CONFIG_PATH, attributes);
        if (expectSuccess) {
            methodCall.call();
        } else {
            assertThatThrownBy(methodCall).isInstanceOf(ServiceConfigException.class);
        }
    }

    @NamedParameters("existingConfigDataMtlsDisabled")
    private static Object[] existingConfigDataMtlsDisabled() {
        return new Object[][]{
                {MTLS_ON, USER_DN_MISSING, PASSWORD_MISSING, MISSING_CONNECTION_MODE, MISSING_SECRET, EXPECT_FAILURE},
                {MTLS_OFF, USER_DN_MISSING, PASSWORD_MISSING, MISSING_CONNECTION_MODE, MISSING_SECRET, EXPECT_SUCCESS},
                {MTLS_ON, USER_DN_EMPTY, PASSWORD_EMPTY, LDAPS_CONNECTION_MODE, INCLUDED_SECRET, EXPECT_SUCCESS},
                {MTLS_MISSING, USER_DN_EMPTY, PASSWORD_EMPTY, LDAPS_CONNECTION_MODE, INCLUDED_SECRET, EXPECT_FAILURE},
        };
    }

    public void setupMtlsDisabledConfigMock() {
        given(config.mtlsEnabled()).willReturn(false);
        given(config.mtlsSecretLabel()).willReturn(Optional.empty());
        given(config.adminDn()).willReturn(Optional.of("an-admin-dn"));
        given(config.adminPassword()).willReturn(Optional.of("an-admin-password".toCharArray()));
        given(config.ldapConnectionMode()).willReturn(LdapConnectionMode.LDAP);
    }

    @Test
    @Parameters(named = "existingConfigDataMtlsDisabled")
    public void testValidatorWithExistingConfigurationAndMtlsDisabled(Boolean mtlsEnabled, String userName,
            String password, LdapConnectionMode ldapConnectionMode, String mtlsSecretLabel, boolean expectSuccess)
            throws Throwable {
        setupMtlsDisabledConfigMock();
        given(serviceRegistry.getRealmInstance(eq(LdapDecisionNode.Config.class), any(), any()))
                .willReturn(Optional.of(config));
        Map<String, Set<String>> attributes = new HashMap<>();
        if (mtlsEnabled != null) {
            attributes.put("mtlsEnabled", Set.of(mtlsEnabled.toString()));
        }
        if (userName != null) {
            attributes.put("adminDn", Set.of(userName));
        }
        if (password != null) {
            attributes.put("adminPassword", Set.of(password));
        }
        if (ldapConnectionMode != null) {
            attributes.put("ldapConnectionMode", Set.of(ldapConnectionMode.name()));
        }
        if (mtlsSecretLabel != null) {
            attributes.put("mtlsSecretLabel", Set.of(mtlsSecretLabel));
        }
        ThrowableAssert.ThrowingCallable methodCall = () -> configValidator.validate(realm,
                CONFIG_PATH, attributes);
        if (expectSuccess) {
            methodCall.call();
        } else {
            assertThatThrownBy(methodCall).isInstanceOf(ServiceConfigException.class);
        }
    }

    @Test
    public void testGivenServiceRegistryThrowsExceptionValidatorThrowsServiceErrorException()
            throws SMSException, SSOException {
        given(serviceRegistry.getRealmInstance(eq(LdapDecisionNode.Config.class), any(), any()))
                .willThrow(new SMSException());
        assertThatThrownBy(() -> configValidator.validate(realm, CONFIG_PATH, Map.of()))
                .isInstanceOf(ServiceErrorException.class);
    }

    @Test
    public void testGivenNullRealmValidateThrowsNullPointer() {
        assertThatThrownBy(() -> configValidator.validate(null, CONFIG_PATH, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testGivenNullConfigPathValidateThrowsNullPointer() {
        assertThatThrownBy(() -> configValidator.validate(realm, null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testGivenNullAttributesValidateThrowsNullPointer() {
        assertThatThrownBy(() -> configValidator.validate(realm, CONFIG_PATH, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testGivenEmptyConfigPathValidateThrowsIllegalArgument() {
        assertThatThrownBy(() -> configValidator.validate(realm, List.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}