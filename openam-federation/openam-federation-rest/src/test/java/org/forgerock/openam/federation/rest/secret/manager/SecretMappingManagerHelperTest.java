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
 * Copyright 2020-2024 ForgeRock AS.
 */

package org.forgerock.openam.federation.rest.secret.manager;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;

@RunWith(JUnitParamsRunner.class)
public class SecretMappingManagerHelperTest {

    private SecretMappingManagerHelper helper;

    private static final String SIGNING_MAPPING = "am.applications.federation.entity.providers.saml2.%s.signing";
    private static final String BASIC_AUTH_MAPPING = "am.applications.federation.entity.providers.saml2.%s.basicauth";

    private Object[] similarSecretIdProvider() {
        return new Object[][] {
                { "asecretbutdifferent", singleton("asecret") },
                { "a.secret.but.different", singleton("secret") },
                { "a.secret.but.different", singleton("a.secret") }
        };
    }

    @Before
    public void setup() {
        openMocks(this);
        helper = new SecretMappingManagerHelper(null);
    }

    @Test
    public void shouldReturnFalseWhenNonSaml2MappingId() {
        //given
        String mappingId = "non.saml2.mapping.id";

        //when
        boolean isUnusedHosted = helper.isUnusedHostedEntitySecretMapping(emptySet(), mappingId);
        boolean isUnusedRemote = helper.isUnusedRemoteEntitySecretMapping(emptySet(), mappingId);

        //then
        assertThat(isUnusedHosted).isFalse();
        assertThat(isUnusedRemote).isFalse();
    }

    @Test
    @TestCaseName("{method}: {0}")
    @Parameters({
            "am.applications.federation.entity.providers.saml2.secret.signing",
            "am.applications.federation.entity.providers.saml2.secret.encryption"})
    public void shouldReturnUnusedWhenSaml2MappingIdAndNoHostedEntitiesAvailable(String mappingId) {
        //when
        boolean isUnused = helper.isUnusedHostedEntitySecretMapping(emptySet(), mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    public void shouldReturnUnusedWhenSaml2MappingIdAndNoRemoteEntitiesAvailable() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.secret.basicauth";

        //when
        boolean isUnused = helper.isUnusedRemoteEntitySecretMapping(emptySet(), mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    public void shouldReturnUnusedWhenSaml2MappingIdAndNoHostedIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.old.secret.signing";
        Set<String> identifiers = singleton("new.secret");

        //when
        boolean isUnused = helper.isUnusedHostedEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    public void shouldReturnUnusedWhenSaml2MappingIdAndNoRemoteIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.old.secret.basicauth";
        Set<String> identifiers = singleton("new.secret");

        //when
        boolean isUnused = helper.isUnusedRemoteEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    public void shouldReturnInUseWhenSaml2MappingIdAndHostedIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.secret.still.in.use.signing";
        Set<String> identifiers = singleton("secret.still.in.use");

        //when
        boolean isUnused = helper.isUnusedHostedEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isFalse();
    }

    @Test
    public void shouldReturnInUseWhenSaml2MappingIdAndRemoteIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.secret.still.in.use.signing";
        Set<String> identifiers = singleton("secret.still.in.use");

        //when
        boolean isUnused = helper.isUnusedHostedEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isFalse();
    }

    @Test
    @Parameters(method = "similarSecretIdProvider")
    public void shouldReturnRemoteMappingNotInUseWhenSimilarSecretIdExists(String mappingIdSecret,
            Set<String> identifiers) {
        // given
        String mappingId = String.format(BASIC_AUTH_MAPPING, mappingIdSecret);

        //when
        boolean isUnused = helper.isUnusedRemoteEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    @Parameters(method = "similarSecretIdProvider")
    public void shouldReturnHostedMappingNotInUseWhenSimilarSecretIdExists(String mappingIdSecret,
            Set<String> identifiers) {
        // given
        String mappingId = String.format(SIGNING_MAPPING, mappingIdSecret);

        //when
        boolean isUnused = helper.isUnusedHostedEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isTrue();
    }
}