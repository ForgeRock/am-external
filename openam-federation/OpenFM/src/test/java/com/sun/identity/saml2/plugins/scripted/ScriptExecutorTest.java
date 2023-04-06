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
 * Copyright 2023 ForgeRock AS.
 */
package com.sun.identity.saml2.plugins.scripted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.ScriptStore;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.identity.saml2.common.SAML2Utils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SAML2Utils.class, ScriptExecutor.class})
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
    private ScriptEvaluationHelper scriptEvaluationHelper;
    @Mock
    private ScriptEvaluator scriptEvaluator;
    @Mock
    private Script script;

    // Class under test
    private ScriptExecutor executor;

    @Before
    public void setup() throws Exception {
        executor = new ScriptExecutor(scriptStoreFactory, realmLookup, scriptEvaluationHelper);
        PowerMockito.mockStatic(SAML2Utils.class);
        Realm mockRealm = mock(Realm.class);
        // Given
        when(realmLookup.lookup(realm)).thenReturn(mockRealm);
        when(SAML2Utils.getAttributeValueFromSSOConfig(realm, hostedEntityId, role, scriptAttribute))
                .thenReturn(scriptAttributeValue);
        when(scriptStoreFactory.create(mockRealm)).thenReturn(scriptStore);
        when(scriptStore.get(scriptAttributeValue)).thenReturn(scriptConfiguration(scriptId.toString()));
    }

    @Test
    public void testEvaluateVoidScriptFunction() throws Exception {
        Bindings bindings = new SimpleBindings();
        executor.evaluateVoidScriptFunction(scriptEvaluator, script, realm, bindings, function);

        verifyScriptEvaluation(bindings, null);
    }

    @Test
    public void testEvaluateScriptFunction() throws Exception {
        Bindings bindings = new SimpleBindings();
        executor.evaluateScriptFunction(scriptEvaluator, script, realm, bindings, function);

        verifyScriptEvaluation(bindings, Boolean.class);
    }

    @Test
    public void testGetScriptConfiguration() throws Exception {
        Script script = executor.getScriptConfiguration(realm, hostedEntityId, role, scriptAttribute);
        assertThat(script.getScript()).isEqualTo(exampleScript);
        assertThat(script.getName()).isEqualTo(scriptName);
    }

    private void verifyScriptEvaluation(Bindings bindings, Class returnType) throws Exception {
        if (returnType == null) {
            verify(scriptEvaluationHelper, times(1)).evaluateFunction(eq(scriptEvaluator), eq(script), eq(bindings),
                    eq(function), any(Realm.class));
        } else {
            verify(scriptEvaluationHelper, times(1)).evaluateFunction(eq(scriptEvaluator), eq(script), eq(bindings),
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
