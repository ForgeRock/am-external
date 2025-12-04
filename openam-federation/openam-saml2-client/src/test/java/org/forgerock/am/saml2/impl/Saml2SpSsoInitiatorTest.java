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
 * Copyright 2019-2025 Ping Identity Corporation.
 */
package org.forgerock.am.saml2.impl;

import static com.sun.identity.saml2.common.SAML2Constants.HTTP_POST;
import static com.sun.identity.saml2.common.SAML2Constants.HTTP_REDIRECT;
import static com.sun.identity.saml2.common.SAML2Constants.SAML_REQUEST;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.AM_LOCATION_COOKIE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import javax.security.auth.callback.Callback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.am.saml2.api.Saml2SsoException;
import org.forgerock.am.saml2.api.Saml2SsoInitiator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.headers.CookieUtilsWrapper;
import org.forgerock.util.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.saml2.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class Saml2SpSsoInitiatorTest {

    @Mock
    private CookieUtilsWrapper cookieUtils;
    @Mock
    private SAML2MetaManager metaManager;
    @Mock
    private AuthnRequestUtils authnRequestUtils;
    @Mock
    private IDPSSODescriptorType idpDescriptor;
    @Mock
    private SPSSODescriptorType spDescriptor;
    @Mock
    private BaseConfigType spConfig;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Realm realm;
    private Saml2SsoInitiator client;

    @BeforeEach
    void setup() throws Exception {
        client = new Saml2SpSsoInitiator(metaManager, cookieUtils, authnRequestUtils);

        given(realm.asPath()).willReturn("/");

        given(metaManager.getIDPSSODescriptor(any(), eq("idp"))).willReturn(idpDescriptor);
        given(metaManager.getSPSSODescriptor(any(), eq("sp"))).willReturn(spDescriptor);
        given(metaManager.getSPSSOConfig(any(), eq("sp"))).willReturn(new SPSSOConfigElement(spConfig));
        given(spConfig.getAttribute()).willReturn(Collections.emptyList());
    }

    @Test
    void shouldThrowSaml2ClientExceptionIfStandardMetadataIsMissing() {
        assertThatThrownBy(
            () -> client.initiateSso(request, response, realm, "badger", "idp", Options.defaultOptions()))
            .isInstanceOf(Saml2SsoException.class)
            .hasMessageMatching(".*SAML2 Configuration.*");
    }

    @Test
    void shouldThrowSaml2ClientExceptionIfSsoEndpointCannotBeFound() {
        assertThatThrownBy(() -> client.initiateSso(request, response, realm, "sp", "idp", Options.defaultOptions()))
            .isInstanceOf(Saml2SsoException.class)
            .hasMessageMatching(".*Single Sign-on.*");
    }

    @Test
    void shouldCacheAuthnRequest() throws Exception {
        given(authnRequestUtils.findSsoEndpoint(eq(idpDescriptor), any())).willReturn(ssoEndpoint(HTTP_POST));

        client.initiateSso(request, response, realm, "sp", "idp", Options.defaultOptions());

        verify(authnRequestUtils).cacheAuthnRequest(any());
    }

    @Test
    void shouldCreateAuthenticationStepCookie() throws Exception {
        given(authnRequestUtils.findSsoEndpoint(eq(idpDescriptor), any())).willReturn(ssoEndpoint(HTTP_POST));

        client.initiateSso(request, response, realm, "sp", "idp", Options.defaultOptions());

        // "bnVsbC8_cmVhbG09Lw" base64 decoded is - 'null/?realm=/'. Previously this was 'null/XUI/?realm=/'
        verify(cookieUtils).addCookieToResponseForRequestDomains(eq(request), eq(response), eq(AM_LOCATION_COOKIE),
            eq("bnVsbC8_cmVhbG09Lw"), eq(-1));
    }

    @Test
    void shouldReturnRedirectCallbackForRedirectBinding() throws Exception {
        given(authnRequestUtils.findSsoEndpoint(eq(idpDescriptor), any())).willReturn(ssoEndpoint(HTTP_POST));
        given(authnRequestUtils.encodeForPostBinding(any(), eq(realm), eq("idp"), eq(spDescriptor), eq("sp"),
            eq(idpDescriptor))).willReturn("weasel");

        Callback callback = client.initiateSso(request, response, realm, "sp", "idp",
            Options.defaultOptions());

        assertThat(callback).isInstanceOf(RedirectCallback.class);
        RedirectCallback redirectCallback = (RedirectCallback) callback;
        assertThat(redirectCallback.getTrackingCookie()).isTrue();
        assertThat(redirectCallback.getRedirectUrl()).isEqualTo("badger");
        assertThat(redirectCallback.getRedirectData()).isEqualTo(singletonMap(SAML_REQUEST, "weasel"));
        assertThat(redirectCallback.getMethod()).isEqualTo("POST");
    }

    @Test
    void shouldReturnRedirectCallbackForPostBinding() throws Exception {
        EndpointType ssoEndpoint = ssoEndpoint(HTTP_REDIRECT);
        given(authnRequestUtils.findSsoEndpoint(eq(idpDescriptor), any())).willReturn(ssoEndpoint);
        given(authnRequestUtils.getRedirectBindingUrl(any(), eq(realm), eq("idp"), eq(spDescriptor), eq("sp"),
            eq(idpDescriptor), eq(ssoEndpoint))).willReturn("weasel");

        Callback callback = client.initiateSso(request, response, realm, "sp", "idp",
            Options.defaultOptions());

        assertThat(callback).isInstanceOf(RedirectCallback.class);
        RedirectCallback redirectCallback = (RedirectCallback) callback;
        assertThat(redirectCallback.getTrackingCookie()).isTrue();
        assertThat(redirectCallback.getRedirectUrl()).isEqualTo("weasel");
        assertThat(redirectCallback.getRedirectData()).isNull();
        assertThat(redirectCallback.getMethod()).isEqualTo("GET");
    }

    @Test
    void shouldEncodeAuthnRequestForRedirectBinding() throws Exception {
        EndpointType ssoEndpoint = ssoEndpoint(HTTP_REDIRECT);
        given(authnRequestUtils.findSsoEndpoint(eq(idpDescriptor), any())).willReturn(ssoEndpoint);

        client.initiateSso(request, response, realm, "sp", "idp", Options.defaultOptions());

        verify(authnRequestUtils).getRedirectBindingUrl(any(), eq(realm), eq("idp"), eq(spDescriptor), eq("sp"),
            eq(idpDescriptor), eq(ssoEndpoint));
    }

    private EndpointType ssoEndpoint(String binding) {
        EndpointType endpoint = new EndpointType();
        endpoint.setLocation("badger");
        endpoint.setBinding(binding);
        return endpoint;
    }
}
