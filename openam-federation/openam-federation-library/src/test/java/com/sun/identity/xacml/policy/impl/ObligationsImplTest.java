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

package com.sun.identity.xacml.policy.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


public class ObligationsImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml:Obligations " +
                        "xmlns:xacml=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\" >" +
                        "<xacml:Obligation " +
                        "FulfillOn=\"test\" ObligationId=\"urn:test\"/></xacml:Obligations>" },
                { true, false, "<xacml:Obligations>" +
                        "<xacml:Obligation FulfillOn=\"test\" ObligationId=\"urn:test\"/></xacml:Obligations>" },
                { false, false, "<Obligations>" +
                        "<Obligation FulfillOn=\"test\" ObligationId=\"urn:test\"/></Obligations>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ObligationsImpl obligations = new ObligationsImpl();
        ObligationImpl obligation = new ObligationImpl();
        obligation.setObligationId(URI.create("urn:test"));
        obligation.setFulfillOn("test");
        obligations.setObligations(List.of(obligation));

        // When
        String xml = obligations.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}
