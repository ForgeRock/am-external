/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.utils.CollectionUtils.*;


public class OpenIdConnectConfigTest {
    private Map<String, Set<String>> configState;
    private static final String PRINCIPAL_MAPPER  =
            "org.forgerock.openam.authentication.modules.oidc.DefaultPrincipalMapper";

    @BeforeTest
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
    public void testAttributeConversion() {
        OpenIdConnectConfig config = new OpenIdConnectConfig(configState);
        assertThat(config.getJwkToLocalAttributeMappings().get("id")).isEqualTo("sub");
    }

    @Test
    public void testDuplicateMappingEntries() {
        configState.get(OpenIdConnectConfig.JWK_TO_LOCAL_ATTRIBUTE_MAPPINGS_KEY).add("id=bobo");
        OpenIdConnectConfig config = new OpenIdConnectConfig(configState);
        assertThat(config.getJwkToLocalAttributeMappings()).isNotNull().hasSize(1);
        assertThat(config.getJwkToLocalAttributeMappings().get("id")).isEqualTo("sub");
    }

    @Test
    public void testBasicAttributeLookup() {
        OpenIdConnectConfig config = new OpenIdConnectConfig(configState);
        assertThat(config.getPrincipalMapperClass()).isEqualTo(PRINCIPAL_MAPPER);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidCryptoContext() {
        configState.remove(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY);
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_TYPE_KEY, asSet("bogus_type"));
        new OpenIdConnectConfig(configState);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidUrl() {
        configState.remove(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY);
        configState.put(OpenIdConnectConfig.CRYPTO_CONTEXT_VALUE_KEY, asSet("bogus_url"));
        new OpenIdConnectConfig(configState);
    }
}

