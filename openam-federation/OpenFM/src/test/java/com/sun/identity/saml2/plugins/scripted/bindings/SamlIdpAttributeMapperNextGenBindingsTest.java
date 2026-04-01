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
 * Copyright 2026 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted.bindings;

import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REALM;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REMOTE_ENTITY;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.forgerock.openam.scripting.api.ScriptedSession;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.saml2.plugins.scripted.IdpAttributeMapperScriptNextGenHelper;

/**
 * Unit tests for {@link SamlIdpAttributeMapperNextGenBindings}.
 */
class SamlIdpAttributeMapperNextGenBindingsTest {

    private static final String TEST_REALM = "/testRealm";
    private static final String TEST_REMOTE_ENTITY_ID = "http://sp.example.com";
    private static final String TEST_HOSTED_ENTITY_ID = "http://idp.example.com";

    private SSOToken ssoToken;

    @BeforeEach
    void setUp() {
        ssoToken = mock(SSOToken.class);
    }

    @Test
    void shouldBuildBindingsWithAllParameters() {
        // When
        SamlIdpAttributeMapperNextGenBindings bindings = SamlIdpAttributeMapperNextGenBindings.builder()
                .withSsoToken(ssoToken)
                .withRealm(TEST_REALM)
                .withRemoteEntityId(TEST_REMOTE_ENTITY_ID)
                .withHostedEntityId(TEST_HOSTED_ENTITY_ID)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();

        assertThat(bindingsMap.get(REALM)).isEqualTo(TEST_REALM);
        assertThat(bindingsMap.get(REMOTE_ENTITY)).isEqualTo(TEST_REMOTE_ENTITY_ID);
        assertThat(bindingsMap.get(HOSTED_ENTITYID)).isEqualTo(TEST_HOSTED_ENTITY_ID);

        assertThat(bindingsMap.get(IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER)).isInstanceOf(
                IdpAttributeMapperScriptNextGenHelper.class);
        assertThat(bindingsMap.get(SESSION)).isInstanceOf(ScriptedSession.class);
    }

    @Test
    void shouldBuildBindingsWithMinimalParameters() {
        // When
        SamlIdpAttributeMapperNextGenBindings bindings = SamlIdpAttributeMapperNextGenBindings.builder().build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();

        assertThat(bindingsMap.get(REALM)).isNull();
        assertThat(bindingsMap.get(REMOTE_ENTITY)).isNull();
        assertThat(bindingsMap.get(HOSTED_ENTITYID)).isNull();
        assertThat(bindingsMap.get(SESSION)).isNull();

        // Helper should still be created, even with a null token
        assertThat(bindingsMap.get(IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER)).isInstanceOf(
                IdpAttributeMapperScriptNextGenHelper.class);
    }

    @Test
    void shouldHandleNullSsoToken() {
        // When
        SamlIdpAttributeMapperNextGenBindings bindings = SamlIdpAttributeMapperNextGenBindings.builder()
                .withSsoToken(null)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap.get(SESSION)).isNull();
        assertThat(bindingsMap.get(IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER)).isNotNull();
    }

    @Test
    void shouldIncludeAllBindingKeys() {
        // When
        SamlIdpAttributeMapperNextGenBindings bindings = SamlIdpAttributeMapperNextGenBindings.builder()
                .withSsoToken(ssoToken)
                .withRealm(TEST_REALM)
                .withRemoteEntityId(TEST_REMOTE_ENTITY_ID)
                .withHostedEntityId(TEST_HOSTED_ENTITY_ID)
                .build();

        // Then
        BindingsMap bindingsMap = bindings.nextGenBindings();
        assertThat(bindingsMap).containsKeys(
                IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER, SESSION, REALM, REMOTE_ENTITY, HOSTED_ENTITYID
        );
        assertThat(bindingsMap).hasSize(5);
    }
}
