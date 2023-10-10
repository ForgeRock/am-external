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
 * Copyright 2021-2023 ForgeRock AS.
 */

package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ATTRIBUTE_MAPPER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.IDP_ATTRIBUTE_MAPPER_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.LOGGER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REALM;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REMOTE_ENTITY;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.forgerock.openam.saml2.service.Saml2ScriptContext.SAML2_IDP_ATTRIBUTE_MAPPER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.List;
import java.util.UUID;

import javax.script.Bindings;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.scripting.application.ScriptEvaluationHelper;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.scripting.persistence.ScriptStore;
import org.forgerock.openam.scripting.persistence.ScriptStoreFactory;
import org.forgerock.openam.session.Session;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.plugin.monitoring.impl.AgentProvider;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.profile.IDPSSOUtil;
import com.sun.identity.shared.debug.Debug;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IDPSSOUtil.class, MonitorManager.class})
public class ScriptedIdpAttributeMapperTest {

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

    private final UUID scriptId = UUID.randomUUID();
    private final String exampleScript = "exampleScript";

    // Class under test
    private ScriptedIdpAttributeMapper scriptedIdpAttributeMapper;

    @Before
    public void setup() throws Exception {
        scriptedIdpAttributeMapper = new ScriptedIdpAttributeMapper(__ -> scriptEvaluator, scriptStoreFactory,
                scriptEvaluationHelper, validationHelper, realmLookup);
        PowerMockito.mockStatic(MonitorManager.class);
        mockStatic(IDPSSOUtil.class);
    }

    @Test
    public void shouldEvaluateIdpAttributeMapperScript() throws Exception {
        String hostedEntityId = "myIdp";
        String remoteEntityId = "mySp";
        String realm = "myRealm";
        Realm realmObject = mock(Realm.class);
        String scriptContent = "myScript";
        when(realmLookup.lookup(realm)).thenReturn(realmObject);

        ArgumentCaptor<Bindings> bindingsCaptor = ArgumentCaptor.forClass(Bindings.class);
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
        verify(scriptEvaluationHelper, times(1)).evaluateScript(eq(scriptEvaluator), scriptCaptor.capture(),
                bindingsCaptor.capture(), eq(List.class), eq(realmObject));
        final Script script = scriptCaptor.getValue();
        assertThat(script.getScript()).isEqualTo(exampleScript);
        assertThat(script.getName()).isEqualTo(IDP_ATTRIBUTE_MAPPER_SCRIPT);
        assertThat(bindingsCaptor.getValue()).contains(entry(SESSION, session),
                        entry(HOSTED_ENTITYID, hostedEntityId),
                        entry(REMOTE_ENTITY, remoteEntityId),
                        entry(REALM, realm),
                        entry(LOGGER, Debug.getInstance(String.format("scripts.%s.%s.(%s)",
                                SAML2_IDP_ATTRIBUTE_MAPPER.name(), script.getId(), script.getName()))))
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
