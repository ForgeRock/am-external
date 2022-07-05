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
 * Copyright 2021 ForgeRock AS.
 */

package com.sun.identity.xacml.context.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.xacml.context.Decision;
import com.sun.identity.xacml.context.StatusCode;
import com.sun.identity.xacml.context.StatusDetail;
import com.sun.identity.xacml.context.StatusMessage;
import com.sun.identity.xacml.policy.Obligation;
import com.sun.identity.xacml.policy.Obligations;
import com.sun.identity.xacml.policy.impl.ObligationImpl;
import com.sun.identity.xacml.policy.impl.ObligationsImpl;

public class ResponseImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml-context:Response " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">" +
                        "<xacml-context:Result ResourceId=\"testResource\">" +
                        "<xacml-context:Decision>Permit</xacml-context:Decision>" +
                        "<xacml-context:Status><xacml-context:StatusCode Value=\"testValue\">" +
                        "<xacml-context:StatusCode Value=\"testMinorValue\"/></xacml-context:StatusCode>" +
                        "<xacml-context:StatusMessage>testMessage</xacml-context:StatusMessage>" +
                        "<xacml-context:StatusDetail/></xacml-context:Status>" +
                        "<xacml:Obligations xmlns:xacml=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\">" +
                        "<xacml:Obligation FulfillOn=\"test\" ObligationId=\"urn:test\"/>" +
                        "</xacml:Obligations></xacml-context:Result></xacml-context:Response>" },
                { true, false, "<xacml-context:Response>" +
                        "<xacml-context:Result ResourceId=\"testResource\">" +
                        "<xacml-context:Decision>Permit</xacml-context:Decision>" +
                        "<xacml-context:Status><xacml-context:StatusCode Value=\"testValue\">" +
                        "<xacml-context:StatusCode Value=\"testMinorValue\"/></xacml-context:StatusCode>" +
                        "<xacml-context:StatusMessage>testMessage</xacml-context:StatusMessage>" +
                        "<xacml-context:StatusDetail/></xacml-context:Status>" +
                        "<xacml:Obligations xmlns:xacml=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\">" +
                        "<xacml:Obligation FulfillOn=\"test\" ObligationId=\"urn:test\"/>" +
                        "</xacml:Obligations></xacml-context:Result></xacml-context:Response>" },
                { false, false, "<Response>" +
                        "<Result ResourceId=\"testResource\">" +
                        "<Decision>Permit</Decision>" +
                        "<Status><StatusCode Value=\"testValue\">" +
                        "<StatusCode Value=\"testMinorValue\"/></StatusCode>" +
                        "<StatusMessage>testMessage</StatusMessage>" +
                        "<StatusDetail/></Status>" +
                        "<Obligations>" +
                        "<Obligation FulfillOn=\"test\" ObligationId=\"urn:test\"/>" +
                        "</Obligations></Result></Response>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        ResponseImpl response = new ResponseImpl();
        ResultImpl result = new ResultImpl();
        Decision decision = new DecisionImpl();
        decision.setValue("Permit");
        result.setDecision(decision);
        result.setResourceId("testResource");
        Obligations obligations = new ObligationsImpl();
        Obligation obligation = new ObligationImpl();
        obligation.setObligationId(URI.create("urn:test"));
        obligation.setFulfillOn("test");
        obligations.setObligations(List.of(obligation));
        result.setObligations(obligations);
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
        result.setStatus(status);
        response.setResults(List.of(result));

        // When
        String xml = response.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}