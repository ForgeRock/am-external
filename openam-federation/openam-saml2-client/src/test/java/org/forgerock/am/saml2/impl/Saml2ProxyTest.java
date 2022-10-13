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
 * Copyright 2015-2022 ForgeRock AS.
 */
package org.forgerock.am.saml2.impl;

import static com.sun.identity.saml2.common.SAML2Constants.RELAY_STATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.AM_LOCATION_COOKIE;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.ERROR_CODE_PARAM_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.ERROR_MESSAGE_PARAM_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.ERROR_PARAM_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.RESPONSE_KEY;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.forgerock.util.encode.Base64url;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sun.identity.saml2.common.SAML2Constants;

public class Saml2ProxyTest {

    private static final String AUTH_INDEX_TYPE_PARAM = "authIndexType";
    private static final String AUTH_INDEX_TYPE = "test";
    private static final String AUTH_INDEX_VALUE_PARAM = "authIndexValue";
    private static final String AUTH_INDEX_VALUE = "true";
    private static final String QUERY_PARAMS = AUTH_INDEX_TYPE_PARAM + "=" + AUTH_INDEX_TYPE + "&"
            + AUTH_INDEX_VALUE_PARAM + "=" + AUTH_INDEX_VALUE;
    private static final String COOKIE_LOCATION = "/openam/XUI/#login?" + QUERY_PARAMS;
    private static final String KEY = "key";
    private static final String RELAY_STATE_VALUE = "testRelayState";
    private Cookie[] validCookies;
    private static final Optional<String> LOCAL_AUTH_URL = Optional.ofNullable(null);

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
        String result = Saml2Proxy.getUrlWithKey(mockRequest, KEY, RELAY_STATE_VALUE, LOCAL_AUTH_URL);

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains(RESPONSE_KEY + "=" + KEY);
        assertThat(result).contains(ERROR_PARAM_KEY + "=" + "false");
        assertThat(result).contains(RELAY_STATE + "=" + RELAY_STATE_VALUE);
    }

    @Test
    public void shouldAddQueryParamsToLocalAuthUrl() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        Optional<String> localAuthUrl = Optional.of("http://local.test.me");

        //when
        String result = Saml2Proxy.getUrlWithKey(mockRequest, KEY, RELAY_STATE_VALUE, localAuthUrl);

        //then
        assertThat(result).contains(localAuthUrl.get());
        assertThat(result).contains(RESPONSE_KEY + "=" + KEY);
        assertThat(result).contains(ERROR_PARAM_KEY + "=" + "false");
        assertThat(result).contains(RELAY_STATE + "=" + RELAY_STATE_VALUE);
        assertThat(result).contains("?" + QUERY_PARAMS);
    }

    @Test
    public void shouldCorrectlyAppendQueryParamsToLocalAuthUrlWithParams() throws MalformedURLException,
            URISyntaxException {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        String localAuthUrlParams = "?existingParam1=123&existingParam2=two";
        Optional<String> localAuthUrl = Optional.of("http://local.test.me" + localAuthUrlParams);

        //when
        URL result = new URL(Saml2Proxy.getUrlWithKey(mockRequest, KEY, RELAY_STATE_VALUE, localAuthUrl));

        //then
        Map<String, String> expectedParams = Map.of(
                "existingParam1", "123",
                "existingParam2", "two",
                RESPONSE_KEY, KEY,
                ERROR_PARAM_KEY, "false",
                RELAY_STATE, RELAY_STATE_VALUE,
                AUTH_INDEX_TYPE_PARAM, AUTH_INDEX_TYPE,
                AUTH_INDEX_VALUE_PARAM, AUTH_INDEX_VALUE);
        verifyURL(result, "http", "local.test.me", expectedParams);
    }

    @Test
    public void shouldNotAddQueryParamsToLocalAuthUrlIfNoParams() throws MalformedURLException, URISyntaxException {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        String urlNoParams = "/am/XUI/#test";
        Cookie[] cookies = {new Cookie(AM_LOCATION_COOKIE, Base64url.encode(urlNoParams))};
        given(mockRequest.getCookies()).willReturn(cookies);
        Optional<String> localAuthUrl = Optional.of("http://local.test.me");

        //when
        URL result = new URL(Saml2Proxy.getUrlWithKey(mockRequest, KEY, RELAY_STATE_VALUE, localAuthUrl));

        //then
        Map<String, String> expectedParams = Map.of(
                RESPONSE_KEY, KEY,
                ERROR_PARAM_KEY, "false",
                RELAY_STATE, RELAY_STATE_VALUE);
        verifyURL(result, "http", "local.test.me", expectedParams);
    }

    @Test
    public void shouldCreateValidUrlFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        given(mockRequest.getMethod()).willReturn("GET");

        //when
        String result = Saml2Proxy.getUrlWithKey(mockRequest, KEY, RELAY_STATE_VALUE, LOCAL_AUTH_URL);

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains(RESPONSE_KEY + "=" + KEY);
        assertThat(result).contains(ERROR_PARAM_KEY + "=" + "false");
        assertThat(result).contains(RELAY_STATE + "=" + RELAY_STATE_VALUE);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldErrorDueToEmptyAuthenticationStepCookiePOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getMethod()).willReturn("POST");

        //when
        Saml2Proxy.getUrlWithKey(mockRequest, KEY, null, LOCAL_AUTH_URL);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldErrorDueToEmptyAuthenticationStepCookieGET() {

        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getMethod()).willReturn("GET");

        //when
        Saml2Proxy.getUrlWithKey(mockRequest, KEY, null, LOCAL_AUTH_URL);
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
        assertThat(result).contains("&" + ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + ERROR_MESSAGE_PARAM_KEY + "=" + "messageDetail");
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
        assertThat(result).contains("&" + ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + ERROR_MESSAGE_PARAM_KEY + "=" + "MyMessage");
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
        assertThat(result).contains("&" + ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + ERROR_MESSAGE_PARAM_KEY + "=" + "MyMessage");
    }

    private void verifyURL(URL result, String expectedProtocol, String expectedHost,
                           Map<String, String> expectedParams) throws URISyntaxException {
        assertThat(result.getProtocol()).isEqualTo(expectedProtocol);
        assertThat(result.getHost()).isEqualTo(expectedHost);
        List<NameValuePair> params = URLEncodedUtils.parse(result.toURI(), StandardCharsets.UTF_8);
        assertThat(params).hasSize(expectedParams.size());
        params.stream().forEach(param -> assertThat(param.getValue()).isEqualTo(expectedParams.get(param.getName())));
        assertThat(result.getPath()).isNullOrEmpty();
        assertThat(result.getRef()).isNullOrEmpty();
    }

}
