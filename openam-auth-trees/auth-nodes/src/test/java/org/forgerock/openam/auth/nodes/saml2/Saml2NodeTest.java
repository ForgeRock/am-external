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
package org.forgerock.openam.auth.nodes.saml2;

import static com.sun.identity.shared.FeatureEnablementConstants.DISABLE_SAML2_RELAY_STATE_VALIDATION;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.AM_LOCATION_COOKIE;
import static org.forgerock.am.saml2.impl.Saml2ClientConstants.RESPONSE_KEY;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.assertThat;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.USERNAME;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.ACCOUNT_EXISTS;
import static org.forgerock.openam.auth.nodes.oauth.AbstractSocialAuthLoginNode.SocialAuthOutcome.NO_ACCOUNT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.Optional;

import javax.security.auth.callback.Callback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.forgerock.am.identity.application.LegacyIdentityService;
import org.forgerock.am.saml2.api.AuthComparison;
import org.forgerock.am.saml2.api.Saml2Options;
import org.forgerock.am.saml2.api.Saml2SsoInitiator;
import org.forgerock.am.saml2.impl.Saml2ResponseData;
import org.forgerock.am.saml2.impl.Saml2SsoResponseUtils;
import org.forgerock.am.saml2.profile.Saml2SsoResult;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.ExternalRequestContext.Builder;
import org.forgerock.openam.auth.node.api.NodeProcessException;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.saml2.Saml2Node.Binding;
import org.forgerock.openam.auth.nodes.saml2.Saml2Node.Config;
import org.forgerock.openam.auth.nodes.saml2.Saml2Node.RequestBinding;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.headers.CookieUtilsWrapper;
import org.forgerock.util.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iplanet.am.util.SystemPropertiesWrapper;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.impl.NameIDImpl;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaManager;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class Saml2NodeTest {

    private Saml2Node node;
    @Mock
    private Config config;
    @Mock
    private Realm realm;
    @Mock
    private Saml2SsoInitiator ssoInitiator;
    @Mock
    private Saml2SsoResponseUtils responseUtils;
    @Mock
    private CookieUtilsWrapper cookieUtils;
    @Mock
    private SAML2MetaManager metaManager;
    @Mock
    private LegacyIdentityService identityService;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private HttpServletResponse servletResponse;
    @Mock
    private SystemPropertiesWrapper systemPropertiesWrapper;

    @BeforeEach
    void setup() throws Exception {
        given(config.idpEntityId()).willReturn("idp-entity-id");
        given(metaManager.getEntityByMetaAlias(any())).willReturn("sp-entity-id");
        node = new Saml2Node(config, realm, ssoInitiator, responseUtils, cookieUtils, metaManager, identityService,
                systemPropertiesWrapper);
    }

    @Test
    void testProcessAddsIdentifiedIdentityOfExistingUser() throws Exception {
        // Given
        setupSuccessfulFederation();

        // When
        Action action = node.process(getContext(Map.of(RESPONSE_KEY, new String[]{"storage-key"})));

        // Then
        assertThat(action.identifiedIdentity).isPresent();
    }

    @Test
    void testProcessDoesNotAddIdentifiedIdentityOfNonExistentUser() throws Exception {
        // Given
        setupSuccessfulFederation(ssoResult("universalId", true));
        given(identityService.doesIdentityExist("universalId")).willReturn(false);

        // When
        Action action = node.process(getContext(Map.of(RESPONSE_KEY, new String[]{"storage-key"})));

        // Then
        assertThat(action.identifiedIdentity).isEmpty();
    }

    @Test
    void shouldBuildUpSaml2OptionsUsingConfig() throws Exception {
        IDPSSODescriptorType idpDescriptor = new IDPSSODescriptorType();
        given(metaManager.getIDPSSODescriptor(any(), any())).willReturn(idpDescriptor);
        SPSSODescriptorType spDescriptor = new SPSSODescriptorType();
        given(metaManager.getSPSSODescriptor(any(), any())).willReturn(spDescriptor);
        mockConfig();
        ArgumentCaptor<Options> captor = ArgumentCaptor.forClass(Options.class);
        given(ssoInitiator.initiateSso(any(), any(), any(), any(), any(), captor.capture())).willReturn(null);

        node.process(getContext(emptyMap()));

        Options options = captor.getValue();
        assertThat(options).isNotNull();
        assertThat(options.get(Saml2Options.ALLOW_CREATE)).isTrue();
        assertThat(options.get(Saml2Options.AUTH_COMPARISON)).isEqualTo(AuthComparison.MAXIMUM);
        assertThat(options.get(Saml2Options.AUTH_CONTEXT_CLASS_REF)).containsOnly("class-ref-1", "class-ref-2");
        assertThat(options.get(Saml2Options.AUTH_CONTEXT_DECL_REF)).containsOnly("class-decl-1", "class-decl-2");
        assertThat(options.get(Saml2Options.FORCE_AUTHN)).isTrue();
        assertThat(options.get(Saml2Options.IS_PASSIVE)).isTrue();
        assertThat(options.get(Saml2Options.NAME_ID_FORMAT)).isEqualTo("custom:name-id");
        assertThat(options.get(Saml2Options.REQUEST_BINDING)).isEqualTo(RequestBinding.HTTP_POST.toString());
        assertThat(options.get(Saml2Options.RESPONSE_BINDING)).isEqualTo(Binding.HTTP_POST.toString());
    }

    @Test
    void shouldInitiateSsoOnFirstCall() throws Exception {
        IDPSSODescriptorType idpDescriptor = new IDPSSODescriptorType();
        given(metaManager.getIDPSSODescriptor(any(), any())).willReturn(idpDescriptor);
        SPSSODescriptorType spDescriptor = new SPSSODescriptorType();
        given(metaManager.getSPSSODescriptor(any(), any())).willReturn(spDescriptor);
        mockConfig();
        Callback callback = mock(Callback.class);
        given(ssoInitiator.initiateSso(any(), any(), any(), any(), any(), any())).willReturn(callback);

        Action action = node.process(getContext(emptyMap()));

        assertThat(action).isNotNull();
        assertThat(action.callbacks).isNotEmpty().containsOnly(callback);
    }

    @Test
    void shouldThrowExceptionIfResponseCouldNotBeProcessed() throws Exception {
        given(servletRequest.getParameter("error")).willReturn("true");

        assertThatThrownBy(() -> node.process(getContext(emptyMap())))
                .isInstanceOf(NodeProcessException.class)
                .hasMessage("Unable to verify the response.");
    }

    @Test
    void shouldThrowExceptionIfTheSaml2ResponseDataIsMissing() throws Exception {
        assertThatThrownBy(() -> node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"}))))
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("response not found");
    }

    @Test
    void shouldProcessSaml2ResponseOnSecondCall() throws Exception {
        setupSuccessfulFederation();

        Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

        verify(cookieUtils).addCookieToResponseForRequestDomains(servletRequest, servletResponse, AM_LOCATION_COOKIE,
                "", 0);
        assertThat(action.callbacks).isEmpty();
        assertThat(action.outcome).isEqualTo(ACCOUNT_EXISTS.name());
        assertThat(action.sharedState).stringAt(USERNAME).isEqualTo("userId");
        assertThat(action.sharedState).hasArray("userInfo/userNames/username").contains("userId");
        assertThat(action.sharedState).hasArray("userInfo/attributes/sun-fm-saml2-nameid-info");
        assertThat(action.sharedState).hasArray("userInfo/attributes/sun-fm-saml2-nameid-infokey");
    }

    @Test
    void shouldMapAttributesToSharedStateWhenAccountExists() throws Exception {
        setupSuccessfulFederation();
        given(responseUtils.mapSamlAttributes(any(), eq("sp-entity-id"), eq("idp-entity-id"), any(), any()))
                .willReturn(ImmutableMap.of("badger", singleton("weasel"), "otter", singleton("mink")));

        Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

        assertThat(action.sharedState).hasArray("userInfo/attributes/badger").contains("weasel");
        assertThat(action.sharedState).hasArray("userInfo/attributes/otter").contains("mink");
    }

    @Test
    void shouldMapAttributesToSharedStateWhenThereIsNoLocalAccount() throws Exception {
        setupSuccessfulFederation(ssoResult(null, false));
        given(responseUtils.mapSamlAttributes(any(), eq("sp-entity-id"), eq("idp-entity-id"), any(), any()))
                .willReturn(ImmutableMap.of("badger", singleton("weasel"), "otter", singleton("mink")));

        Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

        assertThat(action.outcome).isEqualTo(NO_ACCOUNT.name());
        assertThat(action.sharedState).hasArray("userInfo/attributes/badger").contains("weasel");
        assertThat(action.sharedState).hasArray("userInfo/attributes/otter").contains("mink");
    }

    @Test
    void shouldMapEmailAttributeToSharedState() throws Exception {
        setupSuccessfulFederation();
        given(responseUtils.mapSamlAttributes(any(), eq("sp-entity-id"), eq("idp-entity-id"), any(), any()))
                .willReturn(singletonMap("mail", singleton("test@example.com")));

        Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

        assertThat(action.sharedState).hasArray("userInfo/attributes/mail").contains("test@example.com");
        assertThat(action.sharedState).stringAt("emailAddress").contains("test@example.com");
    }

    @Test
    void shouldSetSessionProperties() throws Exception {
        setupSuccessfulFederation();
        given(responseUtils.mapSamlAttributes(any(), eq("sp-entity-id"), eq("idp-entity-id"), any(), any()))
                .willReturn(ImmutableMap.of("badger", singleton("weasel"), "otter", singleton("mink")));

        Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

        assertThat(action.sessionProperties)
                .containsEntry("SessionIndex", "session-index")
                .containsEntry("cacheKey", "storage-key")
                .containsEntry("isTransient", "false")
                .containsKey("NameID");
    }

    @Test
    void shouldSetIsTransientToTrueWhenNameIdIsTransient() throws Exception {
        setupSuccessfulFederation(ssoResult("universalId", true));
        given(responseUtils.mapSamlAttributes(any(), eq("sp-entity-id"), eq("idp-entity-id"), any(), any()))
                .willReturn(ImmutableMap.of("badger", singleton("weasel"), "otter", singleton("mink")));

        Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

        assertThat(action.sessionProperties)
                .containsEntry("isTransient", "true");
    }

    @Test
    void shouldNotSetNameIdAttributesWhenNameIdShouldNotPersisted() throws Exception {
        setupSuccessfulFederation(ssoResult("universalId", true));
        given(responseUtils.mapSamlAttributes(any(), eq("sp-entity-id"), eq("idp-entity-id"), any(), any()))
                .willReturn(ImmutableMap.of("badger", singleton("weasel"), "otter", singleton("mink")));

        Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

        assertThat(action.sharedState).hasObject("userInfo/attributes")
                .containsFields("badger", "otter")
                .doesNotContain("sun-fm-saml2-nameid-info", "sun-fm-saml2-nameid-infokey");
    }

    @Test
    void shouldReportNoAccountExistsWhenTheUniversalIDCanNotBeResolved() throws Exception {
        // Given
        setupSuccessfulFederation(ssoResult("universalId", true));
        given(identityService.doesIdentityExist("universalId")).willReturn(false);

        // When
        Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

        // Then
        assertThat(action.outcome).isEqualTo("NO_ACCOUNT");
    }

    @Test
    void shouldFailWhenStorageKeyNotFound() throws Exception {
        given(responseUtils.readSaml2ResponseData(any())).willThrow(SAML2TokenRepositoryException.class);

        assertThatThrownBy(() -> {
            Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

            verify(cookieUtils).addCookieToResponseForRequestDomains(servletRequest, servletResponse,
                    AM_LOCATION_COOKIE, "", 0);
            assertThat(action.callbacks).isEmpty();
            assertThat(action.outcome).isEqualTo(ACCOUNT_EXISTS.name());
            assertThat(action.sharedState).stringAt(USERNAME).isEqualTo("userId");
            assertThat(action.sharedState).hasArray("userInfo/userNames/username").contains("userId");
            assertThat(action.sharedState).hasArray("userInfo/attributes/sun-fm-saml2-nameid-info");
            assertThat(action.sharedState).hasArray("userInfo/attributes/sun-fm-saml2-nameid-infokey");
        })
                .isInstanceOf(NodeProcessException.class)
                .hasMessageContaining("response not found");
    }

    @Test
    void shouldSucceedOnFailureToRemoveStorageKey() throws Exception {
        setupSuccessfulFederation();
        doThrow(SAML2TokenRepositoryException.class).when(responseUtils).removeSaml2ResponseData(any());

        Action action = node.process(getContext(singletonMap(RESPONSE_KEY, new String[]{"storage-key"})));

        verify(cookieUtils).addCookieToResponseForRequestDomains(servletRequest, servletResponse, AM_LOCATION_COOKIE,
                "", 0);
        assertThat(action.callbacks).isEmpty();
        assertThat(action.outcome).isEqualTo(ACCOUNT_EXISTS.name());
        assertThat(action.sharedState).stringAt(USERNAME).isEqualTo("userId");
        assertThat(action.sharedState).hasArray("userInfo/userNames/username").contains("userId");
        assertThat(action.sharedState).hasArray("userInfo/attributes/sun-fm-saml2-nameid-info");
        assertThat(action.sharedState).hasArray("userInfo/attributes/sun-fm-saml2-nameid-infokey");
    }

    @Test
    void shouldReturnActionWithCallbackWhenRelayStateIsPresentAndValid() throws Exception {
        // Given
        setupSuccessfulFederation();
        given(servletRequest.getRequestURL()).willReturn(new StringBuffer("http://am.localtest.me:8080/openam"));

        Action action;
        try (MockedStatic<SAML2Utils> samlUtilsMockedStatic = mockStatic(SAML2Utils.class)) {
            // stop SAML2Utils.validateRelayStateURL from throwing an exception by mocking it

            // When
            action = node.process(getContext(Map.of(RESPONSE_KEY, new String[]{"storage-key"},
                    "RelayState", new String[]{"http://relay-state.com"})));
        }
        // Then
        assertThat(action.callbacks).isEmpty();
        assertThat(action.outcome).isEqualTo(ACCOUNT_EXISTS.name());
        assertThat(action.sharedState).stringAt("successUrl")
                .isEqualTo("http://relay-state.com");
    }

    @Test
    void shouldReturnActionWithCallbackWhenRelayStateIsPresentAndInvalidButSystemPropertyEnabled() throws Exception {
        // Given
        setupSuccessfulFederation();
        given(servletRequest.getRequestURL()).willReturn(new StringBuffer("http://am.localtest.me:8080/openam"));

        Action action;
        try (MockedStatic<SAML2Utils> samlUtilsMockedStatic = mockStatic(SAML2Utils.class)) {
            given(systemPropertiesWrapper.getAsBoolean(DISABLE_SAML2_RELAY_STATE_VALIDATION, false))
                    .willReturn(true);
            samlUtilsMockedStatic.when(() -> SAML2Utils.validateRelayStateURL(any(), any(), any(), any(), any()))
                    .thenThrow(new SAML2Exception("Invalid RelayState URL"));

            // When
            action = node.process(getContext(Map.of(RESPONSE_KEY, new String[]{"storage-key"},
                    "RelayState", new String[]{"http://relay-state.com"})));
        }
        // Then
        assertThat(action.callbacks).isEmpty();
        assertThat(action.outcome).isEqualTo(ACCOUNT_EXISTS.name());
        assertThat(action.sharedState).stringAt("successUrl")
                .isEqualTo("http://relay-state.com");
    }

    @Test
    void shouldReturnActionWithCallbackWhenRelayStateIsPresentButInvalid() throws Exception {
        // Given
        setupSuccessfulFederation();
        given(servletRequest.getRequestURL()).willReturn(new StringBuffer("http://am.localtest.me:8080/openam"));

        Action action;
        try (MockedStatic<SAML2Utils> samlUtilsMockedStatic = mockStatic(SAML2Utils.class)) {
            samlUtilsMockedStatic.when(() -> SAML2Utils.validateRelayStateURL(any(), any(), any(), any(), any()))
                    .thenThrow(new SAML2Exception("Invalid RelayState URL"));

            // When / Then
            assertThatThrownBy(() -> node.process(getContext(Map.of(RESPONSE_KEY, new String[]{"storage-key"},
                    "RelayState", new String[]{"http://relay-state.com"}))))
                    .isInstanceOf(NodeProcessException.class)
                    .hasMessage("Unable to complete SAML2 authentication, the RelayState is invalid");
        }

    }

    private Saml2SsoResult ssoResult(String universalId, boolean isTransient) throws Exception {
        return new Saml2SsoResult(universalId, getNameID(isTransient), emptySet(), !isTransient);
    }

    private void setupSuccessfulFederation() throws Exception {
        setupSuccessfulFederation(ssoResult("universalId", false));
    }

    private void setupSuccessfulFederation(Saml2SsoResult ssoResult) throws Exception {
        Saml2ResponseData responseData = new Saml2ResponseData();
        responseData.setSessionIndex("session-index");
        given(responseUtils.readSaml2ResponseData("storage-key")).willReturn(responseData);
        given(identityService.getIdentityName(ssoResult.getUniversalId())).willReturn("userId");
        given(identityService.doesIdentityExist("universalId")).willReturn(true);

        given(responseUtils.getSsoResultWithoutLocalLogin(any(), any(), any(), any(), any(), eq("storage-key")))
                .willReturn(ssoResult);
    }

    private void mockConfig() {
        given(config.allowCreate()).willReturn(true);
        given(config.authComparison()).willReturn(AuthComparison.MAXIMUM);
        given(config.authnContextClassRef()).willReturn(ImmutableList.of("class-ref-1", "class-ref-2"));
        given(config.authnContextDeclRef()).willReturn(ImmutableList.of("class-decl-1", "class-decl-2"));
        given(config.forceAuthn()).willReturn(true);
        given(config.isPassive()).willReturn(true);
        given(config.nameIdFormat()).willReturn("custom:name-id");
        given(config.requestBinding()).willReturn(RequestBinding.HTTP_POST);
        given(config.binding()).willReturn(Binding.HTTP_POST);
    }

    private NameID getNameID(boolean isTransient) throws Exception {
        NameID nameId = new NameIDImpl();
        nameId.setValue("name-id");
        if (isTransient) {
            nameId.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        }
        return nameId;
    }

    private TreeContext getContext(Map<String, String[]> parameters) {
        ExternalRequestContext request = new Builder()
                .parameters(parameters)
                .servletRequest(servletRequest)
                .servletResponse(servletResponse)
                .build();
        return new TreeContext(json(object()), request, emptyList(), Optional.empty());
    }
}
