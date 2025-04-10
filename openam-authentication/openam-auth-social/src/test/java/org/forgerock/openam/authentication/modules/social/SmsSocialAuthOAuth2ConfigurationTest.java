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
 * Copyright 2017-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.social;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.oauth.clients.oauth2.OAuth2ClientConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for SmsSocialAuthOAuth2Configuration.
 */
public class SmsSocialAuthOAuth2ConfigurationTest {

    private final Map<String, Set<String>> options = mock(Map.class);

    private SmsSocialAuthOAuth2Configuration configuration;

    @BeforeEach
    void setup() {
        given(options.get("clientId")).willReturn(Collections.singleton("client"));
        given(options.get("clientSecret")).willReturn(Collections.singleton("secret"));
        given(options.get("authorizeEndpoint")).willReturn(Collections.singleton("http://authz/endpoint"));
        given(options.get("tokenEndpoint")).willReturn(Collections.singleton("http://token/endpoint"));
        given(options.get("userInfoEndpoint")).willReturn(Collections.singleton("http://userinfo/endpoint"));
        given(options.get("scope")).willReturn(Collections.singleton("email"));
        given(options.get("scopeDelimiter")).willReturn(Collections.singleton(":"));
        given(options.get("usesBasicAuth")).willReturn(Collections.singleton("true"));
        given(options.get("ssoProxyUrl")).willReturn(Collections.singleton("http://sso/proxy"));
        given(options.get("subjectProperty")).willReturn(Collections.singleton("sub property"));
        given(options.get("provider")).willReturn(Collections.singleton("provider"));
        given(options.get("clientConfigClass"))
                .willReturn(Collections.singleton("org.forgerock.oauth.clients.oauth2.OAuth2ClientConfiguration"));
    }

    @Test
    void shouldBuildTheClientConfiguration() throws Exception {
        //when
        configuration = new SmsSocialAuthOAuth2Configuration(options);
        OAuth2ClientConfiguration config = (OAuth2ClientConfiguration) configuration.getOAuthClientConfiguration();

        //then
        assertThat(config.getClientId()).isEqualTo("client");
        assertThat(config.getClientSecret()).isEqualTo("secret");
        assertThat(config.getAuthorizationEndpoint()).isEqualTo(new URI("http://authz/endpoint"));
        assertThat(config.getTokenEndpoint()).isEqualTo(URI.create("http://token/endpoint"));
        assertThat(config.getUserInfoEndpoint()).isEqualTo(URI.create("http://userinfo/endpoint"));
        assertThat(config.getScope()).isEqualTo(List.of("email"));
        assertThat(config.getScopeDelimiter()).isEqualTo(":");
        assertThat(config.getProvider()).isEqualTo("provider");
        assertThat(config.getAuthenticationFilter()).isInstanceOf(
                Class.forName("org.forgerock.http.filter.Filters$HttpBasicAuthClientFilter"));
    }

    @Test
    void shouldFailWhenProviderNotSpecified() {
        //given
        given(options.get("provider")).willReturn(null);
        configuration = new SmsSocialAuthOAuth2Configuration(options);

        //when
        assertThatThrownBy(() -> configuration.getOAuthClientConfiguration())
                //then
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No value found for key 'provider'.");
    }

    @Test
    void shouldFailWhenClientIdNotSpecified() {
        //given
        given(options.get("clientId")).willReturn(null);
        configuration = new SmsSocialAuthOAuth2Configuration(options);

        //when
        assertThatThrownBy(() -> configuration.getOAuthClientConfiguration())
                //then
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No value found for key 'clientId'.");
    }

    @Test
    void shouldFailWhenClientSecretNotSpecified() {
        //given
        given(options.get("clientSecret")).willReturn(null);
        configuration = new SmsSocialAuthOAuth2Configuration(options);

        //when
        assertThatThrownBy(() -> configuration.getOAuthClientConfiguration())
                //then
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No value found for key 'clientSecret'.");
    }

    @Test
    void shouldFailWhenAuthzEndpointNotValid() {
        //given
        given(options.get("authorizeEndpoint")).willReturn(Collections.singleton("authz/endpoint"));
        configuration = new SmsSocialAuthOAuth2Configuration(options);

        //when
        assertThatThrownBy(() -> configuration.getOAuthClientConfiguration())
                //then
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting authorizationEndpoint to be absolute.");
    }

    @Test
    void shouldFailWhenTokenEndpointNotValid() {
        //given
        given(options.get("tokenEndpoint")).willReturn(Collections.singleton("token/endpoint"));
        configuration = new SmsSocialAuthOAuth2Configuration(options);

        //when
        assertThatThrownBy(() -> configuration.getOAuthClientConfiguration())
                //then
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting tokenEndpoint to be absolute.");
    }

    @Test
    void shouldFailWhenUserInfoEndpointNotValid() {
        //given
        given(options.get("userInfoEndpoint")).willReturn(Collections.singleton("userinfo/endpoint"));
        configuration = new SmsSocialAuthOAuth2Configuration(options);

        //when
        assertThatThrownBy(() -> configuration.getOAuthClientConfiguration())
                //then
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting userInfoEndpoint to be absolute.");
    }

    @Test
    void shouldFailWhenredirectUrlNotValid() {
        //given
        given(options.get("ssoProxyUrl")).willReturn(Collections.singleton("sso/proxy"));
        configuration = new SmsSocialAuthOAuth2Configuration(options);

        //when
        assertThatThrownBy(() -> configuration.getOAuthClientConfiguration())
                //then
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expecting redirectUri to be absolute.");
    }
}
