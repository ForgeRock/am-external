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
 * Copyright 2021-2025 Ping Identity Corporation.
 */

package com.sun.identity.xacml.context.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class EnvironmentImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml-context:Environment " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os " +
                        "http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd\">" +
                        "<xacml-context:Attribute DataType=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Environment>" },
                { true, false, "<xacml-context:Environment>" +
                        "<xacml-context:Attribute DataType=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Environment>" },
                { false, false, "<Environment>" +
                        "<Attribute DataType=\"urn:test\"><AttributeValue>a</AttributeValue></Attribute>" +
                        "</Environment>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        EnvironmentImpl environment = new EnvironmentImpl();
        AttributeImpl attribute = new AttributeImpl();
        attribute.setAttributeStringValues(List.of("a"));
        attribute.setDataType(URI.create("urn:test"));
        environment.setAttributes(List.of(attribute));

        // When
        String xml = environment.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
