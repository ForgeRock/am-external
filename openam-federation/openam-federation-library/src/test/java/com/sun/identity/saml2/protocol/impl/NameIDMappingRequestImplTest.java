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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package com.sun.identity.saml2.protocol.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.impl.NameIDImpl;
import com.sun.identity.saml2.common.SAML2Constants;

public class NameIDMappingRequestImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:NameIDMappingRequest " +
                        "xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"5138-11-16T09:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:NameID>testName</saml:NameID>" +
                        "<samlp:NameIDPolicy AllowCreate=\"true\" " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\"/>" +
                        "</samlp:NameIDMappingRequest>" },
                { true, false, "<samlp:NameIDMappingRequest " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"5138-11-16T09:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:NameID>testName</saml:NameID>" +
                        "<samlp:NameIDPolicy AllowCreate=\"true\" " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\"/>" +
                        "</samlp:NameIDMappingRequest>" },
                { false, false, "<NameIDMappingRequest " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"5138-11-16T09:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<NameID>testName</NameID>" +
                        "<NameIDPolicy AllowCreate=\"true\" " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\"/>" +
                        "</NameIDMappingRequest>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        NameIDMappingRequestImpl mappingRequest = new NameIDMappingRequestImpl();
        mappingRequest.setID("testID");
        mappingRequest.setIssueInstant(new Date(100000000000000L));
        mappingRequest.setVersion("2.0");
        NameID nameID = new NameIDImpl();
        nameID.setValue("testName");
        mappingRequest.setNameID(nameID);
        NameIDPolicyImpl policy = new NameIDPolicyImpl();
        policy.setAllowCreate(true);
        policy.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        mappingRequest.setNameIDPolicy(policy);

        // When
        String xml = mappingRequest.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

}
