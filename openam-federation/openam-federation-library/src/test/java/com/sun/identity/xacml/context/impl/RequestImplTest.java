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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;

public class RequestImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<xacml-context:Request " +
                        "xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os " +
                        "http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd\">" +
                        "<xacml-context:Subject SubjectCategory=\"urn:testcategory\">" +
                        "<xacml-context:Attribute " +
                        "Issuer=\"testIssuer\"><xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute>" +
                        "</xacml-context:Subject>" +
                        "<xacml-context:Resource>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute>" +
                        "<xacml-context:ResourceContent>test content</xacml-context:ResourceContent>" +
                        "</xacml-context:Resource>" +
                        "<xacml-context:Action>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Action>" +
                        "<xacml-context:Environment>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Environment></xacml-context:Request>" },
                { true, false, "<xacml-context:Request>" +
                        "<xacml-context:Subject SubjectCategory=\"urn:testcategory\">" +
                        "<xacml-context:Attribute " +
                        "Issuer=\"testIssuer\"><xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute>" +
                        "</xacml-context:Subject>" +
                        "<xacml-context:Resource>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute>" +
                        "<xacml-context:ResourceContent>test content</xacml-context:ResourceContent>" +
                        "</xacml-context:Resource>" +
                        "<xacml-context:Action>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Action>" +
                        "<xacml-context:Environment>" +
                        "<xacml-context:Attribute Issuer=\"testIssuer\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Environment></xacml-context:Request>" },
                { false, false, "<Request>" +
                        "<Subject SubjectCategory=\"urn:testcategory\">" +
                        "<Attribute Issuer=\"testIssuer\"><AttributeValue>a</AttributeValue></Attribute>" +
                        "</Subject>" +
                        "<Resource><Attribute Issuer=\"testIssuer\"><AttributeValue>a</AttributeValue></Attribute>" +
                        "<ResourceContent " +
                        "xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">test content</ResourceContent>" +
                        "</Resource><Action><Attribute Issuer=\"testIssuer\">" +
                        "<AttributeValue>a</AttributeValue></Attribute></Action><Environment>" +
                        "<Attribute Issuer=\"testIssuer\"><AttributeValue>a</AttributeValue></Attribute>" +
                        "</Environment></Request>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
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

        // When
        String xml = request.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
}