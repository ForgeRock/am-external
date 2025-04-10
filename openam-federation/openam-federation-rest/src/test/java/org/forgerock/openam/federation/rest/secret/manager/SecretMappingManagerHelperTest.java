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
 * Copyright 2020-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.federation.rest.secret.manager;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SecretMappingManagerHelperTest {

    private SecretMappingManagerHelper helper;

    private static final String SIGNING_MAPPING = "am.applications.federation.entity.providers.saml2.%s.signing";
    private static final String BASIC_AUTH_MAPPING = "am.applications.federation.entity.providers.saml2.%s.basicauth";

    private static Object[] similarSecretIdProvider() {
        return new Object[][] {
                { "asecretbutdifferent", singleton("asecret") },
                { "a.secret.but.different", singleton("secret") },
                { "a.secret.but.different", singleton("a.secret") }
        };
    }

    @BeforeEach
    void setup() {
        helper = new SecretMappingManagerHelper(null);
    }

    @Test
    void shouldReturnFalseWhenNonSaml2MappingId() {
        //given
        String mappingId = "non.saml2.mapping.id";

        //when
        boolean isUnusedHosted = helper.isUnusedHostedEntitySecretMapping(emptySet(), mappingId);
        boolean isUnusedRemote = helper.isUnusedRemoteEntitySecretMapping(emptySet(), mappingId);

        //then
        assertThat(isUnusedHosted).isFalse();
        assertThat(isUnusedRemote).isFalse();
    }

    @ParameterizedTest(name = "{index}: {0}")
    @ValueSource(strings = {
            "am.applications.federation.entity.providers.saml2.secret.signing",
            "am.applications.federation.entity.providers.saml2.secret.encryption"})
    public void shouldReturnUnusedWhenSaml2MappingIdAndNoHostedEntitiesAvailable(String mappingId) {
        //when
        boolean isUnused = helper.isUnusedHostedEntitySecretMapping(emptySet(), mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    void shouldReturnUnusedWhenSaml2MappingIdAndNoRemoteEntitiesAvailable() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.secret.basicauth";

        //when
        boolean isUnused = helper.isUnusedRemoteEntitySecretMapping(emptySet(), mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    void shouldReturnUnusedWhenSaml2MappingIdAndNoHostedIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.old.secret.signing";
        Set<String> identifiers = singleton("new.secret");

        //when
        boolean isUnused = helper.isUnusedHostedEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    void shouldReturnUnusedWhenSaml2MappingIdAndNoRemoteIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.old.secret.basicauth";
        Set<String> identifiers = singleton("new.secret");

        //when
        boolean isUnused = helper.isUnusedRemoteEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    void shouldReturnInUseWhenSaml2MappingIdAndHostedIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.secret.still.in.use.signing";
        Set<String> identifiers = singleton("secret.still.in.use");

        //when
        boolean isUnused = helper.isUnusedHostedEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isFalse();
    }

    @Test
    void shouldReturnInUseWhenSaml2MappingIdAndRemoteIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.secret.still.in.use.signing";
        Set<String> identifiers = singleton("secret.still.in.use");

        //when
        boolean isUnused = helper.isUnusedHostedEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isFalse();
    }

    @ParameterizedTest
    @MethodSource("similarSecretIdProvider")
    public void shouldReturnRemoteMappingNotInUseWhenSimilarSecretIdExists(String mappingIdSecret,
            Set<String> identifiers) {
        // given
        String mappingId = String.format(BASIC_AUTH_MAPPING, mappingIdSecret);

        //when
        boolean isUnused = helper.isUnusedRemoteEntitySecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @ParameterizedTest
    @MethodSource("similarSecretIdProvider")
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
