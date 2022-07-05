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

package com.sun.identity.xacml.saml2.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.context.Decision;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.xacml.context.StatusCode;
import com.sun.identity.xacml.context.StatusDetail;
import com.sun.identity.xacml.context.StatusMessage;
import com.sun.identity.xacml.context.impl.ActionImpl;
import com.sun.identity.xacml.context.impl.AttributeImpl;
import com.sun.identity.xacml.context.impl.DecisionImpl;
import com.sun.identity.xacml.context.impl.EnvironmentImpl;
import com.sun.identity.xacml.context.impl.RequestImpl;
import com.sun.identity.xacml.context.impl.ResourceImpl;
import com.sun.identity.xacml.context.impl.ResponseImpl;
import com.sun.identity.xacml.context.impl.ResultImpl;
import com.sun.identity.xacml.context.impl.StatusCodeImpl;
import com.sun.identity.xacml.context.impl.StatusDetailImpl;
import com.sun.identity.xacml.context.impl.StatusImpl;
import com.sun.identity.xacml.context.impl.StatusMessageImpl;
import com.sun.identity.xacml.context.impl.SubjectImpl;
import com.sun.identity.xacml.policy.Obligation;
import com.sun.identity.xacml.policy.Obligations;
import com.sun.identity.xacml.policy.impl.ObligationImpl;
import com.sun.identity.xacml.policy.impl.ObligationsImpl;

public class XACMLAuthzDecisionStatementImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:Statement " +
                        "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xacml-saml=\"urn:oasis:names:tc:xacml:2.0:saml:assertion:schema:os\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xacml-saml:XACMLAuthzDecisionStatement\">" +
                        "<xacml-context:Response " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">" +
                        "<xacml-context:Result ResourceId=\"testResource\">" +
                        "<xacml-context:Decision>Permit</xacml-context:Decision>" +
                        "<xacml-context:Status>" +
                        "<xacml-context:StatusCode Value=\"testValue\">" +
                        "<xacml-context:StatusCode Value=\"testMinorValue\"/></xacml-context:StatusCode>" +
                        "<xacml-context:StatusMessage>testMessage</xacml-context:StatusMessage>" +
                        "<xacml-context:StatusDetail/></xacml-context:Status>" +
                        "<xacml:Obligations xmlns:xacml=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\">" +
                        "<xacml:Obligation FulfillOn=\"test\" ObligationId=\"urn:test\"/></xacml:Obligations>" +
                        "</xacml-context:Result></xacml-context:Response>" +
                        "<xacml-context:Request xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os " +
                        "http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd\">" +
                        "<xacml-context:Subject SubjectCategory=\"urn:testcategory\">" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute>" +
                        "</xacml-context:Subject>" +
                        "<xacml-context:Resource>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute>" +
                        "<xacml-context:ResourceContent>test content</xacml-context:ResourceContent>" +
                        "</xacml-context:Resource>" +
                        "<xacml-context:Action>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute>" +
                        "</xacml-context:Action>" +
                        "<xacml-context:Environment>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Environment>" +
                        "</xacml-context:Request></saml:Statement>" },
                { true, false, "<saml:Statement " +
                        "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xacml-saml=\"urn:oasis:names:tc:xacml:2.0:saml:assertion:schema:os\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xacml-saml:XACMLAuthzDecisionStatement\">" +
                        "<xacml-context:Response " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">" +
                        "<xacml-context:Result ResourceId=\"testResource\">" +
                        "<xacml-context:Decision>Permit</xacml-context:Decision>" +
                        "<xacml-context:Status>" +
                        "<xacml-context:StatusCode Value=\"testValue\">" +
                        "<xacml-context:StatusCode Value=\"testMinorValue\"/></xacml-context:StatusCode>" +
                        "<xacml-context:StatusMessage>testMessage</xacml-context:StatusMessage>" +
                        "<xacml-context:StatusDetail/></xacml-context:Status>" +
                        "<xacml:Obligations xmlns:xacml=\"urn:oasis:names:tc:xacml:2.0:policy:schema:os\">" +
                        "<xacml:Obligation FulfillOn=\"test\" ObligationId=\"urn:test\"/></xacml:Obligations>" +
                        "</xacml-context:Result></xacml-context:Response>" +
                        "<xacml-context:Request xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os " +
                        "http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd\">" +
                        "<xacml-context:Subject SubjectCategory=\"urn:testcategory\">" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute>" +
                        "</xacml-context:Subject>" +
                        "<xacml-context:Resource>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute>" +
                        "<xacml-context:ResourceContent>test content</xacml-context:ResourceContent>" +
                        "</xacml-context:Resource>" +
                        "<xacml-context:Action>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute>" +
                        "</xacml-context:Action>" +
                        "<xacml-context:Environment>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Environment>" +
                        "</xacml-context:Request></saml:Statement>" },
                { false, false, "<saml:Statement xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xacml-saml=\"urn:oasis:names:tc:xacml:2.0:saml:assertion:schema:os\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xacml-saml:XACMLAuthzDecisionStatement\">" +
                        "<Response><Result ResourceId=\"testResource\">" +
                        "<Decision>Permit</Decision><Status><StatusCode Value=\"testValue\">" +
                        "<StatusCode Value=\"testMinorValue\"/></StatusCode><StatusMessage>testMessage</StatusMessage>" +
                        "<StatusDetail/></Status>" +
                        "<Obligations><Obligation FulfillOn=\"test\" ObligationId=\"urn:test\"/></Obligations>" +
                        "</Result></Response>" +
                        "<Request " +
                        "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os " +
                        "http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd\">" +
                        "<Subject SubjectCategory=\"urn:testcategory\">" +
                        "<Attribute Issuer=\"testIssuer\"><AttributeValue>a</AttributeValue></Attribute></Subject>" +
                        "<Resource><Attribute Issuer=\"testIssuer\"><AttributeValue>a</AttributeValue></Attribute>" +
                        "<ResourceContent xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">test content" +
                        "</ResourceContent></Resource><Action><Attribute Issuer=\"testIssuer\">" +
                        "<AttributeValue>a</AttributeValue></Attribute></Action><Environment>" +
                        "<Attribute Issuer=\"testIssuer\"><AttributeValue>a</AttributeValue></Attribute>" +
                        "</Environment></Request></saml:Statement>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        XACMLAuthzDecisionStatementImpl statement = new XACMLAuthzDecisionStatementImpl();
        statement.setRequest(getRequest());
        statement.setResponse(getResponse());

        // When
        String xml = statement.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

    private Request getRequest() throws Exception {
        RequestImpl request = new RequestImpl();
        AttributeImpl attribute = new AttributeImpl();
        attribute.setIssuer("testIssuer");
        attribute.setAttributeStringValues(List.of("a"));
        ActionImpl action = new ActionImpl();
        action.setAttributes(List.of(attribute));
        request.setAction(action);
        EnvironmentImpl environment = new EnvironmentImpl();
        environment.setAttributes(List.of(attribute));
        request.setEnvironment(environment);
        ResourceImpl resource = new ResourceImpl();
        Document document = XMLUtils.newDocument();
        Element content = document.createElementNS(XACMLConstants.CONTEXT_NS_URI, XACMLConstants.RESOURCE_CONTENT);
        content.setTextContent("test content");
        resource.setResourceContent(content);
        resource.setAttributes(List.of(attribute));
        request.setResources(List.of(resource));
        SubjectImpl subject = new SubjectImpl();
        subject.setSubjectCategory(URI.create("urn:testcategory"));
        subject.setAttributes(List.of(attribute));
        request.setSubjects(List.of(subject));
        return request;
    }

    private Response getResponse() throws Exception {
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
        return response;
    }
}