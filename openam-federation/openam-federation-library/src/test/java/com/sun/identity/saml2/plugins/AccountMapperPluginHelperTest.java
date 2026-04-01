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
 * Copyright 2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins;

import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

public class AccountMapperPluginHelperTest {

    private static MockedStatic<SAML2Utils> saml2UtilsMockedStatic;
    private static MockedStatic<SAML2MetaUtils> saml2MetaUtilsMockedStatic;

    private AccountMapperPluginHelper pluginHelper;

    @BeforeEach
    void setUp() {
        pluginHelper = spy(new AccountMapperPluginHelper());
        saml2UtilsMockedStatic = mockStatic(SAML2Utils.class);
        saml2MetaUtilsMockedStatic = mockStatic(SAML2MetaUtils.class);
    }

    @AfterEach
    void tearDown() {
        saml2UtilsMockedStatic.close();
        saml2MetaUtilsMockedStatic.close();
    }

    @Test
    void shouldReturnNullWhenArgumentsNull() {
        // Given, When
        String result = pluginHelper.getAttribute(null, "entityID", "attributeName", "SP");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenAttributesNotConfigured() throws SAML2MetaException {
        // Given
        String realm = "realm";
        String entityID = "entityID";
        String attributeName = SAML2Constants.USE_NAMEID_AS_SP_USERID;
        Map<String, List<String>> attributes = Collections.emptyMap();

        SAML2MetaManager saml2MetaManager = mock(SAML2MetaManager.class);

        given(saml2MetaManager.getSPSSOConfig(realm, entityID)).willReturn(null);
        given(SAML2MetaUtils.getAttributes(any())).willReturn(attributes);

        // When
        String value = pluginHelper.getAttribute(realm,  entityID, attributeName, SP_ROLE);

        // Then
        assertThat(value).isNull();
    }

    @Test
    void shouldReturnConfiguredAttributeValue() throws SAML2MetaException {
        // Given
        String realm = "realm";
        String entityID = "entityID";
        String attributeName = SAML2Constants.USE_NAMEID_AS_SP_USERID;
        Map<String, List<String>> attributes = Map.of(attributeName, List.of("value"));

        SAML2MetaManager saml2MetaManager = mock(SAML2MetaManager.class);

        given(saml2MetaManager.getSPSSOConfig(realm, entityID)).willReturn(null);
        given(SAML2MetaUtils.getAttributes(any())).willReturn(attributes);

        // When
        String value = pluginHelper.getAttribute(realm,  entityID, attributeName, SP_ROLE);

        // Then
        assertThat(value).isEqualTo("value");
    }

    @Test
    void shouldGetAutoFedUser() throws SAML2Exception, DataStoreProviderException {
        // Given
        String realm = "realm";
        String entityID = "entityID";
        Assertion assertion = mock(Assertion.class);
        String decryptedNameID = "decryptedNameID";
        Set<PrivateKey> decryptionKeys = Set.of(mock(PrivateKey.class));

        Map<String, List<String>> attributes = Map.of(SAML2Constants.AUTO_FED_ENABLED, List.of("true"),
                SAML2Constants.AUTO_FED_ATTRIBUTE, List.of("mail"), SAML2Constants.USE_NAMEID_AS_SP_USERID,
                List.of("true"));

        SAML2MetaManager saml2MetaManager = mock(SAML2MetaManager.class);
        given(saml2MetaManager.getSPSSOConfig(realm, entityID)).willReturn(null);
        given(SAML2MetaUtils.getAttributes(any())).willReturn(attributes);

        DataStoreProvider dsProvider = mock(DataStoreProvider.class);
        given(SAML2Utils.getDataStoreProvider()).willReturn(dsProvider);
        given(dsProvider.isUsernameUniversalId(any())).willReturn(false);
        given(dsProvider.getUserID(any(), any())).willReturn("userId");

        // When
        String user = pluginHelper.getAutoFedUser(realm,  entityID, assertion, decryptedNameID, decryptionKeys,
                SP_ROLE);

        // Then
        assertThat(user).isEqualTo("userId");
    }

    @Test
    void shouldThrowExceptionWhenDataStoreProviderInitialisationFails() throws SAML2Exception {
        // Given
        given(SAML2Utils.getDataStoreProvider()).willThrow(new SAML2Exception("initialization error"));

        final Throwable thrown = Assertions.assertThrows(SAML2Exception.class, () -> {
            // When, Then
            pluginHelper.validateUserId("userId");
        });
        assertThat(thrown.getMessage()).matches("Could not initialize DataStoreProvider");
    }

    @Test
    void shouldThrowExceptionWhenUserValidationFails() throws SAML2Exception, DataStoreProviderException {
        // Given
        DataStoreProvider dsProvider = mock(DataStoreProvider.class);
        given(SAML2Utils.getDataStoreProvider()).willReturn(dsProvider);
        given(dsProvider.isUsernameUniversalId(any())).willReturn(true);

        final Throwable thrown = Assertions.assertThrows(SAML2Exception.class, () -> {
            // When, Then
            pluginHelper.validateUserId("userId");
        });
        assertThat(thrown.getMessage()).matches("Invalid user");
    }

    @Test
    void shouldValidateUserId() throws SAML2Exception, DataStoreProviderException {
        // Given
        DataStoreProvider dsProvider = mock(DataStoreProvider.class);
        given(SAML2Utils.getDataStoreProvider()).willReturn(dsProvider);
        given(dsProvider.isUsernameUniversalId("userId")).willReturn(false);

        // When, Then
        pluginHelper.validateUserId("userId");
    }
}