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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.service;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ADAPTER_SCRIPT;
import static com.sun.identity.shared.Constants.EMPTY_DROPDOWN_SELECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ADAPTER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2GlobalScript.SAML2_SP_ADAPTER_SCRIPT;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ADAPTER;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ATTRIBUTE_MAPPER;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_SP_ADAPTER;
import static org.forgerock.openam.scripting.domain.ScriptingLanguage.JAVASCRIPT;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;

/**
 * Tests for {@link Saml2ScriptContextProvider}.
 */
@ExtendWith(MockitoExtension.class)
public class Saml2ScriptContextProviderTest {

    private static final String ADAPTER_SCRIPT_IN_USE = "adapterScriptInUse";

    private static final String ADAPTER_SCRIPT_NOT_IN_USE = "adapterScriptNotInUse";

    private static final String NO_SCRIPT_CONFIGURED = EMPTY_DROPDOWN_SELECTION;

    @Mock
    private Realm realm;

    @Mock
    private SAML2MetaManager saml2MetaManager;

    @Mock
    private EntityConfigElement entityConfigElement;

    @Mock
    private EntityConfigType entityConfigType;

    @Mock
    private IDPSSOConfigElement idpssoConfigElement;

    @InjectMocks
    private Saml2ScriptContextProvider provider;

    private MockedStatic<SAML2Utils> saml2UtilsMockedStatic;
    private MockedStatic<SAML2MetaUtils> saml2MetaUtilsMockedStatic;

    @BeforeEach
    void setup() throws Exception {
        saml2UtilsMockedStatic = mockStatic(SAML2Utils.class);
        saml2MetaUtilsMockedStatic = mockStatic(SAML2MetaUtils.class);
        when(SAML2Utils.getSAML2MetaManager()).thenReturn(saml2MetaManager);
    }

    @AfterEach
    void tearDown() {
        saml2UtilsMockedStatic.close();
        saml2MetaUtilsMockedStatic.close();
    }

    @Test
    void shouldReturnAllSaml2ScriptContexts() {
        assertThat(provider.get().size()).isEqualTo(4);
    }

    @Test
    void shouldReturnSaml2IdpAttributeMapperContext() {
        // When
        List<ScriptContextDetails> actual = provider.get();

        // Then
        Optional<ScriptContextDetails> context = getScriptContextDetails(actual, SAML2_IDP_ATTRIBUTE_MAPPER);
        assertThat(context.isPresent()).isTrue();
        assertThat(context.get().getDefaultScriptId()).isEqualTo(SAML2_IDP_ATTRIBUTE_MAPPER_SCRIPT.getId());
        assertThat(context.get().getWhiteList())
                .containsAll(Arrays.asList("java.lang.String", "com.sun.identity.saml2.common.SAML2Exception"));
    }

    @Test
    void shouldReturnSaml2IdpAdapterContext() {
        // When
        List<ScriptContextDetails> actual = provider.get();

        // Then
        Optional<ScriptContextDetails> context = getScriptContextDetails(actual, SAML2_IDP_ADAPTER);
        assertThat(context.isPresent()).isTrue();
        assertThat(context.get().getDefaultScriptId()).isEqualTo(SAML2_IDP_ADAPTER_SCRIPT.getId());
        assertThat(context.get().getWhiteList())
                .containsAll(Arrays.asList("java.lang.String", "com.sun.identity.saml2.plugins.scripted.IdpAdapterScriptHelper"));
    }

    @Test
    void shouldReturnSaml2SpAdapterContext() {
        // When
        List<ScriptContextDetails> actual = provider.get();

        // Then
        Optional<ScriptContextDetails> context = getScriptContextDetails(actual, SAML2_SP_ADAPTER);
        assertThat(context.isPresent()).isTrue();
        assertThat(context.get().getDefaultScriptId()).isEqualTo(SAML2_SP_ADAPTER_SCRIPT.getId());
        assertThat(context.get().getWhiteList())
                .containsAll(Arrays.asList("java.lang.String", "com.sun.identity.saml2.plugins.scripted.SpAdapterScriptHelper"));
    }

    @Test
    void shouldReturnUsageCountWhenGivenScriptInUse() throws Exception {
        //When
        Script givenScript = createScript(ADAPTER_SCRIPT_IN_USE, SAML2_IDP_ADAPTER);
        mockSamlEntityConfig(ADAPTER_SCRIPT_IN_USE);

        //Then
        assertThat(provider.getUsageCount(realm, givenScript)).isEqualTo(1);
    }

    @Test
    void shouldReturnNoUsageWhenGivenScriptNotInUse() throws Exception {
        //When
        Script givenScript = createScript(ADAPTER_SCRIPT_NOT_IN_USE, SAML2_IDP_ADAPTER);
        mockSamlEntityConfig(ADAPTER_SCRIPT_IN_USE);

        //Then
        assertThat(provider.getUsageCount(realm, givenScript)).isEqualTo(0);
    }

    @Test
    void shouldReturnNoUsageWhenNoScriptConfigured() throws Exception {
        //When
        Script givenScript = createScript(ADAPTER_SCRIPT_NOT_IN_USE, SAML2_IDP_ADAPTER);
        mockSamlEntityConfig(NO_SCRIPT_CONFIGURED);

        //Then
        assertThat(provider.getUsageCount(realm, givenScript)).isEqualTo(0);
    }

    @Test
    void shouldFailWhenUsageCantBeDetermined() throws Exception {
        //Given
        Script givenScript = createScript(ADAPTER_SCRIPT_NOT_IN_USE, SAML2_IDP_ADAPTER);
        when(saml2MetaManager.getAllHostedEntityConfigs(realm.asPath())).thenThrow(SAML2MetaException.class);

        //When-Then
        assertThatThrownBy(() ->
                provider.getUsageCount(realm, givenScript))
                .isInstanceOf(SamlPluginScriptUsageCountException.class)
                .hasMessage("Unable to check if the script name_adapterScriptNotInUse is used.");
    }

    private Optional<ScriptContextDetails> getScriptContextDetails(List<ScriptContextDetails> scriptContextDetails, Saml2ScriptContext match) {
        return scriptContextDetails.stream().filter(s -> s.name().equals(match.name())).findFirst();
    }

    private void mockSamlEntityConfig(String scriptId) throws SAML2MetaException {
        when(saml2MetaManager.getAllHostedEntityConfigs(realm.asPath())).thenReturn(List.of(entityConfigElement));
        when(saml2MetaManager.getAllRemoteEntityConfigs(realm.asPath())).thenReturn(Collections.singletonList(null));
        when(entityConfigElement.getValue()).thenReturn(entityConfigType);
        when(entityConfigType.getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig())
                .thenReturn(List.of(idpssoConfigElement));
        when(SAML2MetaUtils.getAttributes(idpssoConfigElement))
                .thenReturn(Map.of(IDP_ADAPTER_SCRIPT, List.of(scriptId)));
    }

    private Script createScript(String scriptId, ScriptContext scriptContext) throws ScriptException {
        return Script.builder()
                .setId(scriptId)
                .setScript("")
                .setLanguage(JAVASCRIPT)
                .setName("name_" + scriptId)
                .setContext(scriptContext)
                .build();
    }
}
