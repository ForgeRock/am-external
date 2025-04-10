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
 * Copyright 2023-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml2.plugins.scripted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.ScriptStore;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.sun.identity.saml2.common.SAML2Utils;

public class ScriptExecutorTest {
    private static final UUID scriptId = UUID.randomUUID();
    private static final String exampleScript = "exampleScript";
    private static final String hostedEntityId = "myIdp";
    private static final String realm = "myRealm";

    private static final String role = "myRole";
    private static final String scriptAttribute = "myScriptAttribute";
    private static final String scriptAttributeValue = "myScriptAttributeValue";
    private static final String scriptName = "myScriptName";
    private static final String function = "myFunction";
    @Mock
    private ScriptStoreFactory scriptStoreFactory;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private ScriptStore scriptStore;
    @Mock
    private ScriptEvaluationHelper<LegacyScriptBindings> scriptEvaluationHelper;
    @Mock
    private ScriptEvaluator<LegacyScriptBindings> scriptEvaluator;
    @Mock
    private Script script;

    private MockedStatic<SAML2Utils> saml2UtilsMockedStatic;

    // Class under test
    private ScriptExecutor executor;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        executor = new ScriptExecutor(scriptStoreFactory, realmLookup);
        saml2UtilsMockedStatic = mockStatic(SAML2Utils.class);
        Realm mockRealm = mock(Realm.class);
        // Given
        when(realmLookup.lookup(realm)).thenReturn(mockRealm);
        when(SAML2Utils.getAttributeValueFromSSOConfig(realm, hostedEntityId, role, scriptAttribute))
                .thenReturn(scriptAttributeValue);
        when(scriptStoreFactory.create(mockRealm)).thenReturn(scriptStore);
        when(scriptStore.get(scriptAttributeValue)).thenReturn(scriptConfiguration(scriptId.toString()));
        when(scriptEvaluator.getScriptEvaluationHelper()).thenReturn(scriptEvaluationHelper);
    }

    @AfterEach
    void tearDown() {
        saml2UtilsMockedStatic.close();
    }

    @Test
    void testEvaluateVoidScriptFunction() throws Exception {
        LegacyScriptBindings bindings = mock(LegacyScriptBindings.class);
        executor.evaluateVoidScriptFunction(scriptEvaluator, script, realm, bindings, function);

        verifyScriptEvaluation(bindings, null);
    }

    @Test
    void testEvaluateScriptFunction() throws Exception {
        LegacyScriptBindings bindings = mock(LegacyScriptBindings.class);
        executor.evaluateScriptFunction(scriptEvaluator, script, realm, bindings, function);

        verifyScriptEvaluation(bindings, Boolean.class);
    }

    @Test
    void testGetScriptConfiguration() throws Exception {
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, role, scriptAttribute);
        assertThat(script.getScript()).isEqualTo(exampleScript);
        assertThat(script.getName()).isEqualTo(scriptName);
    }

    private void verifyScriptEvaluation(LegacyScriptBindings bindings, Class returnType) throws Exception {
        if (returnType == null) {
            verify(scriptEvaluationHelper, times(1)).evaluateFunction(eq(script), eq(bindings),
                    eq(function), any(Realm.class));
        } else {
            verify(scriptEvaluationHelper, times(1)).evaluateFunction(eq(script), eq(bindings),
                    eq(returnType), eq(function), any(Realm.class));
        }
    }

    private Script scriptConfiguration(String id) throws Exception {
        return Script.builder()
                .setId(id)
                .setName(scriptName)
                .setCreationDate(10)
                .setDescription("Plugin Script")
                .setScript(exampleScript)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setContext(() -> "myContext").build();
    }
}
