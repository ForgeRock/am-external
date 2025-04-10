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

package com.sun.identity.xacml.context.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AttributeImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml-context:Attribute " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "AttributeId=\"urn:test\" DataType=\"urn:thing\" Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "<xacml-context:AttributeValue>b</xacml-context:AttributeValue>" +
                        "<xacml-context:AttributeValue>c</xacml-context:AttributeValue></xacml-context:Attribute>" },
                { true, false, "<xacml-context:Attribute AttributeId=\"urn:test\" DataType=\"urn:thing\" " +
                        "Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "<xacml-context:AttributeValue>b</xacml-context:AttributeValue>" +
                        "<xacml-context:AttributeValue>c</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute>" },
                { false, false, "<Attribute AttributeId=\"urn:test\" DataType=\"urn:thing\" Issuer=\"testIssuer\">\n" +
                        "<AttributeValue>a</AttributeValue>\n" +
                        "<AttributeValue>b</AttributeValue>\n" +
                        "<AttributeValue>c</AttributeValue>\n" +
                        "</Attribute>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        AttributeImpl attribute = new AttributeImpl();
        attribute.setAttributeId(URI.create("urn:test"));
        attribute.setAttributeStringValues(List.of("a", "b", "c"));
        attribute.setIssuer("testIssuer");
        attribute.setDataType(URI.create("urn:thing"));

        // When
        String xml = attribute.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
