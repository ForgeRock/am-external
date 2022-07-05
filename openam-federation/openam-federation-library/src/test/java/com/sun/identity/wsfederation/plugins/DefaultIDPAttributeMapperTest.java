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
 * Copyright 2017-2021 ForgeRock AS.
 */
package com.sun.identity.wsfederation.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asList;
import static org.forgerock.openam.utils.CollectionUtils.asSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance;
import org.forgerock.openam.federation.testutils.TestCaseDataStoreProvider;
import org.forgerock.openam.federation.testutils.TestCaseSessionProvider;
import org.forgerock.util.encode.Base64;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.shared.xml.XMLUtils;

public class DefaultIDPAttributeMapperTest {

    private CircleOfTrustManager cotManager;

    private static final String TEST_SESSION_ID = "TestSSOTokenId";
    private static final String TEST_USER_ID = "TestUser";
    private static final String TEST_COT = "TestCOT";

    // User attribute values
    private static final String EMAIL = "test@example.com";
    private static final String SN = "User";
    private static final String CN = "Test";
    private static final byte[] CERT = "SomeRandomBytesRepresentingAUserCert".getBytes();
    private static final String CERT_ENCODED = Base64.encode(CERT);

    // User session property values
    private static final String SESSION1 = "value1";
    private static final String SESSION2 = "value2";

    @Mock
    private SSOToken ssoToken;

    @BeforeClass
    public void init() throws Exception {
        // Setup the session properties to be used in across all test cases.
        Map<String, List<String>> sessionProperties = new HashMap<>(2);
        sessionProperties.put("sessionProp1", asList(SESSION1));
        sessionProperties.put("sessionProp2", asList(SESSION2));
        TestCaseSessionProvider.setState(ssoToken, TEST_SESSION_ID, TEST_USER_ID, sessionProperties);

        // Setup the user attributes to be used in across all test cases.
        Map<String, Set<String>> userAttributeValues = new HashMap<>(3);
        userAttributeValues.put("mail", asSet(EMAIL));
        userAttributeValues.put("sn", asSet(SN));
        userAttributeValues.put("cn", asSet(CN));
        Map<String, Map<String, Set<String>>> userAttributes = new HashMap<>(1);
        userAttributes.put(TEST_USER_ID, userAttributeValues);

        // Setup the binary user attributes to be used in across all test cases.
        Map<String, byte[][]> userAttributeBinaryValues = new HashMap<>(1);
        byte[][] values = new byte[1][];
        values[0] = CERT;
        userAttributeBinaryValues.put("certificate", values);
        Map<String, Map<String, byte[][]>> userAttributesBinary = new HashMap<>(1);
        userAttributesBinary.put(TEST_USER_ID, userAttributeBinaryValues);

        TestCaseDataStoreProvider.resetAttributes(userAttributes, userAttributesBinary);

        cotManager = new CircleOfTrustManager();
    }

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        TestCaseConfigurationInstance.resetConfiguration();
    }

    @DataProvider(name = "mappingData")
    private Object[][] mappingData() {
        return new Object[][]{
                { "testIDPAttributeMapping", "idp-extended.xml", "sp-extended.xml",
                        new Object[]{ "EmailAddress", "LastName", "UserCert" },
                        new Object[]{EMAIL, SN, CERT_ENCODED} },
                { "testIdpAttributeMappingSessionProps", "idp-extended-sessionprops.xml", "sp-extended.xml",
                        new Object[]{ "EmailAddress", "LastName", "Property1", "Property2" },
                        new Object[]{EMAIL, SN, SESSION1, SESSION2} },
                { "testSPOverridesIDPAttributeMapping", "idp-extended.xml", "sp-extended-mapping.xml",
                        new Object[]{ "FirstName", "Property1", "Property2" },
                        new Object[]{CN, SESSION1, SESSION2} }
        };
    }

    @Test(dataProvider = "mappingData")
    public void testAttributeMapping(String realm,
                                     String idpExtended,
                                     String spExtended,
                                     Object[] expectedAttributeNames,
                                     Object[] expectedAttributeValues) throws Exception {

        configureRealm(realm, idpExtended, spExtended);
        DefaultIDPAttributeMapper mapper = new DefaultIDPAttributeMapper();
        // Set a realm to foil the metadata cache.
        List<Attribute> attributes = mapper.getAttributes(ssoToken, "openam-wsfed-idp", "external-wsfed-sp", realm);


        assertThat(attributes).hasSize(expectedAttributeNames.length);
        // Attribute does not provide an equals method so can't easily compare to expected set of Attribute values.
        for (Attribute attribute : attributes) {
            assertThat(attribute.getAttributeName()).isIn(expectedAttributeNames);
            assertThat(getFirstAttributeValue(attribute)).isIn(expectedAttributeValues);
        }
    }

    private void configureRealm(String realm, String idpExtended, String spExtended) throws Exception {
        // This just keeps the error logging noise down by having a valid COT setup, set a realm to foil the COT cache.
        cotManager.createCircleOfTrust(realm, new CircleOfTrustDescriptor(TEST_COT, realm, COTConstants.ACTIVE));

        TestCaseConfigurationInstance.configureWsFed(realm, "/wsfedmetadata/idp.xml",
                "/wsfedmetadata/" + idpExtended);
        TestCaseConfigurationInstance.configureWsFed(realm, "/wsfedmetadata/sp-external.xml",
                "/wsfedmetadata/" + spExtended);
    }

    private String getFirstAttributeValue(Attribute attribute) throws SAMLException {
        return XMLUtils.getElementValue(attribute.getAttributeValue().get(0));
    }
}