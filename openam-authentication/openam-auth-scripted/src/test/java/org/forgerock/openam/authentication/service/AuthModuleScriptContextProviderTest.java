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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.authentication.service.AuthModuleGlobalScript.AUTH_MODULE_SERVER_SIDE;
import static org.forgerock.openam.authentication.service.AuthModuleScriptContext.AUTHENTICATION_CLIENT_SIDE;
import static org.forgerock.openam.authentication.service.AuthModuleScriptContext.AUTHENTICATION_SERVER_SIDE;
import static org.forgerock.openam.scripting.domain.Script.EMPTY_SCRIPT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptContext;
import org.forgerock.openam.scripting.domain.ScriptContextDetails;
import org.forgerock.openam.scripting.domain.ScriptException;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.iplanet.dpro.session.service.DsameAdminTokenProvider;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.ServiceManagementDAO;
import com.sun.identity.sm.SmsEntryUid;

/**
 * Tests for {@link AuthModuleScriptContextProvider}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthModuleScriptContextProviderTest {

    @Mock
    private DsameAdminTokenProvider dsameAdminTokenProvider;
    @Mock
    private ServiceManagementDAO serviceManagementDAO;
    @Mock
    private SSOToken adminToken;

    private Realm realm;
    private AuthModuleScriptContextProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        realm = mockRealm("myRealm");
        given(dsameAdminTokenProvider.getAdminToken()).willReturn(adminToken);
        given(serviceManagementDAO.search(eq(adminToken), any(), contains("=usedScript"), anyInt(), anyInt(), anyBoolean(), anyBoolean()))
                .willAnswer(invocation -> {
                    if (invocation.getArgument(1, SmsEntryUid.class).getUid().contains("o=myRealm")) {
                        return Set.of("entry1");
                    }
                    return new HashSet<>();
                });

        provider = new AuthModuleScriptContextProvider(dsameAdminTokenProvider, serviceManagementDAO);
    }

    @Test
    void shouldReturnAuthModuleScriptContexts() {
        // When
        List<ScriptContextDetails> actual = provider.get();

        // Then
        assertThat(actual.size()).isEqualTo(2);

        assertThat(actual.get(0).name()).isEqualTo(AUTHENTICATION_SERVER_SIDE.name());
        assertThat(actual.get(0).getDefaultScriptId()).isEqualTo(AUTH_MODULE_SERVER_SIDE.getId());
        assertThat(actual.get(0).getWhiteList())
                .containsAll(Arrays.asList("java.lang.String", "org.forgerock.openam.authentication.modules.scripted.*"));

        assertThat(actual.get(1).name()).isEqualTo(AUTHENTICATION_CLIENT_SIDE.name());
        assertThat(actual.get(1).getDefaultScriptId()).isEqualTo(EMPTY_SCRIPT.getId());
        assertThat(actual.get(1).getWhiteList()).isNull();
    }

    @Test
    void shouldReturnServerSideUsageCount() throws Exception {
        Script script = createScriptWithIdAndContext("usedScript", AUTHENTICATION_SERVER_SIDE);

        assertThat(provider.getUsageCount(realm, script)).isEqualTo(2); // two potential references
    }

    @Test
    void shouldReturnClientSideUsageCount() throws Exception {
        Script script = createScriptWithIdAndContext("usedScript", AUTHENTICATION_CLIENT_SIDE);

        assertThat(provider.getUsageCount(realm, script)).isEqualTo(2); // two potential references
    }

    @Test
    void shouldExceptForUnknownContext() throws Exception {
        Script script = createScriptWithIdAndContext("usedScript", EMPTY_SCRIPT.getContext());

        assertThatThrownBy(() -> provider.getUsageCount(realm, script))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Called getUsageCount on wrong provider");
    }

    @Test
    void shouldReturnNoUsagesForUnknownScript() throws Exception {
        Script script = createScriptWithIdAndContext("unknownScript", AUTHENTICATION_SERVER_SIDE);

        assertThat(provider.getUsageCount(realm, script)).isEqualTo(0);
    }

    @Test
    void shouldReturnNoUsagesForScriptInWrongRealm() throws Exception {
        Realm otherRealm = mockRealm("otherRealm");
        Script script = createScriptWithIdAndContext("usedScript", AUTHENTICATION_SERVER_SIDE);

        assertThat(provider.getUsageCount(otherRealm, script)).isEqualTo(0);
    }

    private Script createScriptWithIdAndContext(String scriptId, ScriptContext scriptContext)
            throws ScriptException {
        return Script.builder()
                .setId(scriptId)
                .setScript("")
                .setLanguage(ScriptingLanguage.GROOVY)
                .setName("name_" + scriptId)
                .setContext(scriptContext)
                .build();
    }

    private Realm mockRealm(String realmName) {
        Realm realm = mock(Realm.class);
        given(realm.asDN()).willReturn("o=" + realmName + ",ou=services,dc=openam,dc=example,dc=com");
        return realm;
    }
}
