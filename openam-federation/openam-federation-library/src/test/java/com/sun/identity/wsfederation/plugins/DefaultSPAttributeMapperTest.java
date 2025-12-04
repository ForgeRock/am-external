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
package com.sun.identity.wsfederation.plugins;

import static com.sun.identity.saml.common.SAMLUtilsCommon.makeEndElementTagXML;
import static com.sun.identity.saml.common.SAMLUtilsCommon.makeStartElementTagXML;
import static com.sun.identity.shared.xml.XMLUtils.escapeSpecialCharacters;
import static com.sun.identity.shared.xml.XMLUtils.toDOMDocument;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.guice.core.GuiceExtension;
import org.forgerock.openam.federation.testutils.TestCaseConfigurationInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.saml.assertion.Attribute;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.wsfederation.common.WSFederationConstants;

@ExtendWith(GuiceExtension.class)
public class DefaultSPAttributeMapperTest {

    private static final String TEST_COT = "TestCOT";
    private static final String EMAIL = "test@example.com";
    private static final String CN = "Test";
    private static final String SN = "User";

    private static CircleOfTrustManager cotManager;

    @BeforeAll
    static void init() throws Exception {
        cotManager = new CircleOfTrustManager();
    }

    @BeforeEach
    void setUp() throws Exception {
        TestCaseConfigurationInstance.resetConfiguration();
    }

    @Test
    void testAttributeMapping() throws Exception {

        final String[] expectedMappedAttributeNames = new String[]{ "upn", "cn", "sn" };
        final String[] expectedMappedAttributeValues = new String[]{ EMAIL, CN, SN };

        configureRealm("spAttributeMapping", "idp-extended-external.xml", "sp-extended-mapping-hosted.xml");
        DefaultSPAttributeMapper mapper = new DefaultSPAttributeMapper();
        List<Attribute> attributes = new ArrayList<>();
        // Mapping has email=upn so this attribute is expected to be mapped to upn after processing.
        attributes.add(makeAttribute("email", "test@example.com"));
        // These are not mapped and expected to be mapped as is after processing.
        attributes.add(makeAttribute("cn", "Test"));
        attributes.add(makeAttribute("sn", "User"));

        Map<String, Set<String>> attributeMap = mapper.getAttributes(attributes, "test",
                "openam-wsfed-sp", "external-wsfed-idp", "spAttributeMapping");

        assertThat(attributeMap).hasSize(expectedMappedAttributeNames.length);
        for (int i = 0; i < expectedMappedAttributeNames.length; i++) {
            assertThat(attributeMap.get(expectedMappedAttributeNames[i])).isNotNull();
            assertThat(attributeMap.get(expectedMappedAttributeNames[i]))
                    .containsExactly(expectedMappedAttributeValues[i]);
        }
    }

    private void configureRealm(String realm, String idpExtended, String spExtended) throws Exception {
        // This just keeps the error logging noise down by having a valid COT setup, set a realm to foil the COT cache.
        cotManager.createCircleOfTrust(realm, new CircleOfTrustDescriptor(TEST_COT, realm, COTConstants.ACTIVE));

        TestCaseConfigurationInstance.configureWsFed(realm, "/wsfedmetadata/idp-external.xml",
                "/wsfedmetadata/" + idpExtended);
        TestCaseConfigurationInstance.configureWsFed(realm, "/wsfedmetadata/sp.xml",
                "/wsfedmetadata/" + spExtended);
    }

    private Attribute makeAttribute(String name, String value) throws SAMLException {
        return new Attribute(name, WSFederationConstants.CLAIMS_URI,
                singletonList(toDOMDocument(makeStartElementTagXML("AttributeValue", true, true)
                        + escapeSpecialCharacters(value)
                        + makeEndElementTagXML("AttributeValue", true)).getDocumentElement()));
    }
}
