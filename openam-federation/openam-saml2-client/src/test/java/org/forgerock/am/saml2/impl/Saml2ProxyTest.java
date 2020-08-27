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
 * Copyright 2015-2019 ForgeRock AS.
 */
package org.forgerock.am.saml2.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.AM_LOCATION_COOKIE;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.util.encode.Base64url;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sun.identity.saml2.common.SAML2Constants;

public class Saml2ProxyTest {

    private static final String COOKIE_LOCATION = "/openam/XUI/#login";
    private static final String KEY = "key";
    private Cookie[] validCookies;

    @BeforeTest
    void setUp() {
        validCookies = new Cookie[1];
        validCookies[0] = new Cookie(AM_LOCATION_COOKIE, Base64url.encode(COOKIE_LOCATION));
    }

    @Test
    public void shouldCreateValidUrlFromDataViaPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");

        //when
        String result = Saml2Proxy.getUrlWithKey(mockRequest, KEY);

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains(Saml2ClientConstants.RESPONSE_KEY + "=" + KEY);
        assertThat(result).contains(Saml2ClientConstants.ERROR_PARAM_KEY + "=" + "false");
    }

    @Test
    public void shouldCreateValidUrlFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("GET");

        //when
        String result = Saml2Proxy.getUrlWithKey(mockRequest, KEY);

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains(Saml2ClientConstants.RESPONSE_KEY + "=" + KEY);
        assertThat(result).contains(Saml2ClientConstants.ERROR_PARAM_KEY + "=" + "false");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldErrorDueToEmptyAuthenticationStepCookiePOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getMethod()).willReturn("POST");

        //when
        Saml2Proxy.getUrlWithKey(mockRequest, KEY);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldErrorDueToEmptyAuthenticationStepCookieGET() {

        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getMethod()).willReturn("GET");

        //when
        Saml2Proxy.getUrlWithKey(mockRequest, KEY);
    }

    @Test
    public void shouldCreateDefaultErrorHTMLPostFromDataViaPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");
        String errorType = "200";

        //when
        String response = Saml2Proxy.getUrlWithError(mockRequest, errorType);

        //then
        assertThat(response).contains(urlEncodeQueryParameterNameOrValue(Saml2Proxy.DEFAULT_ERROR_MESSAGE));
    }

    @Test
    public void shouldCreateDefaultErrorHTMLPostFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");
        String errorType = "200";

        //when
        String url = Saml2Proxy.getUrlWithError(mockRequest, errorType);

        //then
        assertThat(url).contains(urlEncodeQueryParameterNameOrValue(Saml2Proxy.DEFAULT_ERROR_MESSAGE));
    }

    @Test
    public void shouldCreateErrorHTMLPostFromDataViaPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");
        String errorType = "200";
        given(mockRequest.getParameter(SAML2Constants.SAML_RESPONSE)).willReturn("SAMLResponse");

        //when
        String result = Saml2Proxy.getUrlWithError(mockRequest, errorType, "messageDetail");

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains("&" + Saml2ClientConstants.ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + Saml2ClientConstants.ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + Saml2ClientConstants.ERROR_MESSAGE_PARAM_KEY + "=" + "messageDetail");
    }

    @Test
    public void shouldCreateErrorHTMLPostFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("GET");
        String errorType = "200";

        //when
        String result = Saml2Proxy.getUrlWithError(mockRequest, errorType, "MyMessage");

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains("&" + Saml2ClientConstants.ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + Saml2ClientConstants.ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + Saml2ClientConstants.ERROR_MESSAGE_PARAM_KEY + "=" + "MyMessage");
    }

    @Test
    public void shouldCreateErrorHTMLPostFromDataWithMessage() {

        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("POST");
        String errorType = "200";

        //when
        String result = Saml2Proxy.getUrlWithError(mockRequest, errorType, "MyMessage");

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains("&" + Saml2ClientConstants.ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + Saml2ClientConstants.ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + Saml2ClientConstants.ERROR_MESSAGE_PARAM_KEY + "=" + "MyMessage");
    }
}
