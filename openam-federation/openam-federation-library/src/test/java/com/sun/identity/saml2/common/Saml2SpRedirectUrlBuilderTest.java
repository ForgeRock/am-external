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
 * Copyright 2025 Ping Identity Corporation.
 */

package com.sun.identity.saml2.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.forgerock.http.util.Uris;
import org.forgerock.openam.feature.Feature;
import org.forgerock.openam.feature.Features;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.profile.SPACSUtils;
import com.sun.identity.saml2.protocol.impl.ResponseImpl;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class Saml2SpRedirectUrlBuilderTest {

    private final String realmName = "testRealm";
    private final String hostEntityId = "testEntity";
    private final String relayState = "relay123";
    private final String treeName = "testTree";
    private final String responseId = "responseId123";
    private final String requestURL = "http://localhost:8080/am/Consumer/metaAlias/test";
    private final String localAuthUrl = "http://localhost:8080/UI/Login?realm=testRealm";

    @Mock
    private HttpServletRequest request;
    @Mock
    private SAML2MetaManager metaManager;
    @Mock
    private ResponseInfo respInfo;
    @InjectMocks
    private Saml2SpRedirectUrlBuilder saml2SpRedirectUrlBuilder;
    private MockedStatic<SPACSUtils> spacsUtilsMockedStatic;

    @BeforeEach
    void setup() {
        spacsUtilsMockedStatic = Mockito.mockStatic(SPACSUtils.class);
    }

    @AfterEach
    void clearMocks() {
        if (spacsUtilsMockedStatic != null) {
            spacsUtilsMockedStatic.close();
        }
    }

    void setupMocks(String treeNameConfig, String localAuthConfigUrl) {
        given(SPACSUtils.getConfiguredServiceForHostedSP(realmName, hostEntityId, metaManager,
                SAML2Constants.REDIRECT_TREE_NAME)).willReturn(treeNameConfig);
        given(SPACSUtils.getAttributeValueFromSPSSOConfig(realmName, hostEntityId, metaManager,
                SAML2Constants.LOCAL_AUTH_URL)).willReturn(localAuthConfigUrl);
        given(SPACSUtils.prepareForLocalLogin(realmName, hostEntityId, metaManager, respInfo, requestURL)).willReturn(
                localAuthUrl);

    }

    @Test
    void shouldReturnLocalAuthUrlGivenTreeNotConfiguredAndLocalAuthUrlConfigured() throws Exception {
        // given
        setupMocks(null, localAuthUrl);
        given(request.getRequestURL()).willReturn(new StringBuffer(requestURL));
        given(respInfo.getResponse()).willReturn(getResponse());

        // when
        String redirectUrl =
                saml2SpRedirectUrlBuilder.getRedirectUrl(realmName, hostEntityId, metaManager, respInfo, request, relayState);

        // then
        assertThat(redirectUrl).isEqualTo(getReturnLocalLoginUrl());
    }

    @Test
    void shouldReturnGeneratedLocalAuthUrlGivenTreeAndLocalAuthUrlNotConfigured() throws Exception {
        // given
        setupMocks(null, null);
        given(request.getRequestURL()).willReturn(new StringBuffer(requestURL));
        given(respInfo.getResponse()).willReturn(getResponse());

        // when
        String redirectUrl =
                saml2SpRedirectUrlBuilder.getRedirectUrl(realmName, hostEntityId, metaManager, respInfo, request, relayState);

        // then
        assertThat(redirectUrl).isEqualTo(getReturnLocalLoginUrl());
    }


    @Test
    void shouldReturnLocalAuthUrlGivenTreeAndLocalAuthUrlConfigured() throws Exception {
        // given
        setupMocks(treeName, localAuthUrl);
        given(request.getRequestURL()).willReturn(new StringBuffer(requestURL));
        given(respInfo.getResponse()).willReturn(getResponse());

        // when
        String redirectUrl =
                saml2SpRedirectUrlBuilder.getRedirectUrl(realmName, hostEntityId, metaManager, respInfo, request, relayState);

        // then
        assertThat(redirectUrl).isEqualTo(getReturnLocalLoginUrl());
    }

    @Test
    void shouldReturnTreeGivenTreeConfiguredAndLocalAuthUrlNotConfigured() throws Exception {
        // given
        setupMocks(treeName, null);
        setUpRequest();
        given(respInfo.getResponse()).willReturn(getResponse());

        // when
        String redirectUrl =
                saml2SpRedirectUrlBuilder.getRedirectUrl(realmName, hostEntityId, metaManager, respInfo, request, relayState);

        // then
        assertThat(redirectUrl).isEqualTo(getHostedSpTreeUrl());

    }

    private String getReturnLocalLoginUrl() {
        String expectedGoto =
                requestURL + "?resID=" + Uris.urlEncodeQueryParameterNameOrValue(responseId) + "&RelayState=" +
                        Uris.urlEncodeQueryParameterNameOrValue(relayState);
        return localAuthUrl + "&goto=" + Uris.urlEncodeQueryParameterNameOrValue(expectedGoto);
    }

    private String getHostedSpTreeUrl() {
        String expectedGoto =
                requestURL + "?resID=" + Uris.urlEncodeQueryParameterNameOrValue(responseId) + "&RelayState=" +
                        Uris.urlEncodeQueryParameterNameOrValue(relayState);
        String expectedSpRedirectTreeUrl = "/am?realm=testRealm&authIndexType=service&authIndexValue=" + treeName;
        return expectedSpRedirectTreeUrl + "&goto=" + Uris.urlEncodeQueryParameterNameOrValue(expectedGoto);
    }


    private void setUpRequest() {
        given(request.getContextPath()).willReturn("/am");
        given(request.getRequestURL()).willReturn(new StringBuffer(requestURL));
    }

    private ResponseImpl getResponse() throws SAML2Exception {
        ResponseImpl response = new ResponseImpl();
        response.setID(responseId);
        return response;
    }
}
