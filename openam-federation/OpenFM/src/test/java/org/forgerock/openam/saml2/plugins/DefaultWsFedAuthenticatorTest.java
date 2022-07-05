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
 * Copyright 2021 ForgeRock AS.
 */

package org.forgerock.openam.saml2.plugins;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import org.apache.http.HttpStatus;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.authentication.api.AuthenticationHandler;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookup;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.sm.ConfigurationAttributes;
import org.forgerock.openam.wsfederation.common.ActiveRequestorException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.SOAPMessage;
import java.security.Principal;
import java.util.Collections;

import static com.sun.identity.authentication.util.ISAuthConstants.AUTH_LEVEL_REQUEST_ATTR;
import static com.sun.identity.authentication.util.ISAuthConstants.PRINCIPAL_UID_REQUEST_ATTR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.shouldHaveThrown;
import static org.forgerock.json.JsonValue.array;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners(RealmTestHelper.RealmFixture.class)
public class DefaultWsFedAuthenticatorTest {

    private final static String AUTHN_SERVICE_NAME = "testAuthn";
    private final static String TEST_USERNAME = "testUser";
    private final static String TEST_PASSWORD = "testPassword";
    private final static String TEST_UID = "uid=test-uid";

    @RealmTestHelper.RealmHelper
    private static RealmTestHelper realmHelper;
    private Realm realm;

    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private AuthenticationHandler authenticationHandler;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    RealmLookup realmLookup;
    @Mock
    SSOTokenManager ssoTokenManager;
    @Mock
    ServiceConfigManager serviceConfigManager;
    @Mock
    ServiceConfig organizationConfig;
    @Mock
    ConfigurationAttributes configurationAttributes;
    @Mock
    HttpServletResponse response;
    @Mock
    SOAPMessage soapMessage;
    @Mock
    SSOToken ssoToken;
    @Mock
    Principal principal;

    private DefaultWsFedAuthenticator authenticator;
    private String camelCaseRealmName = "camelCaseRealmName";

