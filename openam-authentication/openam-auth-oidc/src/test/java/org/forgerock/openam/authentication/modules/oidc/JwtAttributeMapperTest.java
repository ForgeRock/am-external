/*
 * Copyright 2014-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

public class JwtAttributeMapperTest {
    private static final String EMAIL = "email";
    private static final String AM_EMAIL = "mail";
    private static final String EMAIL_VALUE = "bobo@bobo.com";
    private static final String SUB = "sub";
    private static final String UID = "uid";
    private static final String ISS = "iss";
    private static final String SUBJECT_VALUE = "112403712094132422537";
    private static final String ISSUER = "accounts.google.com";
    private Map<String, Object> jwtMappings;
    private Map<String, String> attributeMappings;
    private JwtClaimsSet claimsSet;
    private JwtAttributeMapper defaultPrincipalMapper;

    @BeforeTest
    public void initialize() {
        jwtMappings = new HashMap<String, Object>();
        jwtMappings.put(SUB, SUBJECT_VALUE);
        jwtMappings.put(ISS, ISSUER);
        jwtMappings.put(EMAIL, EMAIL_VALUE);

        attributeMappings = new HashMap<String, String>();
        attributeMappings.put(SUB, UID);
        attributeMappings.put(EMAIL, AM_EMAIL);

        claimsSet = new JwtClaimsSet(jwtMappings);
        defaultPrincipalMapper = new JwtAttributeMapper("uid", "prefix-");
    }

    @Test
    public void testBasicJwtMapping() {
        final Map<String, Set<String>> attrs =
                defaultPrincipalMapper.getAttributes(attributeMappings, claimsSet);
        assertThat(attrs.get(UID).iterator().next()).isEqualTo("prefix-" + SUBJECT_VALUE);
        assertThat(attrs.get(AM_EMAIL).iterator().next()).isEqualTo(EMAIL_VALUE);
    }
}
