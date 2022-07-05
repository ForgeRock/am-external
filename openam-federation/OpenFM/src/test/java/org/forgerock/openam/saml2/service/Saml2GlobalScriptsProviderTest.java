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
 * Copyright 2021 ForgeRock AS.
 */
package org.forgerock.openam.saml2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ADAPTER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT;

import java.util.List;
import java.util.stream.Collectors;

import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.Script;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link Saml2GlobalScriptsProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class Saml2GlobalScriptsProviderTest {

    @InjectMocks
    private Saml2GlobalScriptsProvider provider;

    @Test
    public void shouldReturnSaml2GlobalScripts() throws ScriptException {
        // When
        List<String> globalScriptIds = provider.get().stream().map(Script::getId).collect(Collectors.toList());

        // Then
        assertThat(globalScriptIds.size()).isEqualTo(2);
        assertThat(globalScriptIds).containsExactlyInAnyOrder(SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT.getId(),
                SAML2_IDP_ADAPTER_SCRIPT.getId());
    }
}