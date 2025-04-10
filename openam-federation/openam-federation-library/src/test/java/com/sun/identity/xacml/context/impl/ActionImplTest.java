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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ActionImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml-context:Action " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os " +
                        "http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd\">" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "<xacml-context:AttributeValue>b</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Action>" },
                { true, false, "<xacml-context:Action>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "<xacml-context:AttributeValue>b</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute>" +
                        "</xacml-context:Action>" },
                { false, false, "<Action>" +
                        "<Attribute Issuer=\"testIssuer\">" +
                        "<AttributeValue>a</AttributeValue>" +
                        "<AttributeValue>b</AttributeValue>" +
                        "</Attribute></Action>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ActionImpl action = new ActionImpl();
        AttributeImpl attribute = new AttributeImpl();
        attribute.setIssuer("testIssuer");
        attribute.setAttributeStringValues(List.of("a", "b"));
        action.setAttributes(List.of(attribute));

        // When
        String xml = action.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
