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


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.xacml.context.StatusCode;
import com.sun.identity.xacml.context.StatusDetail;
import com.sun.identity.xacml.context.StatusMessage;

public class StatusImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml-context:Status " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">" +
                        "<xacml-context:StatusCode Value=\"testValue\">" +
                        "<xacml-context:StatusCode Value=\"testMinorValue\"/>" +
                        "</xacml-context:StatusCode>" +
                        "<xacml-context:StatusMessage>testMessage</xacml-context:StatusMessage>" +
                        "<xacml-context:StatusDetail/>" +
                        "</xacml-context:Status>" },
                { true, false, "<xacml-context:Status>" +
                        "<xacml-context:StatusCode Value=\"testValue\">" +
                        "<xacml-context:StatusCode Value=\"testMinorValue\"/>" +
                        "</xacml-context:StatusCode>" +
                        "<xacml-context:StatusMessage>testMessage</xacml-context:StatusMessage>" +
                        "<xacml-context:StatusDetail/>" +
                        "</xacml-context:Status>" },
                { false, false, "<Status>" +
                        "<StatusCode Value=\"testValue\">" +
                        "<StatusCode Value=\"testMinorValue\"/>" +
                        "</StatusCode>" +
                        "<StatusMessage>testMessage</StatusMessage>" +
                        "<StatusDetail/>" +
                        "</Status>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        StatusImpl status = new StatusImpl();
        StatusCode statusCode = new StatusCodeImpl();
        statusCode.setValue("testValue");
        statusCode.setMinorCodeValue("testMinorValue");
        status.setStatusCode(statusCode);

        StatusDetail statusDetail = new StatusDetailImpl();
        status.setStatusDetail(statusDetail);

        StatusMessage statusMessage = new StatusMessageImpl();
        statusMessage.setValue("testMessage");
        status.setStatusMessage(statusMessage);

        // When
        String xml = status.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

}
