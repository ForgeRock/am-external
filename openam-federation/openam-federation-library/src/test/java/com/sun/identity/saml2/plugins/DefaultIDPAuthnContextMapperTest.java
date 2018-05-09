/*
 * Copyright 2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */
package com.sun.identity.saml2.plugins;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.protocol.impl.AuthnRequestImpl;
import com.sun.identity.saml2.protocol.impl.RequestedAuthnContextImpl;


public class DefaultIDPAuthnContextMapperTest {

    private CircleOfTrustManager cotManager;
    private static String TEST_COT = "TestCOT";
    private List<String> authCtxList = new ArrayList<>();


    @BeforeClass
    public void init() throws Exception {
        cotManager = new CircleOfTrustManager();
        authCtxList.add(SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        TestCaseConfigurationInstance.resetConfiguration();
    }

    @DataProvider(name = "comparisonData")
    private Object[][] comparisonData() {
        return new Object[][]{
                // Exact match - default requested is PasswordProtected (level 5), so expecting this
                {"exact", "testrealmexact", 5, SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT},
                // Minimum match, not lower than requested (default of PasswordProtected),
                // so expecting PasswordProtected as that matches what was requested
                {"minimum", "testrealmmin", 5, SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT},
                // Maximum level not more than requested (default of PasswordProtected), so expecting 5
                {"maximum", "testrealmmax", 5, SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT},
                // Better than match. Default of PasswordProtected requested, so better than is Kerberos, level 10
                {"better", "testrealmbetter", 10, "urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos"}
        };
    }

    @Test(dataProvider = "comparisonData")
    public void testAuthnComparison(String comparison,
                                    String realmName,
                                    int expectedAuthnLevel,
                                    String expectedAuthnClassRef) throws Exception {

        RequestedAuthnContext authnContext = new RequestedAuthnContextImpl();
        authnContext.setComparison(comparison);
        authnContext.setAuthnContextClassRef(authCtxList);
        AuthnRequest authnRequest = new AuthnRequestImpl();
        authnRequest.setRequestedAuthnContext(authnContext);

        configureRealm(realmName);
        DefaultIDPAuthnContextMapper mapper = new DefaultIDPAuthnContextMapper();
        IDPAuthnContextInfo info = mapper.getIDPAuthnContextInfo(authnRequest, "openam-saml2-idp", realmName);
        assertThat(info).isNotNull();
        assertThat(info.getAuthnContext()).isNotNull();
        assertThat(info.getAuthnContext().getAuthnContextClassRef()).isEqualTo(expectedAuthnClassRef);
        assertThat(info.getAuthnLevel()).isEqualTo(expectedAuthnLevel);
    }

    private void configureRealm(String realm) throws Exception {
        // This just keeps the error logging noise down by having a valid COT setup, set a realm to foil the COT cache.
        cotManager.createCircleOfTrust(realm, new CircleOfTrustDescriptor(TEST_COT, realm, COTConstants.ACTIVE));
        // Load IDP metadata
        TestCaseConfigurationInstance.configureSaml2(realm, "/saml2/idp.xml", "/saml2/idp-extended.xml");
    }
}
