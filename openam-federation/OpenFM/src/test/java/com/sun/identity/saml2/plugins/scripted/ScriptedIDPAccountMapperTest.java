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
 * Copyright 2024-2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.plugins.scripted;

import static com.sun.identity.saml2.common.SAML2Constants.NAMEID_MAPPER_SCRIPT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.HOSTED_ENTITYID;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.NAMEID_FORMAT;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.NAMEID_SCRIPT_HELPER;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.REMOTE_ENTITY;
import static com.sun.identity.saml2.common.SAML2Constants.ScriptParams.SESSION;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.saml2.service.Saml2NameIdMapperContext;
import org.forgerock.openam.scripting.api.ScriptedSession;
import org.forgerock.openam.scripting.api.identity.ScriptedIdentityRepository;
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
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.ValidationHelper;
import com.sun.identity.saml2.plugins.scripted.bindings.SamlNameIdMapperBindings;
import com.sun.identity.saml2.protocol.ManageNameIDRequest;

@ExtendWith(MockitoExtension.class)
public class ScriptedIDPAccountMapperTest {

    private static final String hostedEntityID = "hostedEntity";
    private static final String remoteEntityID = "remoteEntity";
    private static final String realm = "realm";
    private static final String nameIDFormat = "nameIdFormat";
    private static final UUID scriptId = UUID.randomUUID();
    private static final String exampleScript = "return myNameIDValue";
    private static final ValidationHelper validationHelper = new ValidationHelper();
    private MockedConstruction<ScriptedSession> mockedScriptedSession;
    private MockedConstruction<AMIdentity> mockedIdentity;

    @Mock
    private ScriptEvaluatorFactory scriptEvaluatorFactory;
    @Mock
    private ScriptEvaluator<SamlNameIdMapperBindings> scriptEvaluator;
    @Mock
    private ScriptStoreFactory scriptStoreFactory;
    @Mock
    private ScriptEvaluationHelper scriptEvaluationHelper;
    @Mock
    private ScriptStore scriptStore;
    @Mock
    private SSOToken session;
    @Mock
    private RealmLookup realmLookup;
    @Mock
    private ManageNameIDRequest manageNameIDRequest;
    @Mock
    private ScriptedIdentityRepository.Factory scriptedIdentityRepositoryFactory;
    @Mock
    private Realm realmObject;
    @Mock
    private Saml2NameIdMapperContext saml2NameIdMapperContext;

    private static MockedStatic<SAML2Utils> saml2Utils;

    // Class under test
    private ScriptedIDPAccountMapper scriptedIDPAccountMapper;

    @BeforeEach
    void setup() throws Exception {
        when(scriptEvaluatorFactory.create(saml2NameIdMapperContext)).thenReturn(scriptEvaluator);
        when(scriptEvaluator.getScriptEvaluationHelper()).thenReturn(scriptEvaluationHelper);
        scriptedIDPAccountMapper = new ScriptedIDPAccountMapper(scriptEvaluatorFactory, scriptStoreFactory,
                validationHelper, realmLookup, saml2NameIdMapperContext);
        saml2Utils = Mockito.mockStatic(SAML2Utils.class);
    }

    @AfterEach
    void tearDown() {
        saml2Utils.close();
    }

    @Test
    void shouldEvaluateNameIdMapperScript() throws Exception {
        ArgumentCaptor<SamlNameIdMapperBindings> bindingsCaptor = ArgumentCaptor.forClass(SamlNameIdMapperBindings.class);
        ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);
        // Given
        String scriptName = "Custom Name ID Value Script";
        when(realmLookup.lookup(realm)).thenReturn(realmObject);
        when(SAML2Utils.getAttributeValueFromSSOConfig(realm, remoteEntityID, SAML2Constants.SP_ROLE,
                SAML2Constants.NAMEID_MAPPER_SCRIPT)).thenReturn(scriptName);
        when(scriptStoreFactory.create(realmObject)).thenReturn(scriptStore);
        when(scriptStore.get(scriptName)).thenReturn(scriptConfiguration(scriptId.toString()));
        when(scriptEvaluationHelper.evaluateScript(any(), any(), any(), any())).thenReturn(Optional.of(""));
        mockedScriptedSession = Mockito.mockConstruction(ScriptedSession.class);
        mockedIdentity = Mockito.mockConstruction(AMIdentity.class);
        // When
        scriptedIDPAccountMapper.getNameID(session, hostedEntityID, remoteEntityID, realm, nameIDFormat);
        // Then
        verify(scriptEvaluationHelper, times(1)).evaluateScript(scriptCaptor.capture(),
                bindingsCaptor.capture(), eq(String.class), eq(realmObject));
        final Script script = scriptCaptor.getValue();
        assertThat(script.getScript()).isEqualTo(exampleScript);
        assertThat(script.getName()).isEqualTo(NAMEID_MAPPER_SCRIPT);
        assertThatThrownBy(() -> bindingsCaptor.getValue().legacyBindings())
                .isInstanceOf(UnsupportedOperationException.class);
        BindingsMap nextGenBindings = bindingsCaptor.getValue().nextGenBindings();
        assertThat(nextGenBindings).contains(entry(SESSION, mockedScriptedSession.constructed().get(0)),
                        entry(HOSTED_ENTITYID, hostedEntityID),
                        entry(REMOTE_ENTITY, remoteEntityID),
                        entry(NAMEID_FORMAT, nameIDFormat))
                .containsKey(NAMEID_SCRIPT_HELPER);
    }

    @Test
    void shouldThrowExceptionWhenRealmIsNull() {
        // When Then
        assertThatThrownBy(() -> scriptedIDPAccountMapper.getNameID(session, hostedEntityID, remoteEntityID, null,
                nameIDFormat))
                .isInstanceOf(SAML2Exception.class)
                .hasMessage("Null realm.");
    }

    @Test
    void shouldThrowExceptionWhenRemoteEntityIsNull() {
        // When Then
        assertThatThrownBy(() -> scriptedIDPAccountMapper.getNameID(session, hostedEntityID, null, realm,
                nameIDFormat))
                .isInstanceOf(SAML2Exception.class)
                .hasMessage("Null remote entityid.");
    }

    @Test
    void getIdentity() {
        assertNotImplementedException();
    }

    @Test
    void testGetIdentity() {
        assertNotImplementedException();
    }

    @Test
    void shouldPersistNameIDFormat() {
        assertNotImplementedException();
    }

    private Script scriptConfiguration(String id)
            throws Exception {

        return Script.builder()
                .setId(id)
                .setName(NAMEID_MAPPER_SCRIPT)
                .setCreationDate(10)
                .setDescription("SAML2 NameID Mapper Script")
                .setScript(exampleScript)
                .setLanguage(ScriptingLanguage.JAVASCRIPT)
                .setContext(saml2NameIdMapperContext).build();
    }

    private void assertNotImplementedException() {
        assertThatThrownBy(() -> scriptedIDPAccountMapper.getIdentity(manageNameIDRequest, hostedEntityID, realm))
                .isInstanceOf(NotImplementedException.class)
                .hasMessageContaining("Code is not implemented");
    }
}
