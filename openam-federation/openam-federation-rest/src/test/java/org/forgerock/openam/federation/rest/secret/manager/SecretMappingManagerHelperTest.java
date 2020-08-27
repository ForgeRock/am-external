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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.federation.rest.secret.manager;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SecretMappingManagerHelperTest {

    private SecretMappingManagerHelper helper;

    @BeforeMethod
    public void setup() {
        initMocks(this);
        helper = new SecretMappingManagerHelper(null);
    }

    @Test
    public void shouldReturnFalseWhenNonSaml2MappingId() {
        //given
        String mappingId = "non.saml2.mapping.id";

        //when
        boolean isUnused = helper.isUnusedSecretMapping(emptySet(), mappingId);

        //then
        assertThat(isUnused).isFalse();
    }

    @Test
    public void shouldReturnUnusedWhenSaml2MappingIdAndNoEntitiesAvailable() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.secret.signing";

        //when
        boolean isUnused = helper.isUnusedSecretMapping(emptySet(), mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    public void shouldReturnUnusedWhenSaml2MappingIdAndNoIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.old.secret.signing";
        Set<String> identifiers = singleton("new.secret");

        //when
        boolean isUnused = helper.isUnusedSecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isTrue();
    }

    @Test
    public void shouldReturnInUseWhenSaml2MappingIdAndIdentifiersMatch() {
        //given
        String mappingId = "am.applications.federation.entity.providers.saml2.secret.still.in.use.signing";
        Set<String> identifiers = singleton("secret.still.in.use");

        //when
        boolean isUnused = helper.isUnusedSecretMapping(identifiers, mappingId);

        //then
        assertThat(isUnused).isFalse();
    }
}