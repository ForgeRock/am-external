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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.saml2.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.Realms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.plugin.configuration.ConfigurationActionEvent;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationManager;

@ExtendWith({MockitoExtension.class})
public class SAML2AuthnConfigTest {

    @Mock
    private ConfigurationInstance ci;
    @Mock
    private ConfigurationActionEvent configurationActionEvent;
    @Mock
    Realm realm;

    private MockedStatic<ConfigurationManager> configurationManagerMockedStatic;
    private MockedStatic<Realms> realmsMockedStatic;

    private SAML2AuthnConfig saml2AuthnConfig;

    @BeforeEach
    void setup() throws Exception {
        configurationManagerMockedStatic = mockStatic(ConfigurationManager.class);
        realmsMockedStatic = mockStatic(Realms.class);
        given(Realms.of(anyString())).willReturn(realm);

        given(ConfigurationManager.getConfigurationInstance(anyString())).willReturn(ci);
        saml2AuthnConfig = new SAML2AuthnConfig();
    }

    @AfterEach
    void tearDown() {
        configurationManagerMockedStatic.close();
        realmsMockedStatic.close();
    }

    @Test
    void shouldClearCacheGivenConfigChanged() throws Exception {
        Map<String, Set<String>> configAlpha = new HashMap<>();
        configAlpha.put("iplanet-am-auth-org-config", Set.of("tree1"));
        String alphaRealm = "/alpha";
        given(ci.getConfiguration(eq(alphaRealm), eq(null))).willReturn(configAlpha);
        String alphaService = saml2AuthnConfig.getDefaultServiceForRealm(alphaRealm);
        assertThat(saml2AuthnConfig.config.size()).isEqualTo(1);
        assertThat(alphaService).isEqualTo("tree1");

        Map<String, Set<String>> configBeta = new HashMap<>();
        configBeta.put("iplanet-am-auth-org-config", Set.of("tree2"));
        String betaRealm = "/beta";
        given(ci.getConfiguration(eq(betaRealm), eq(null))).willReturn(configBeta);
        String betaService = saml2AuthnConfig.getDefaultServiceForRealm(betaRealm);
        assertThat(saml2AuthnConfig.config.size()).isEqualTo(2);
        assertThat(betaService).isEqualTo("tree2");

        given(configurationActionEvent.getRealm()).willReturn("o=beta,ou=services,dc=openam,dc=forgerock,dc=org");
        given(realm.asPath()).willReturn(betaRealm);

        saml2AuthnConfig.configChanged(configurationActionEvent);

        assertThat(saml2AuthnConfig.config.size()).isEqualTo(1);
        assertThat(saml2AuthnConfig.config.keySet()).doesNotContain(betaRealm);
        assertThat(saml2AuthnConfig.config.keySet()).contains(alphaRealm);
    }

    @Test
    void shouldReturnServiceGivenServiceConfigFound() throws Exception {
        Map<String, Set<String>> config = new HashMap<>();
        config.put("iplanet-am-auth-org-config", Set.of("Example"));
        given(ci.getConfiguration(anyString(), eq(null))).willReturn(config);

        String service = saml2AuthnConfig.getDefaultServiceForRealm("testRealm");

        assertThat(service).isEqualTo("Example");
    }

    @Test
    void shouldReturnNullGivenServiceConfigNotFound() throws Exception {
        given(ci.getConfiguration(anyString(), eq(null))).willReturn(null);

        String service = saml2AuthnConfig.getDefaultServiceForRealm("testRealm");

        assertThat(service).isNull();
    }

    @Test
    void shouldReturnConfiguredDurationGivenDurationConfigFound() throws Exception {
        Map<String, Set<String>> config = new HashMap<>();
        config.put("openam-auth-authentication-sessions-max-duration", Set.of("10"));
        given(ci.getConfiguration(anyString(), eq(null))).willReturn(config);

        int duration = saml2AuthnConfig.getAuthSessionMaxDurationInSecondsForRealm("testRealm");

        assertThat(duration).isEqualTo(600);
    }

    @Test
    void shouldReturnZeroGivenDurationConfigNotFound() throws Exception {
        Map<String, Set<String>> config = new HashMap<>();
        config.put("attribute1", Set.of("aValue"));
        given(ci.getConfiguration(anyString(), eq(null))).willReturn(config);

        int duration = saml2AuthnConfig.getAuthSessionMaxDurationInSecondsForRealm("testRealm");

        assertThat(duration).isEqualTo(0);
    }


}
