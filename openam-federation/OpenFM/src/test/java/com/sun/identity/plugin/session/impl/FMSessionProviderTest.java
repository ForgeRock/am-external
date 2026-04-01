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
 * Copyright 2026 Ping Identity Corporation.
 */
package com.sun.identity.plugin.session.impl;

import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.feature.Features;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iplanet.sso.SSOToken;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.common.SAMLConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FMSessionProviderTest {

    private static final String AUTH_URI = "http://schemas.example.com/claims/multipleauthn";

    private static final String BUG_AUTH_URI = "urn:com:sun:identity:" + AUTH_URI;

    @Mock
    SSOToken ssoToken;
    @Mock
    FMSessionNotification sessionNotification;
    @Mock
    AuthUtilsWrapper authUtilsWrapper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Features features;

    @BeforeEach
    void setUp() {
        when(features.wsFedAuthMethodUriPrefixCheck().isEnabled()).thenReturn(true);
    }

    @Test
    void returnsAuthMethodUnchangedWhenValueIsUri() throws Exception {
        when(ssoToken.getProperty("AuthType")).thenReturn(AUTH_URI);

        String[] result = newProvider().getProperty(ssoToken, SessionProvider.AUTH_METHOD);

        assertThat(result).containsExactly(AUTH_URI);
    }

    @Test
    void noChangeInBehaviourWhenFeatureDisabled() throws Exception {
        when(features.wsFedAuthMethodUriPrefixCheck().isEnabled()).thenReturn(false);
        when(ssoToken.getProperty("AuthType")).thenReturn(AUTH_URI);

        String[] result = newProvider().getProperty(ssoToken, SessionProvider.AUTH_METHOD);

        assertThat(result).containsExactly(BUG_AUTH_URI);
    }

    @Test
    void prefixesNonUriAuthTypeWithSunIdentityNamespace() throws Exception {
        when(ssoToken.getProperty("AuthType")).thenReturn("customAuth");

        String[] result = newProvider().getProperty(ssoToken, SessionProvider.AUTH_METHOD);

        assertThat(result).containsExactly(SAMLConstants.AUTH_METHOD_URI_PREFIX + "customAuth");
    }

    @Test
    void mapsCertToStandardUri() throws Exception {
        when(ssoToken.getProperty("AuthType")).thenReturn(SAMLConstants.AUTH_METHOD_CERT);

        String[] result = newProvider().getProperty(ssoToken, SessionProvider.AUTH_METHOD);

        assertThat(result).containsExactly(SAMLConstants.AUTH_METHOD_CERT_URI);
    }

    @Test
    void leavesCustomSchemeUriUntouched() throws Exception {
        when(ssoToken.getProperty("AuthType")).thenReturn("custom+foo:bar");

        String[] result = newProvider().getProperty(ssoToken, SessionProvider.AUTH_METHOD);

        assertThat(result).containsExactly("custom+foo:bar");
    }

    private FMSessionProvider newProvider() {
        return new FMSessionProvider(sessionNotification, authUtilsWrapper, features);
    }
}
