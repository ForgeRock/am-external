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

package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ATTRIBUTE_MAPPER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REMOTE_ENTITY;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ATTRIBUTE_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.forgerock.guice.core.GuiceExtension;
import org.forgerock.http.Client;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.BindingsMap;
import org.forgerock.openam.scripting.domain.LegacyScriptBindings;
import org.forgerock.openam.scripting.domain.LegacyScriptContext;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.ScriptStore;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;
import org.forgerock.openam.session.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.monitoring.impl.AgentProvider;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SOAPCommunicator;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.profile.IDPSSOUtil;

@ExtendWith(MockitoExtension.class)
public class ScriptedIdpAttributeMapperTest {

    @RegisterExtension
    GuiceExtension guiceExtension = new GuiceExtension.Builder()
            .addInstanceBinding(SOAPCommunicator.class, mock(SOAPCommunicator.class)).build();

    @Mock
    private ScriptEvaluatorFactory scriptEvaluatorFactory;
    @Mock
    private ScriptEvaluator scriptEvaluator;
    @Mock
    private ScriptStoreFactory scriptStoreFactory;
    @Mock
    ScriptEvaluationHelper scriptEvaluationHelper;
    @Mock
    ValidationHelper validationHelper;
    @Mock
    private ScriptStore scriptStore;
    @Mock
    Session session;
    @Mock
    RealmLookup realmLookup;
    @Mock
    private AgentProvider agentProvider;
    @Mock
    private Client httpClient;

    private final UUID scriptId = UUID.randomUUID();
    private final String exampleScript = "exampleScript";

    private static MockedStatic<MonitorManager> monitorManagerMockedStatic;
    private static MockedStatic<IDPSSOUtil> idpssoUtilMockedStatic;
    // Class under test
    private ScriptedIdpAttributeMapper scriptedIdpAttributeMapper;

    @BeforeEach
    void setup() throws Exception {
        when(scriptEvaluatorFactory.create(any(LegacyScriptContext.class))).thenReturn(scriptEvaluator);
        when(scriptEvaluator.getScriptEvaluationHelper()).thenReturn(scriptEvaluationHelper);
        scriptedIdpAttributeMapper = new ScriptedIdpAttributeMapper(scriptEvaluatorFactory, scriptStoreFactory,
                validationHelper, realmLookup, httpClient);
        monitorManagerMockedStatic = mockStatic(MonitorManager.class);
        idpssoUtilMockedStatic = mockStatic(IDPSSOUtil.class);
    }

    @AfterEach
    void tearDown() {
        monitorManagerMockedStatic.close();
        idpssoUtilMockedStatic.close();
    }

    @Test
    void shouldEvaluateIdpAttributeMapperScript() throws Exception {
        String hostedEntityId = "myIdp";
        String remoteEntityId = "mySp";
        String realm = "myRealm";
        Realm realmObject = mock(Realm.class);
        String scriptContent = "myScript";
        when(realmLookup.lookup(realm)).thenReturn(realmObject);

        ArgumentCaptor<LegacyScriptBindings> bindingsCaptor = ArgumentCaptor.forClass(LegacyScriptBindings.class);
        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);

        // Given
        when(IDPSSOUtil.getAttributeValueFromIDPSSOConfig(realm, hostedEntityId,
                SAML2Constants.IDP_ATTRIBUTE_MAPPER_SCRIPT)).thenReturn(scriptContent);
        when(scriptStoreFactory.create(realmObject)).thenReturn(scriptStore);
        when(scriptStore.get(scriptContent)).thenReturn(scriptConfiguration(scriptId.toString()));
        when(MonitorManager.getAgent()).thenReturn(agentProvider);

        // When
        scriptedIdpAttributeMapper.getAttributes(session, hostedEntityId, remoteEntityId, realm);

        // Then
        verify(scriptEvaluationHelper, times(1)).evaluateScript(scriptCaptor.capture(),
                bindingsCaptor.capture(), eq(List.class), eq(realmObject));
        final Script script = scriptCaptor.getValue();
        assertThat(script.getScript()).isEqualTo(exampleScript);
        assertThat(script.getName()).isEqualTo(IDP_ATTRIBUTE_MAPPER_SCRIPT);
        BindingsMap bindings = bindingsCaptor.getValue().legacyBindings();
        assertThat(bindings).contains(entry(SESSION, session),
                        entry(HOSTED_ENTITYID, hostedEntityId),
                        entry(REMOTE_ENTITY, remoteEntityId))
                .containsKey(IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER);
    }

    private Script scriptConfiguration(String id)
            throws Exception {

        return Script.builder()
                .setId(id)
                .setName(IDP_ATTRIBUTE_MAPPER_SCRIPT)
                .setCreationDate(10)
                .setDescription("SAML2 IDP Attribute Mapper Script")
                .setScript(exampleScript)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setContext(SAML2_IDP_ATTRIBUTE_MAPPER).build();
    }
}
