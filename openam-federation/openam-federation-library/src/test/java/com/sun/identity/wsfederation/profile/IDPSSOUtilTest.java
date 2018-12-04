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
 * Copyright 2018 ForgeRock AS.
 */

package com.sun.identity.wsfederation.profile;

import static com.sun.identity.cot.COTConstants.ACTIVE;
import static com.sun.identity.wsfederation.profile.IDPSSOUtil.getAuthenticationServiceURL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance.configureWsFed;
import static org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance.resetConfiguration;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;

public class IDPSSOUtilTest {

    private static final String TEST_COT = "TestCOT";
    private CircleOfTrustManager cotManager;
    private HttpServletRequest request;

    @BeforeClass
    public void init() throws Exception {
        cotManager = new CircleOfTrustManager();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/sso/wsfeduri ");
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("am.example.com");
        when(request.getServerPort()).thenReturn(443);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        resetConfiguration();
    }

    @DataProvider(name = "mappingData")
    private Object[][] mappingData() {
        return new Object[][]{
                { "testIdpWithNoAuthUrl",
                  "idp-extended.xml",
                  "https://am.example.com:443/sso/UI/Login?realm=testIdpWithNoAuthUrl" },
                { "testIdpWithEmptyAuthUrl",
                  "idp-extended-empty-authurl.xml",
                  "https://am.example.com:443/sso/UI/Login?realm=testIdpWithEmptyAuthUrl" },
                { "testIdpWithAuthUrl",
                  "idp-extended-with-authurl.xml",
                  "https://custom.example.com:443/authurl" }

        };
    }

    @Test(dataProvider = "mappingData")
    public void testGetAuthenticationServiceURL(String realm,
                                                String idpExtended,
                                                String expectedLoginUrl) throws Exception {
        // This just keeps the error logging noise down by having a valid COT setup, set a realm to foil the COT cache.
        cotManager.createCircleOfTrust(realm, new CircleOfTrustDescriptor(TEST_COT, realm, ACTIVE));
        configureWsFed(realm, "/wsfedmetadata/idp.xml", "/wsfedmetadata/" + idpExtended);

        assertThat(getAuthenticationServiceURL(realm, "openam-wsfed-idp", request)).isEqualTo(expectedLoginUrl);
    }
}
