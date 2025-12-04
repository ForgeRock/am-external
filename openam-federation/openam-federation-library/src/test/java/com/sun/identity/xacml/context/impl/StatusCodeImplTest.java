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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class StatusCodeImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml-context:StatusCode " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "Value=\"testValue\"><xacml-context:StatusCode " +
                        "Value=\"testMinorValue\"/></xacml-context:StatusCode>" },
                { true, false, "<xacml-context:StatusCode Value=\"testValue\">" +
                        "<xacml-context:StatusCode Value=\"testMinorValue\"/>" +
                        "</xacml-context:StatusCode>" },
                { false, false, "<StatusCode Value=\"testValue\"><StatusCode Value=\"testMinorValue\"/>" +
                        "</StatusCode>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("testValue");
        statusCode.setMinorCodeValue("testMinorValue");

        // When
        String xml = statusCode.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
