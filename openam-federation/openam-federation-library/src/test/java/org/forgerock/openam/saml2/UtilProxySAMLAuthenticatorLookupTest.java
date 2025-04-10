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
package org.forgerock.openam.saml2;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sun.identity.saml2.common.SOAPCommunicator;

import org.forgerock.am.trees.api.TreeProvider;
import org.forgerock.guice.core.GuiceExtension;
import org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance;
import org.forgerock.openam.federation.testutils.TestCaseSessionProvider;
import org.forgerock.openam.jwt.JwtEncryptionOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;

import com.google.common.collect.ImmutableMap;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.profile.ClientFaultException;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UtilProxySAMLAuthenticatorLookupTest {

    @RegisterExtension
    GuiceExtension guiceExtension = new GuiceExtension.Builder()
            .addInstanceBinding(SOAPCommunicator.class, mock(SOAPCommunicator.class))
            .addInstanceBinding(TreeProvider.class, mock(TreeProvider.class)).build();

    private static final String REALM = "/";
    private static final String TEST_COT = "TestCOT";
    private static IDPSSOFederateRequest data;
    @Mock
    private static HttpServletRequest request;
    @Mock
    private static HttpServletResponse response;
    @Mock
    private static PrintWriter out;
    @Mock
    private static SSOToken ssoToken;
    private static UtilProxySAMLAuthenticatorLookup lookup;

    @BeforeEach
    void setup() {
        data = new IDPSSOFederateRequest("test-id", REALM, null, "test-metaAlias", "openam-saml2-idp");
        data.setSession(ssoToken);
        lookup = new UtilProxySAMLAuthenticatorLookup(data, request, response, out, mock(JwtEncryptionOptions.class));

        TestCaseConfigurationInstance.resetConfiguration();
    }

    @AfterEach
    void teardown() {
        TestCaseConfigurationInstance.resetConfiguration();
    }

    @Test
    void shouldFindSessionInvalidIfTheSessionHasInsufficientAuthLevel() throws Exception {
        // Given
        new CircleOfTrustManager().createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(TEST_COT, REALM, COTConstants.ACTIVE));
        TestCaseConfigurationInstance.configureSaml2(REALM, "/saml2/idp.xml", "/saml2/idp-extended.xml");
        TestCaseSessionProvider.setState(ssoToken, "test-session-id", "test-user", getSessionProperties(REALM, "0"));

        // When
        assertThatThrownBy(() -> lookup.isSessionValid(SessionManager.getProvider()))
                .isInstanceOf(ClientFaultException.class);
    }

    @Test
    void shouldFindSessionValidIfTheSessionWasUpgradedCorrectly() throws Exception {
        // Given
        new CircleOfTrustManager().createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(TEST_COT, REALM, COTConstants.ACTIVE));
        TestCaseConfigurationInstance.configureSaml2(REALM, "/saml2/idp.xml", "/saml2/idp-extended.xml");
        TestCaseSessionProvider.setState(ssoToken, "test-session-id", "test-user", getSessionProperties(REALM, "5"));

        // When
        final boolean sessionValid = lookup.isSessionValid(SessionManager.getProvider());

        // Then
        assertThat(sessionValid).isTrue();
    }

    @Test
    void shouldFindSessionInvalidIfTheSessionBelongsToDifferentRealm() throws Exception {
        // Given
        given(request.getRemoteAddr()).willReturn("");
        new CircleOfTrustManager().createCircleOfTrust(REALM,
                new CircleOfTrustDescriptor(TEST_COT, REALM, COTConstants.ACTIVE));
        TestCaseConfigurationInstance.configureSaml2(REALM, "/saml2/idp.xml", "/saml2/idp-extended.xml");
        TestCaseSessionProvider.setState(ssoToken, "test-session-id", "test-user", getSessionProperties("/foo", "0"));

        // When
        assertThatThrownBy(() -> lookup.isSessionValid(SessionManager.getProvider()))
                .isInstanceOf(ClientFaultException.class);
    }

    private static ImmutableMap<String, List<String>> getSessionProperties(String realm, String authLevel) {
        return ImmutableMap.<String, List<String>>builder()
                .put(SAML2Constants.ORGANIZATION, singletonList(realm))
                .put(SAML2Constants.AUTH_LEVEL, singletonList(authLevel))
                .build();
    }
}
