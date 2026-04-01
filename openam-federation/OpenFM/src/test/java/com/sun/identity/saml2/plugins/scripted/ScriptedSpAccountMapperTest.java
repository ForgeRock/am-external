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
 * Copyright 2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.SP_ACCOUNT_MAPPER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.ASSERTION_WRAPPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SP_ACCOUNT_MAPPER_SCRIPT_HELPER;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;
import org.forgerock.http.Client;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.saml2.service.Saml2AccountMapperScriptContext;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.ScriptStore;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.impl.AssertionImpl;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlSpAccountMapperBindings;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;

@ExtendWith(MockitoExtension.class)
public class ScriptedSpAccountMapperTest {

    private static final String hostedEntityID = "hostedEntity";
    private static final String realm = "realm";
    private static final UUID scriptId = UUID.randomUUID();
    private static final String exampleScript = "return \"user-identity\"";
    private static final ValidationHelper validationHelper = new ValidationHelper();
    private static MockedStatic<SAML2Utils> saml2Utils;
    private final Assertion assertion = new AssertionImpl();
    @Mock
    private ScriptEvaluatorFactory scriptEvaluatorFactory;
    @Mock
    private ScriptEvaluator<SamlSpAccountMapperBindings> scriptEvaluator;
    @Mock
    private ScriptStoreFactory scriptStoreFactory;
    @Mock
    private ScriptEvaluationHelper scriptEvaluationHelper;
    @Mock
    private ScriptStore scriptStore;
    @Mock
    private ManageNameIDRequest manageNameIDRequest;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private Realm realmObject;
    @Mock
    private Saml2AccountMapperScriptContext saml2AccountMapperContext;
    @Mock
    private Client httpClient;
    // Class under test
    private ScriptedSpAccountMapper scriptedSpAccountMapper;

    @BeforeEach
    void setup() {
        given(scriptEvaluatorFactory.create(saml2AccountMapperContext)).willReturn(scriptEvaluator);
        given(scriptEvaluator.getScriptEvaluationHelper()).willReturn(scriptEvaluationHelper);
        scriptedSpAccountMapper = new ScriptedSpAccountMapper(scriptEvaluatorFactory, scriptStoreFactory,
                validationHelper, realmLookup, saml2AccountMapperContext, httpClient);
        saml2Utils = Mockito.mockStatic(SAML2Utils.class);
    }

    @AfterEach
    void tearDown() {
        saml2Utils.close();
    }

    @Test
    void shouldEvaluateAccountMapperScript() throws Exception {
        ArgumentCaptor<SamlSpAccountMapperBindings> bindingsCaptor = ArgumentCaptor
                .forClass(SamlSpAccountMapperBindings.class);
        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);

        // Given
        String scriptName = "Custom Account Mapper Script";
        given(realmLookup.lookup(realm)).willReturn(realmObject);
        given(SAML2Utils.getAttributeValueFromSSOConfig(realm, hostedEntityID, SAML2Constants.SP_ROLE,
                SAML2Constants.SP_ACCOUNT_MAPPER_SCRIPT)).willReturn(scriptName);
        given(scriptStoreFactory.create(realmObject)).willReturn(scriptStore);
        given(scriptStore.get(scriptName)).willReturn(scriptConfiguration(scriptId.toString()));
        given(scriptEvaluationHelper.evaluateScript(any(), any(), any(), any())).willReturn(Optional.of(""));

        // When
        scriptedSpAccountMapper.getIdentity(assertion, hostedEntityID, realm);

        // Then
        verify(scriptEvaluationHelper, times(1)).evaluateScript(scriptCaptor.capture(),
                bindingsCaptor.capture(), eq(String.class), eq(realmObject));
        final Script script = scriptCaptor.getValue();
        assertThat(script.getScript()).isEqualTo(exampleScript);
        assertThat(script.getName()).isEqualTo(SP_ACCOUNT_MAPPER_SCRIPT);
        assertThatThrownBy(() -> bindingsCaptor.getValue().legacyBindings())
                .isInstanceOf(UnsupportedOperationException.class);
        BindingsMap nextGenBindings = bindingsCaptor.getValue().nextGenBindings();
        assertThat(nextGenBindings).contains(
                        entry(HOSTED_ENTITYID, hostedEntityID))
                .containsKey(SP_ACCOUNT_MAPPER_SCRIPT_HELPER)
                .containsKey(ASSERTION_WRAPPER);
    }

    @Test
    void shouldThrowExceptionGivenRealmIsNull() {
        // When Then
        assertThatThrownBy(() -> scriptedSpAccountMapper.getIdentity(assertion, hostedEntityID, null))
                .isInstanceOf(SAML2Exception.class)
                .hasMessage("Null realm.");
    }

    @Test
    void shouldThrowExceptionGivenHostedEntityIsNull() {
        // When Then
        assertThatThrownBy(() -> scriptedSpAccountMapper.getIdentity(assertion, null, realm))
                .isInstanceOf(SAML2Exception.class)
                .hasMessage("Null host entityid.");
    }

    @Test
    void shouldThrowNotImplementedExceptionForManageNameIDRequest() {
        assertThatThrownBy(() -> scriptedSpAccountMapper.getIdentity(manageNameIDRequest, hostedEntityID, realm))
                .isInstanceOf(NotImplementedException.class)
                .hasMessageContaining("Code is not implemented");
    }

    private Script scriptConfiguration(String id)
            throws Exception {

        return Script.builder()
                .setId(id)
                .setName(SP_ACCOUNT_MAPPER_SCRIPT)
                .setCreationDate(10)
                .setDescription("SAML2 Account Mapper Script")
                .setScript(exampleScript)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setContext(saml2AccountMapperContext).build();
    }
}
