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
 * Copyright 2023-2026 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.SP_ADAPTER_ENV;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static java.util.Collections.emptyList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.saml2.plugins.InitializablePlugin.HOSTED_ENTITY_ID;
import static org.forgerock.openam.saml2.plugins.InitializablePlugin.REALM;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.sun.identity.saml2.common.SAML2Utils;


public class SpAdapterScriptHelperTest {

    private static final String REALM_VALUE = "myRealm";
    private static final String SP_ENTITY_ID_VALUE = "mySp";
    private static final List<String> SP_ADAPTER_ENV_LIST = List.of("key1=value1", "key2=value2", "key3=value3");
    private MockedStatic<SAML2Utils> saml2UtilsMockedStatic;

    private SpAdapterScriptHelper scriptHelper;

    @BeforeEach
    void setup() {
        saml2UtilsMockedStatic = mockStatic(SAML2Utils.class);
        scriptHelper = new SpAdapterScriptHelper();
    }

    @AfterEach
    void tearDown() {
        saml2UtilsMockedStatic.close();
    }

    @Test
    void shouldReturnEnvEntriesWhenSpAdapterEnvIsSet() {
        // When
        when(SAML2Utils.getAllAttributeValueFromSSOConfig(REALM_VALUE, SP_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_ENV))
                .thenReturn(SP_ADAPTER_ENV_LIST);

        // Then
        Map<String, String> spAdapterEnv = scriptHelper.getSpAdapterEnv(REALM_VALUE, SP_ENTITY_ID_VALUE);
        assertThat(spAdapterEnv).contains(entry("key1", "value1"),
                entry("key2", "value2"),
                entry("key3", "value3"),
                entry(REALM, REALM_VALUE),
                entry(HOSTED_ENTITY_ID, SP_ENTITY_ID_VALUE));
    }

    @Test
    void shouldReturnDefaultsOnlyWhenSpAdapterEnvIsNotSet() {
        // When
        when(SAML2Utils.getAllAttributeValueFromSSOConfig(REALM_VALUE, SP_ENTITY_ID_VALUE, SP_ROLE, SP_ADAPTER_ENV))
                .thenReturn(emptyList());

        // Then
        Map<String, String> spAdapterEnv = scriptHelper.getSpAdapterEnv(REALM_VALUE, SP_ENTITY_ID_VALUE);
        assertThat(spAdapterEnv).contains(entry(REALM, REALM_VALUE), entry(HOSTED_ENTITY_ID, SP_ENTITY_ID_VALUE));
    }
}
