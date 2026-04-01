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
 * Copyright 2015-2025 Ping Identity Corporation.
 */
package org.forgerock.am.saml2.impl;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.plugin.configuration.ConfigurationInstance;
import com.sun.identity.plugin.configuration.ConfigurationManager;
import com.sun.identity.plugin.monitoring.MonitorManager;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.profile.SPACSUtils;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.forgerock.openam.saml2.SAMLScriptedBindingObjectImpl;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject;
import org.forgerock.openam.scripting.domain.SAMLScriptedBindingObject.FlowInitiator;
import org.forgerock.util.encode.Base64url;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.sun.identity.saml2.common.SAML2Constants.IDP_ROLE;
import static com.sun.identity.saml2.common.SAML2Constants.RELAY_STATE;
import static com.sun.identity.saml2.common.SAML2Constants.SP_ROLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.AM_LOCATION_COOKIE;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.ERROR_CODE_PARAM_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.ERROR_MESSAGE_PARAM_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.ERROR_PARAM_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.RESPONSE_KEY;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.SAML_OBJECT_KEY_PARAM;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class Saml2ProxyTest {

    private static final String AUTH_INDEX_TYPE_PARAM = "authIndexType";
    private static final String AUTH_INDEX_TYPE = "test";
    private static final String AUTH_INDEX_VALUE_PARAM = "authIndexValue";
    private static final String AUTH_INDEX_VALUE = "true";
    private static final String QUERY_PARAMS = AUTH_INDEX_TYPE_PARAM + "=" + AUTH_INDEX_TYPE + "&"
            + AUTH_INDEX_VALUE_PARAM + "=" + AUTH_INDEX_VALUE;
    private static final String COOKIE_LOCATION = "/openam/XUI/#login?" + QUERY_PARAMS;
    private static final String SAML_SCRIPTED_BINDING_OBJECT_KEY = "samlScriptedBindingObjectKey";
    private static final String RELAY_STATE_VALUE = "testRelayState";
    private static final Optional<String> LOCAL_AUTH_URL = Optional.empty();
    private static final Optional<String> HOSTED_SP_TREE_URL =
            Optional.of("http://localhost:8080/am?realm=testRealm&authIndexType=service&authIndexValue=treeName");
    private static final Optional<String> TREE_NAME = Optional.of("treeName");
    private static final Optional<String> EMPTY_TREE_NAME = Optional.empty();
    private static final String REALM = "testRealm";

    @InjectMocks
    private Saml2Proxy saml2Proxy;
    private Cookie[] validCookies;

    @BeforeEach
    void setUp() {
        validCookies = new Cookie[1];
        validCookies[0] = new Cookie(AM_LOCATION_COOKIE, Base64url.encode(COOKIE_LOCATION));
    }

    @Test
    void shouldCreateValidHostedSpTreeUrlGivenEmptyAuthStepCookieAndEmptyLocalAuthUrlAndHostedSpTreeConfigured() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        setUpRequest(mockRequest);
        given(mockRequest.getCookies()).willReturn(null);

        //when
        String result = saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, RELAY_STATE_VALUE,
                LOCAL_AUTH_URL, REALM, TREE_NAME);

        //then
        assertThat(result).contains(HOSTED_SP_TREE_URL.get());
        assertThat(result).contains(RESPONSE_KEY + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
        assertThat(result).contains(ERROR_PARAM_KEY + "=" + "false");
        assertThat(result).contains(RELAY_STATE + "=" + RELAY_STATE_VALUE);
        assertThat(result).contains(SAML_OBJECT_KEY_PARAM + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
    }

    @Test
    void shouldThrowExceptionGivenEmptyAuthStepCookieAndEmptyLocalAuthUrlAndHostedSpTreeNotConfigured() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(null);

        //when - then
        assertThatThrownBy(() -> saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY,
                RELAY_STATE_VALUE, LOCAL_AUTH_URL, REALM, EMPTY_TREE_NAME)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldCreateValidUrlUsingLocalAuthUrlGivenAuthStepCookieAndLocalAuthUrlAndHostedSpTreeConfigured() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        Optional<String> localAuthUrl = Optional.of("http://local.test.me");

        //when
        String result = saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, RELAY_STATE_VALUE,
                localAuthUrl, REALM, HOSTED_SP_TREE_URL);

        //then
        assertThat(result).contains(localAuthUrl.get());
        assertThat(result).contains(RESPONSE_KEY + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
        assertThat(result).contains(ERROR_PARAM_KEY + "=" + "false");
        assertThat(result).contains(RELAY_STATE + "=" + RELAY_STATE_VALUE);
        assertThat(result).contains("?" + QUERY_PARAMS);
    }

    @Test
    void shouldCreateValidUrlWithoutDuplicatesUsingLocalAuthUrl() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        Optional<String> localAuthUrl = Optional.of("http://local.test.me" + "?" + QUERY_PARAMS);

        //when
        String result = saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, RELAY_STATE_VALUE,
                localAuthUrl, REALM, HOSTED_SP_TREE_URL);

        //then
        assertThat(result).contains(localAuthUrl.get());
        assertThat(result).contains(RESPONSE_KEY + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
        assertThat(result).contains(ERROR_PARAM_KEY + "=" + "false");
        assertThat(result).contains(RELAY_STATE + "=" + RELAY_STATE_VALUE);
        assertThat(result).containsOnlyOnce(QUERY_PARAMS);
    }

    @Test
    void shouldCreateValidUrlFromDataViaPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);

        //when
        String result = saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, RELAY_STATE_VALUE,
                LOCAL_AUTH_URL, REALM, EMPTY_TREE_NAME);

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains(SAML_OBJECT_KEY_PARAM + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
        assertThat(result).contains(RESPONSE_KEY + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
        assertThat(result).contains(ERROR_PARAM_KEY + "=" + "false");
        assertThat(result).contains(RELAY_STATE + "=" + RELAY_STATE_VALUE);

    }

    @Test
    void shouldAddQueryParamsToLocalAuthUrl() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        Optional<String> localAuthUrl = Optional.of("http://local.test.me");

        //when
        String result = saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, RELAY_STATE_VALUE,
                localAuthUrl, REALM, EMPTY_TREE_NAME);

        //then
        assertThat(result).contains(localAuthUrl.get());
        assertThat(result).contains(SAML_OBJECT_KEY_PARAM + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
        assertThat(result).contains(RESPONSE_KEY + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
        assertThat(result).contains(ERROR_PARAM_KEY + "=" + "false");
        assertThat(result).contains(RELAY_STATE + "=" + RELAY_STATE_VALUE);
        assertThat(result).contains("?" + QUERY_PARAMS);

    }

    @Test
    void shouldCorrectlyAppendQueryParamsToLocalAuthUrlWithParams() throws MalformedURLException,
            URISyntaxException {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        String localAuthUrlParams = "?existingParam1=123&existingParam2=two";
        Optional<String> localAuthUrl = Optional.of("http://local.test.me" + localAuthUrlParams);

        //when
        URL result = new URL(saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, RELAY_STATE_VALUE,
                localAuthUrl, REALM, EMPTY_TREE_NAME));

        //then
        Map<String, String> expectedParams = Map.of(
                "existingParam1", "123",
                "existingParam2", "two",
                SAML_OBJECT_KEY_PARAM, SAML_SCRIPTED_BINDING_OBJECT_KEY,
                RESPONSE_KEY, SAML_SCRIPTED_BINDING_OBJECT_KEY,
                ERROR_PARAM_KEY, "false",
                RELAY_STATE, RELAY_STATE_VALUE,
                AUTH_INDEX_TYPE_PARAM, AUTH_INDEX_TYPE,
                AUTH_INDEX_VALUE_PARAM, AUTH_INDEX_VALUE);
        verifyURL(result, "http", "local.test.me", expectedParams);

    }

    @Test
    void shouldNotAddQueryParamsToLocalAuthUrlIfNoParams() throws MalformedURLException, URISyntaxException {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        String urlNoParams = "/am/XUI/#test";
        Cookie[] cookies = {new Cookie(AM_LOCATION_COOKIE, Base64url.encode(urlNoParams))};
        given(mockRequest.getCookies()).willReturn(cookies);
        Optional<String> localAuthUrl = Optional.of("http://local.test.me");

        //when
        URL result = new URL(saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, RELAY_STATE_VALUE,
                localAuthUrl, REALM, EMPTY_TREE_NAME));

        //then
        Map<String, String> expectedParams = Map.of(
                SAML_OBJECT_KEY_PARAM, SAML_SCRIPTED_BINDING_OBJECT_KEY,
                RESPONSE_KEY, SAML_SCRIPTED_BINDING_OBJECT_KEY,
                ERROR_PARAM_KEY, "false",
                RELAY_STATE, RELAY_STATE_VALUE);
        verifyURL(result, "http", "local.test.me", expectedParams);

    }

    @Test
    void shouldCreateValidUrlFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);

        //when
        String result = saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, RELAY_STATE_VALUE,
                LOCAL_AUTH_URL, REALM, EMPTY_TREE_NAME);

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains(SAML_OBJECT_KEY_PARAM + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
        assertThat(result).contains(RESPONSE_KEY + "=" + SAML_SCRIPTED_BINDING_OBJECT_KEY);
        assertThat(result).contains(ERROR_PARAM_KEY + "=" + "false");
        assertThat(result).contains(RELAY_STATE + "=" + RELAY_STATE_VALUE);

    }

    @Test
    void shouldErrorGivenEmptyAuthenticationStepCookieAndHostedSpTreeNotConfiguredAndLocalAuthUrlNotConfiguredPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        //when - then
        assertThatThrownBy(() -> saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, null,
                LOCAL_AUTH_URL, REALM, EMPTY_TREE_NAME))
                .isInstanceOf(IllegalStateException.class);

    }

    @Test
    void shouldErrorDueToEmptyAuthenticationStepCookieGET() {

        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        //when - then
        assertThatThrownBy(() -> saml2Proxy.getUrlWithKey(mockRequest, SAML_SCRIPTED_BINDING_OBJECT_KEY, null,
                LOCAL_AUTH_URL, REALM, EMPTY_TREE_NAME))
                .isInstanceOf(IllegalStateException.class);

    }

    @Test
    void shouldCreateDefaultErrorHTMLPostFromDataViaPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        String errorType = "200";

        //when
        String response = saml2Proxy.getUrlWithError(mockRequest, errorType);

        //then
        assertThat(response).contains(urlEncodeQueryParameterNameOrValue(Saml2Proxy.DEFAULT_ERROR_MESSAGE));
    }

    @Test
    void shouldCreateDefaultErrorHTMLPostFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        String errorType = "200";

        //when
        String url = saml2Proxy.getUrlWithError(mockRequest, errorType);

        //then
        assertThat(url).contains(urlEncodeQueryParameterNameOrValue(Saml2Proxy.DEFAULT_ERROR_MESSAGE));
    }

    @Test
    void shouldCreateErrorHTMLPostFromDataViaPOST() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        String errorType = "200";

        //when
        String result = saml2Proxy.getUrlWithError(mockRequest, errorType, "messageDetail");

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains("&" + ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + ERROR_MESSAGE_PARAM_KEY + "=" + "messageDetail");
    }

    @Test
    void shouldCreateErrorHTMLPostFromDataViaGET() {
        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        String errorType = "200";

        //when
        String result = saml2Proxy.getUrlWithError(mockRequest, errorType, "MyMessage");

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains("&" + ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + ERROR_MESSAGE_PARAM_KEY + "=" + "MyMessage");
    }

    @Test
    void shouldCreateErrorHTMLPostFromDataWithMessage() {

        //given
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        given(mockRequest.getCookies()).willReturn(validCookies);
        String errorType = "200";

        //when
        String result = saml2Proxy.getUrlWithError(mockRequest, errorType, "MyMessage");

        //then
        assertThat(result).contains(COOKIE_LOCATION);
        assertThat(result).contains("&" + ERROR_PARAM_KEY + "=" + true);
        assertThat(result).contains("&" + ERROR_CODE_PARAM_KEY + "=" + errorType);
        assertThat(result).contains("&" + ERROR_MESSAGE_PARAM_KEY + "=" + "MyMessage");
    }

    @ParameterizedTest
    @MethodSource("processSamlResponseTestCases")
    void shouldProcessSamlResponseWhenValidRequest(String idpEntityId, AuthnRequest authnRequest)
            throws SAML2Exception, IOException {
        // Given
        try (MockedStatic<SAMLUtils> samlUtilsMock = mockStatic(SAMLUtils.class);
             MockedStatic<ConfigurationManager> configurationManagerMock = setupConfigurationManagerMock();
             MockedStatic<SAML2Utils> saml2UtilsMock = mockStatic(SAML2Utils.class);
             MockedStatic<SystemPropertiesManager> systemPropertiesManagerMock = setupSystemPropertiesManagerMock();
             MockedStatic<FSUtils> fsUtilsMock = mockStatic(FSUtils.class);
             MockedStatic<MonitorManager> monitorManagerMock = mockStatic(MonitorManager.class);
             MockedStatic<SPACSUtils> spacsUtilsMock = mockStatic(SPACSUtils.class);
             MockedStatic<SAML2MetaUtils> saml2MetaUtilsMock = mockStatic(SAML2MetaUtils.class);
             MockedStatic<SAML2FailoverUtils> saml2FailoverUtilsMock = mockStatic(SAML2FailoverUtils.class);
             MockedStatic<CookieUtils> cookieUtilsMock = mockStatic(CookieUtils.class)) {

            String spEntityId = "testSpEntityId";
            String metaAlias = "testMetaAlias";
            String realm = "testRealm";

            HttpServletRequest mockRequest = mock(HttpServletRequest.class);
            StringBuffer requestURL = new StringBuffer();
            when(mockRequest.getRequestURL()).thenReturn(requestURL);
            HttpServletResponse mockResponse = mock(HttpServletResponse.class);

            SAML2MetaManager metaManager = mock(SAML2MetaManager.class);
            when(metaManager.getEntityByMetaAlias(any())).thenReturn(spEntityId);
            saml2UtilsMock.when(SAML2Utils::getSAML2MetaManager).thenReturn(metaManager);

            var spConfigAttributes = Map.of();
            saml2UtilsMock.when(() -> SAML2Utils.getAllAttributesFromSSOConfig(realm, spEntityId, SP_ROLE))
                    .thenReturn(spConfigAttributes);
            var idpConfigAttributes = Map.of();
            saml2UtilsMock.when(() -> SAML2Utils.getAllAttributesFromSSOConfig(realm, idpEntityId, IDP_ROLE))
                    .thenReturn(idpConfigAttributes);

            var encodedLocation = Base64url.encode(COOKIE_LOCATION);
            cookieUtilsMock.when(() -> CookieUtils.getCookieValueFromReq(mockRequest, AM_LOCATION_COOKIE))
                    .thenReturn(encodedLocation);

            ResponseInfo responseInfo = mock(ResponseInfo.class);
            given(responseInfo.getProfileBinding()).willReturn(SAML2Constants.HTTP_POST);
            spacsUtilsMock.when(() -> SPACSUtils.getResponse(any(), any(), any(), any(), any()))
                    .thenReturn(responseInfo);

            saml2MetaUtilsMock.when(() -> SAML2MetaUtils.getMetaAliasByUri(anyString())).thenReturn(metaAlias);

            saml2MetaUtilsMock.when(() -> SAML2MetaUtils.getRealmByMetaAlias(metaAlias))
                    .thenReturn(realm);

            Map smap = mockSamlMap(idpEntityId, authnRequest);

            saml2UtilsMock.when(() -> SAML2Utils.verifyResponse(mockRequest, mockResponse,
                            responseInfo.getResponse(), realm, spEntityId, responseInfo.getProfileBinding()))
                    .thenReturn(smap);

            // When
            saml2Proxy.processSamlResponse(mockRequest, mockResponse);

            // Then
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockResponse).sendRedirect(urlCaptor.capture());
            var redirectUrl = urlCaptor.getValue();
            assertThat(redirectUrl).startsWith(COOKIE_LOCATION).contains(SAML_OBJECT_KEY_PARAM);

            ArgumentCaptor<SAMLScriptedBindingObjectImpl> samlScriptedBindingObjectCaptor =
                    ArgumentCaptor.forClass(SAMLScriptedBindingObjectImpl.class);
            saml2FailoverUtilsMock.verify(() ->
                    SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(anyString(),
                            samlScriptedBindingObjectCaptor.capture()));
            var samlScriptedBindingObject = samlScriptedBindingObjectCaptor.getValue();

            var expectedFlowInitiator = authnRequest == null ? FlowInitiator.IDP.name() : FlowInitiator.SP.name();
            assertThat(samlScriptedBindingObject).extracting(
                            SAMLScriptedBindingObjectImpl::getRawAuthnRequest,
                            SAMLScriptedBindingObject::getFlowInitiator,
                            SAMLScriptedBindingObject::getApplicationId,
                            SAMLScriptedBindingObject::getIdpAttributes,
                            SAMLScriptedBindingObject::getSpAttributes,
                            SAMLScriptedBindingObjectImpl::getRawAssertion)
                    .containsExactly(
                            smap.get(SAML2Constants.AUTHN_REQUEST),
                            expectedFlowInitiator,
                            spEntityId,
                            idpConfigAttributes,
                            spConfigAttributes,
                            smap.get(SAML2Constants.POST_ASSERTION));
        }

    }

    public static Stream<Arguments> processSamlResponseTestCases() {
        return Stream.of(
                Arguments.of((String) null, (AuthnRequest) null),
                Arguments.of("idpEntityId", mock(AuthnRequest.class))
        );
    }

    private MockedStatic<SystemPropertiesManager> setupSystemPropertiesManagerMock() {
        MockedStatic<SystemPropertiesManager> systemPropertiesManagerMockedStatic =
                mockStatic(SystemPropertiesManager.class);
        systemPropertiesManagerMockedStatic.when(() -> SystemPropertiesManager.get(Constants.AM_SERVER_PORT))
                .thenReturn("8080");
        return systemPropertiesManagerMockedStatic;
    }

    private MockedStatic<ConfigurationManager> setupConfigurationManagerMock() {
        MockedStatic<ConfigurationManager> configurationManagerMockedStatic = mockStatic(ConfigurationManager.class);
        ConfigurationInstance configurationInstanceMock = mock(ConfigurationInstance.class);
        configurationManagerMockedStatic.when(() ->
                        ConfigurationManager.getConfigurationInstance(COTConstants.COT_CONFIG_NAME))
                .thenReturn(configurationInstanceMock);
        configurationManagerMockedStatic.when(() -> ConfigurationManager.getConfigurationInstance("SAML2"))
                .thenReturn(configurationInstanceMock);
        return configurationManagerMockedStatic;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map mockSamlMap(String idpEntityId, AuthnRequest authnRequest) {
        Subject subject = mock(Subject.class);
        Assertion assertion = mock(Assertion.class);
        var issuer = mock(Issuer.class);
        when(assertion.getIssuer()).thenReturn(issuer);
        when(issuer.getValue()).thenReturn(idpEntityId);

        Map smap = new HashMap();
        smap.put(SAML2Constants.AUTHN_REQUEST, authnRequest);
        smap.put(SAML2Constants.SUBJECT, subject);
        smap.put(SAML2Constants.POST_ASSERTION, assertion);
        smap.put(SAML2Constants.NOTONORAFTER, 123456L);
        return smap;
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

    private void setUpRequest(HttpServletRequest mockRequest) {
        given(mockRequest.getScheme()).willReturn("http");
        given(mockRequest.getServerName()).willReturn("localhost");
        given(mockRequest.getServerPort()).willReturn(8080);
        given(mockRequest.getContextPath()).willReturn("/am");
    }

}
