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
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.authentication.modules.persistentcookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.message.MessageInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.caf.authentication.framework.AuthenticationFramework;
import org.forgerock.jaspi.modules.session.jwt.JwtSessionModule;
import org.forgerock.jaspi.modules.session.jwt.ServletJwtSessionModule;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;

public class PersistentCookiePAPTest {

    private PersistentCookieAuthModulePostAuthenticationPlugin persistentCookieAuthPAP;

    @BeforeEach
    void setUp() throws Exception {

        ServletJwtSessionModule sessionModule = new ServletJwtSessionModule(new SecretsApiJwtCryptographyHandler());
        RealmLookup realmLookup = mock(RealmLookup.class);
        PersistentCookieModuleWrapper persistentCookieWrapper = new PersistentCookieModuleWrapper(sessionModule,
                realmLookup);

        persistentCookieAuthPAP = new PersistentCookieAuthModulePostAuthenticationPlugin(persistentCookieWrapper);

        Realm realm = mock(Realm.class);
        given(realmLookup.lookup(any())).willReturn(realm);
        given(realm.asRoutingPath()).willReturn("/realms/root/realms/foo");
    }

    @Test
    void shouldInitialisePostAuthProcess() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(ssoToken.getProperty(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY)).willReturn("TOKEN_IDLE_TIME");
        given(ssoToken.getProperty(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY)).willReturn("TOKEN_MAX_LIFE");
        given(ssoToken.getProperty("openam-auth-persistent-cookie-domains")).willReturn("");

        //When
        Map<String, Object> config = persistentCookieAuthPAP.generateConfig(request, response, ssoToken);

        //Then
        assertThat(config.get(JwtSessionModule.TOKEN_IDLE_TIME_IN_MINUTES_CLAIM_KEY)).isEqualTo("TOKEN_IDLE_TIME");
        assertThat(config.get(JwtSessionModule.MAX_TOKEN_LIFE_IN_MINUTES_KEY)).isEqualTo("TOKEN_MAX_LIFE");
    }

    @Test
    void shouldInitialiseAuthModuleWithClientIPEnforcedForPAP() throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(ssoToken.getProperty("openam-auth-persistent-cookie-enforce-ip")).willReturn("true");
        given(ssoToken.getProperty("openam-auth-persistent-cookie-domains")).willReturn("");

        //When
        Map<String, Object> config = persistentCookieAuthPAP.generateConfig(request, response, ssoToken);

        //Then
        assertThat(config.get("openam-auth-persistent-cookie-enforce-ip")).isEqualTo(true);
    }

    @Test
    void shouldCallOnLoginSuccessWhenJwtNotValidated() throws Exception {

        //Given

        MessageInfo messageInfo = mock(MessageInfo.class);
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        Map<String, Object> map = new HashMap<>();
        given(messageInfo.getMap()).willReturn(map);

        Principal principal = mock(Principal.class);
        given(principal.getName()).willReturn("PRINCIPAL_NAME");

        SSOTokenID ssoTokenId = mock(SSOTokenID.class);
        given(ssoTokenId.toString()).willReturn("SSO_TOKEN_ID");

        given(ssoToken.getPrincipal()).willReturn(principal);
        given(ssoToken.getAuthType()).willReturn("AUTH_TYPE");
        given(ssoToken.getTokenID()).willReturn(ssoTokenId);
        given(ssoToken.getProperty("Organization")).willReturn("ORGANISATION");

        //When
        persistentCookieAuthPAP.onLoginSuccess(messageInfo, requestParamsMap, request, response, ssoToken);

        //Then
        assertThat(map.size()).isEqualTo(1);
        Map<String, Object> contextMap = (Map<String, Object>) map.get("org.forgerock.authentication.context");
        assertThat(contextMap.get("openam.usr")).isEqualTo("PRINCIPAL_NAME");
        assertThat(contextMap.get("openam.aty")).isEqualTo("AUTH_TYPE");
        assertThat(contextMap.get("openam.rlm")).isEqualTo("ORGANISATION");
        assertThat(contextMap.get("openam.clientip")).isNull();
    }

    @Test
    void shouldCallOnLoginSuccess() throws Exception {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        Map<String, Object> map = new HashMap<>();
        given(messageInfo.getMap()).willReturn(map);

        Principal principal = mock(Principal.class);
        given(principal.getName()).willReturn("PRINCIPAL_NAME");

        SSOTokenID ssoTokenId = mock(SSOTokenID.class);
        given(ssoTokenId.toString()).willReturn("SSO_TOKEN_ID");

        given(ssoToken.getPrincipal()).willReturn(principal);
        given(ssoToken.getAuthType()).willReturn("AUTH_TYPE");
        given(ssoToken.getTokenID()).willReturn(ssoTokenId);
        given(ssoToken.getProperty("Organization")).willReturn("ORGANISATION");
        given(ssoToken.getProperty("jwtValidated")).willReturn("true");

        //When
        persistentCookieAuthPAP.onLoginSuccess(messageInfo, requestParamsMap, request, response, ssoToken);

        //Then
        assertThat(map.size()).isEqualTo(2);
        assertThat(map.get("jwtValidated")).isEqualTo(true);
        Map<String, Object> contextMap = (Map<String, Object>) map.get("org.forgerock.authentication.context");
        assertThat(contextMap.get("openam.usr")).isEqualTo("PRINCIPAL_NAME");
        assertThat(contextMap.get("openam.aty")).isEqualTo("AUTH_TYPE");
        assertThat(contextMap.get("openam.rlm")).isEqualTo("ORGANISATION");
        assertThat(contextMap.get("openam.clientip")).isNull();
    }

    @Test
    void shouldStoreClientIPOnLoginSuccess() throws Exception {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);
        Map<String, Object> messageInfoMap = new HashMap<>();
        Map<String, Object> contextMap = new HashMap<>();
        Principal principal = mock(Principal.class);
        SSOTokenID ssoTokenID = mock(SSOTokenID.class);

        given(messageInfo.getMap()).willReturn(messageInfoMap);
        messageInfoMap.put(AuthenticationFramework.ATTRIBUTE_AUTH_CONTEXT, contextMap);
        given(ssoToken.getPrincipal()).willReturn(principal);
        given(ssoToken.getTokenID()).willReturn(ssoTokenID);
        given(request.getRemoteAddr()).willReturn("CLIENT_IP");

        //When
        persistentCookieAuthPAP.onLoginSuccess(messageInfo, Collections.emptyMap(), request, response, ssoToken);

        //Then
        assertThat(contextMap.get("openam.clientip")).isEqualTo("CLIENT_IP");
    }
}