    @BeforeMethod
    public void setup() throws Exception {

        realm = realmHelper.mockRealm(camelCaseRealmName);

        MockitoAnnotations.initMocks(this);
        authenticator = new DefaultWsFedAuthenticator(authenticationHandler, coreWrapper, realmLookup, ssoTokenManager);
        when(coreWrapper.getAdminToken()).thenReturn(mock(SSOToken.class));
        when(coreWrapper.getServiceConfigManager(any(), any())).thenReturn(serviceConfigManager);
        when(serviceConfigManager.getOrganizationConfig(any(), any())).thenReturn(organizationConfig);
        when(organizationConfig.getAttributes()).thenReturn(configurationAttributes);
        when(configurationAttributes.get(ISAuthConstants.AUTHCONFIG_ORG)).thenReturn(
                Collections.singleton(AUTHN_SERVICE_NAME));
        when(realmLookup.lookup(any())).thenReturn(realm);
        when(ssoTokenManager.createSSOToken(anyString())).thenReturn(ssoToken);
        when(ssoToken.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(TEST_UID);
        when(ssoToken.getAuthLevel()).thenReturn(5);
    }

    @Test
    public void shouldSetSsoTokenFromAuthentication() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any())).willAnswer(this::success);

        // When
        SSOToken result = authenticator.authenticate(
                servletRequest, response, soapMessage, realm.asPath(), TEST_USERNAME, TEST_PASSWORD.toCharArray());

        // Then
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(authenticationHandler).authenticate(requestCaptor.capture(), any(), any(), any(), any(), any());
        assertThat(requestCaptor.getValue()).isNotSameAs(servletRequest);
        assertThat(requestCaptor.getValue().getParameter(ISAuthConstants.REALM_PARAM)).isEqualTo(realm.asPath());
        verifyResult(result);
    }

    @Test
    public void shouldCompleteCallbacksInBatch() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::usernamePassword).willAnswer(this::success);

        // When
        SSOToken result = authenticator.authenticate(
                servletRequest, response, soapMessage, realm.asPath(), TEST_USERNAME, TEST_PASSWORD.toCharArray());

        // Then
        ArgumentCaptor<JsonValue> jsonCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(authenticationHandler, times(2))
                .authenticate(any(), any(), jsonCaptor.capture(), any(), any(), any());
        assertThat(jsonCaptor.getAllValues().get(0)).isEmpty();
        System.out.println(jsonCaptor.getAllValues().get(1).toString());
        assertThat(jsonCaptor.getAllValues().get(1).toString()).contains(
                "\"NameCallback\", \"input\": [ { \"value\": \"testUser\"");
        assertThat(jsonCaptor.getAllValues().get(1).toString()).contains(
                "\"PasswordCallback\", \"input\": [ { \"value\": \"testPassword\"");
        verifyResult(result);
    }

    @Test
    public void shouldCompleteIndividualCallbacks() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::username).willAnswer(this::password).willAnswer(this::success);

        // When
        SSOToken result = authenticator.authenticate(
                servletRequest, response, soapMessage, realm.asPath(), TEST_USERNAME, TEST_PASSWORD.toCharArray());

        // Then
        ArgumentCaptor<JsonValue> jsonCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(authenticationHandler, times(3))
                .authenticate(any(), any(), jsonCaptor.capture(), any(), any(), any());
        assertThat(jsonCaptor.getAllValues().get(0)).isEmpty();
        assertThat(jsonCaptor.getAllValues().get(1).toString()).contains(
                "\"NameCallback\", \"input\": [ { \"value\": \"testUser\"");
        assertThat(jsonCaptor.getAllValues().get(2).toString()).contains(
                "\"PasswordCallback\", \"input\": [ { \"value\": \"testPassword\"");
        verifyResult(result);
    }

    @Test
    public void shouldFailWhenEntityIsNotJson() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::notJson);

        // When
        try {
            authenticator.authenticate(
                    servletRequest, response, soapMessage, realm.asPath(), TEST_USERNAME, TEST_PASSWORD.toCharArray());
            shouldHaveThrown(ActiveRequestorException.class);
        } catch (ActiveRequestorException e) {
            // Then
            verifyFailureResult(e);
        }
    }

    @Test
    public void shouldFailWhenUserNameNotProvided() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::username).willAnswer(this::failure);

        // When
        try {
            authenticator.authenticate(servletRequest, response, soapMessage, realm.asPath(), null, null);
            shouldHaveThrown(ActiveRequestorException.class);
        } catch (ActiveRequestorException e) {
            // Then
            verifyFailureResult(e);
        }
    }

    @Test
    public void shouldFailWhenPasswordNotProvided() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::username).willAnswer(this::password).willAnswer(this::failure);

        // When
        try {
            authenticator.authenticate(servletRequest, response, soapMessage, realm.asPath(), TEST_USERNAME, null);
            shouldHaveThrown(ActiveRequestorException.class);
        } catch (ActiveRequestorException e) {
            // Then
            verifyFailureResult(e);
        }
    }

    @Test
    public void shouldFailWhenUsernameIsInvalid() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::username).willAnswer(this::password).willAnswer(this::failure);

        // When
        try {
            authenticator.authenticate(servletRequest, response, soapMessage, realm.asPath(),
                    "NonExistentUser", "IncorrectPassword".toCharArray());
            shouldHaveThrown(ActiveRequestorException.class);
        } catch (ActiveRequestorException e) {
            // Then
            verifyFailureResult(e);
        }
    }

    @Test
    public void shouldFailWhenPasswordIsInvalid() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::username).willAnswer(this::password).willAnswer(this::failure);

        // When
        try {
            authenticator.authenticate(servletRequest, response, soapMessage, realm.asPath(),
                    TEST_USERNAME, "IncorrectPassword".toCharArray());
            shouldHaveThrown(ActiveRequestorException.class);
        } catch (ActiveRequestorException e) {
            // Then
            verifyFailureResult(e);
        }
    }

    @Test
    public void shouldFailOnFailureWithoutSpecificMessage() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::failure);

        // When
        try {
            authenticator.authenticate(servletRequest, response, soapMessage, realm.asPath(), null, null);
            shouldHaveThrown(ActiveRequestorException.class);
        } catch (ActiveRequestorException e) {
            // Then
            verifyFailureResult(e);
        }
    }

    @Test
    public void shouldFailOnFailureWithSameMessage() throws Exception {
        // Given
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::failureWithMessage);

        // When
        try {
            authenticator.authenticate(servletRequest, response, soapMessage, realm.asPath(), null, null);
            shouldHaveThrown(ActiveRequestorException.class);
        } catch (ActiveRequestorException e) {
            // Then
            verifyFailureResult(e);
        }
    }

    @Test
    public void shouldFixRealmInUUID() throws Exception {
        // Given
        when(principal.getName()).thenReturn(TEST_UID + ",o=" + camelCaseRealmName +
                ",ou=services,dc=openam,dc=example,dc=com");
        given(authenticationHandler.authenticate(any(), any(), any(), any(), any(), any()))
                .willAnswer(this::successCamelRealm);

        // When
        SSOToken result = authenticator.authenticate(
                servletRequest, response, soapMessage, realm.asPath(), TEST_USERNAME, TEST_PASSWORD.toCharArray());

        // Then
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(authenticationHandler).authenticate(requestCaptor.capture(), any(), any(), any(), any(), any());
        assertThat(requestCaptor.getValue()).isNotSameAs(servletRequest);
        verifyResultCamelRealm(result);
    }

    private void verifyResult(SSOToken result) throws SSOException {
        assertThat(result).isNotNull();
        assertThat(result.getPrincipal().getName()).isEqualTo(TEST_UID);
        assertThat(result.getAuthLevel()).isEqualTo(5);
    }

    private void verifyFailureResult(ActiveRequestorException e) {
        assertThat(e.getMessage()).isEqualTo("Unable to authenticate end-user.");
        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
        assertThat(e.getErrorCode()).isEqualTo("unableToAuthenticate");
    }

    private void verifyResultCamelRealm(SSOToken result) throws SSOException {
        assertThat(result.getAuthLevel()).isEqualTo(5);
        assertThat(result.getPrincipal().getName()).isEqualTo(
                TEST_UID + ",o=" + camelCaseRealmName + ",ou=services,dc=openam,dc=example,dc=com");
    }

    private Response usernamePassword(InvocationOnMock invocation) {
        Response response = new Response(Status.OK);
        response.getEntity().setJson(object(field("callbacks", array(
                object(field("type", "NameCallback"), field("input", array(object(field("value", ""))))),
                object(field("type", "PasswordCallback"), field("input", array(object(field("value", "")))))
        ))));
        return response;
    }

    private Response username(InvocationOnMock invocation) {
        Response response = new Response(Status.OK);
        response.getEntity().setJson(object(field("callbacks", array(
                object(field("type", "NameCallback"), field("input", array(object(field("value", "")))))
        ))));
        return response;
    }

    private Response password(InvocationOnMock invocation) {
        Response response = new Response(Status.OK);
        response.getEntity().setJson(object(field("callbacks", array(
                object(field("type", "PasswordCallback"), field("input", array(object(field("value", "")))))
        ))));
        return response;
    }

    private Response success(InvocationOnMock invocation) {
        when(servletRequest.getAttribute(PRINCIPAL_UID_REQUEST_ATTR)).thenReturn(TEST_UID);
        when(servletRequest.getAttribute(AUTH_LEVEL_REQUEST_ATTR)).thenReturn(5);
        Response response = new Response(Status.OK);
        response.getEntity().setJson(object(field("successUrl", "/success"), field("tokenId", "test-token-id")));
        return response;
    }

    private Response successCamelRealm(InvocationOnMock invocation) {
        when(servletRequest.getAttribute(PRINCIPAL_UID_REQUEST_ATTR)).thenReturn(TEST_UID + ",o=" +
                camelCaseRealmName.toLowerCase() +",ou=services,dc=openam,dc=example,dc=com");
        when(servletRequest.getAttribute(AUTH_LEVEL_REQUEST_ATTR)).thenReturn(5);
        Response response = new Response(Status.OK);
        response.getEntity().setJson(object(field("successUrl", "/success"), field("tokenId", "test-token-id")));
        return response;
    }

    private Response notJson(InvocationOnMock invocation) {
        Response response = new Response(Status.OK);
        response.getEntity().setString("not json");
        return response;
    }

    private Response failure(InvocationOnMock invocation) {
        Response response = new Response(Status.UNAUTHORIZED);
        response.getEntity().setJson(object(field("failureUrl", "/failure"), field("code", "108")));
        return response;
    }

    private Response failureWithMessage(InvocationOnMock invocation) {
        Response response = new Response(Status.UNAUTHORIZED);
        response.getEntity().setJson(object(
                field("failureUrl", "/failure"),
                field("code", "108"),
                field("message", "Custom failure message")));
        return response;
    }
}
