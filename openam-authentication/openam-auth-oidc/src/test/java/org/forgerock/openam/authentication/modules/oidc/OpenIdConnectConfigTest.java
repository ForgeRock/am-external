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
 * Copyright 2014-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.utils.CollectionUtils.asOrderedSet;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class OpenIdConnectConfigTest {
    private final String PRINCIPAL_MAPPER =
            "org.forgerock.openam.authentication.modules.oidc.DefaultPrincipalMapper";
    private Map<String, Set<String>> configState;

    @BeforeEach
    void initalize() {
        configState = new HashMap<String, Set<String>>();
        configState.put(OpenIdConnectConfig.HEADER_NAME_KEY, asSet("header_name"));
        configState.put(OpenIdConnectConfig.ISSUER_NAME_KEY, asSet("accounts.google.com"));
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY,
                asSet(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CONFIG_URL));
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY,
                asSet("https://accounts.google.com/.well-known/openid-configuration"));
        configState.put(OpenIdConnectConfig.PRINCIPAL_MAPPER_CLASS_KEY, asSet(PRINCIPAL_MAPPER));
        configState.put(OpenIdConnectConfig.JWK_TO_LOCAL_ATTRIBUTE_MAPPINGS_KEY, asOrderedSet("id=sub"));
    }

    @Test
    void testAttributeConversion() {
        OpenIdConnectConfig config = new OpenIdConnectConfig(configState);
        assertThat(config.getJwkToLocalAttributeMappings().get("id")).isEqualTo("sub");
    }

    @Test
    void testDuplicateMappingEntries() {
        configState.get(OpenIdConnectConfig.JWK_TO_LOCAL_ATTRIBUTE_MAPPINGS_KEY).add("id=bobo");
        OpenIdConnectConfig config = new OpenIdConnectConfig(configState);
        assertThat(config.getJwkToLocalAttributeMappings()).isNotNull().hasSize(1);
        assertThat(config.getJwkToLocalAttributeMappings().get("id")).isEqualTo("sub");
    }

    @Test
    void testBasicAttributeLookup() {
        OpenIdConnectConfig config = new OpenIdConnectConfig(configState);
        assertThat(config.getPrincipalMapperClass()).isEqualTo(PRINCIPAL_MAPPER);
    }

    @Test
    void testInvalidCryptoContext() {
        configState.remove(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY);
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY, asSet("bogus_type"));
        assertThatThrownBy(() -> new OpenIdConnectConfig(configState))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testInvalidUrl() {
        configState.remove(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY);
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY, asSet("bogus_url"));
        assertThatThrownBy(() -> new OpenIdConnectConfig(configState))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testInvalidCryptoContextNullValue() {
        configState.remove(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY);
        assertThatThrownBy(() -> new OpenIdConnectConfig(configState))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testCryptoContextClientSecretIgnoresContextValue() {
        configState.remove(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY);
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY, asSet(
                OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_CLIENT_SECRET));
        configState.remove(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY);
        new OpenIdConnectConfig(configState);
    }
}

