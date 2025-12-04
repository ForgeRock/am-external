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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.forgerock.openam.session.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.plugins.IDPAccountMapper;

@ExtendWith(MockitoExtension.class)
public class NameIDScriptHelperTest {

    @Mock
    private Session session;
    @Mock
    private IDPAccountMapper idpAccountMapper;
    @Mock
    private NameID nameID;

    private NameIDScriptHelper nameIDScriptHelper; // class under test

    private static final String HOSTED_ENTITY_ID = "hostedEntity";
    private static final String REMOTE_ENTITY_ID = "remoteEntity";
    private static final String REALM = "aRealm";
    private static final String NAME_ID_FORMAT = "someFormat";

    private static MockedStatic<SAML2Utils> saml2Utils;
    private static MockedStatic<MonitorManager> monitorManager;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this).close();
        monitorManager = mockStatic(MonitorManager.class);
        saml2Utils = mockStatic(SAML2Utils.class);

        saml2Utils.when(() -> SAML2Utils.getIDPAccountMapper(REALM, HOSTED_ENTITY_ID)).thenReturn(idpAccountMapper);

        nameIDScriptHelper = new NameIDScriptHelper(session, HOSTED_ENTITY_ID, REMOTE_ENTITY_ID, REALM, NAME_ID_FORMAT);
    }

    @AfterEach
    void tearDown() {
        saml2Utils.close();
        monitorManager.close();
    }

    @Test
    void testCreateNameIdentifier() {
        // given
        saml2Utils.when(SAML2Utils::createNameIdentifier).thenReturn("NameIdentifier");
        // when
        String nameIdentifier = nameIDScriptHelper.createNameIdentifier();
        // then
        assertThat(nameIdentifier).isEqualTo("NameIdentifier");
    }

    @Test
    void willDelegateToJavaPluginForNameID() throws Exception {
        // given
        when(idpAccountMapper.getNameID(session, HOSTED_ENTITY_ID, REMOTE_ENTITY_ID, REALM, NAME_ID_FORMAT))
                .thenReturn(nameID);
        // when
        nameIDScriptHelper.getNameIDValue();
        // then
        verify(idpAccountMapper, times(1)).getNameID(session, HOSTED_ENTITY_ID,
                REMOTE_ENTITY_ID, REALM, NAME_ID_FORMAT);
    }

    @Test
    void willDelegateToJavaPluginForPersist() {
        // when
        nameIDScriptHelper.shouldPersistNameIDFormat();
        // then
        verify(idpAccountMapper, times(1)).shouldPersistNameIDFormat(REALM, HOSTED_ENTITY_ID,
                REMOTE_ENTITY_ID, NAME_ID_FORMAT);
    }
}
