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

package com.sun.identity.xacml.saml2.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.impl.IssuerImpl;
import com.sun.identity.saml2.protocol.Extensions;
import com.sun.identity.saml2.protocol.impl.ExtensionsImpl;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.impl.ActionImpl;
import com.sun.identity.xacml.context.impl.AttributeImpl;
import com.sun.identity.xacml.context.impl.EnvironmentImpl;
import com.sun.identity.xacml.context.impl.RequestImpl;
import com.sun.identity.xacml.context.impl.ResourceImpl;
import com.sun.identity.xacml.context.impl.SubjectImpl;

public class XACMLAuthzDecisionQueryImplTest {

    public static Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:RequestAbstract xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "xmlns:xacml-samlp=\"urn:oasis:xacml:2.0:saml:protocol:schema:os\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"1973-03-03T09:46:40Z\" " +
                        "Version=\"2.0\" " +
                        "xacml-samlp:InputContextOnly=\"true\" " +
                        "xacml-samlp:ReturnContext=\"true\" " +
                        "xsi:type=\"xacml-samlp:XACMLAuthzDecisionQuery\">" +
                        "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                        "<samlp:Extensions>" +
                        "<foo>bar</foo>" +
                        "</samlp:Extensions>" +
                        "<xacml-context:Request xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os " +
                        "http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd\">" +
                        "<xacml-context:Subject SubjectCategory=\"urn:testcategory\">" +
                        "<xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Subject>" +
                        "<xacml-context:Resource>" +
                        "<xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Resource>" +
                        "<xacml-context:Action><xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Action>" +
                        "<xacml-context:Environment><xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Environment></xacml-context:Request>" +
                        "</samlp:RequestAbstract>" },
                { true, false, "<samlp:RequestAbstract " +
                        "xmlns:xacml-samlp=\"urn:oasis:xacml:2.0:saml:protocol:schema:os\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"1973-03-03T09:46:40Z\" " +
                        "Version=\"2.0\" " +
                        "xacml-samlp:InputContextOnly=\"true\" " +
                        "xacml-samlp:ReturnContext=\"true\" " +
                        "xsi:type=\"xacml-samlp:XACMLAuthzDecisionQuery\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<samlp:Extensions>" +
                        "<foo>bar</foo>" +
                        "</samlp:Extensions>" +
                        "<xacml-context:Request xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os " +
                        "http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd\">" +
                        "<xacml-context:Subject SubjectCategory=\"urn:testcategory\">" +
                        "<xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Subject>" +
                        "<xacml-context:Resource>" +
                        "<xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Resource>" +
                        "<xacml-context:Action><xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Action>" +
                        "<xacml-context:Environment><xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Environment></xacml-context:Request>" +
                        "</samlp:RequestAbstract>" },
                { false, false, "<RequestAbstract " +
                        "xmlns:xacml-samlp=\"urn:oasis:xacml:2.0:saml:protocol:schema:os\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com\" " +
                        "ID=\"testID\" " +
                        "IssueInstant=\"1973-03-03T09:46:40Z\" " +
                        "Version=\"2.0\" " +
                        "xacml-samlp:InputContextOnly=\"true\" " +
                        "xacml-samlp:ReturnContext=\"true\" " +
                        "xsi:type=\"xacml-samlp:XACMLAuthzDecisionQuery\">" +
                        "<Issuer>testIssuer</Issuer><Extensions>" +
                        "<foo>bar</foo>" +
                        "</Extensions>" +
                        "<xacml-context:Request xmlns:xacml-context=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\" " +
                        "xsi:schemaLocation=\"urn:oasis:names:tc:xacml:2.0:context:schema:os " +
                        "http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd\">" +
                        "<xacml-context:Subject SubjectCategory=\"urn:testcategory\">" +
                        "<xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Subject>" +
                        "<xacml-context:Resource><xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Resource>" +
                        "<xacml-context:Action><xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Action>" +
                        "<xacml-context:Environment><xacml-context:Attribute AttributeId=\"urn:test\">" +
                        "<xacml-context:AttributeValue>a</xacml-context:AttributeValue>" +
                        "</xacml-context:Attribute></xacml-context:Environment></xacml-context:Request>" +
                        "</RequestAbstract>" }
        };
    }

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        XACMLAuthzDecisionQueryImpl query = new XACMLAuthzDecisionQueryImpl();
        query.setID("testID");
        query.setIssueInstant(new Date(100000000000L));
        query.setConsent("testConsent");
        query.setDestination("https://test.example.com");
        query.setVersion("2.0");
        query.setInputContextOnly(true);
        query.setReturnContext(true);
        Issuer issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        query.setIssuer(issuer);
        query.setRequest(request());
        Extensions extensions = new ExtensionsImpl();
        extensions.setAny(List.of("<foo>bar</foo>"));
        query.setExtensions(extensions);

        // When
        String xml = query.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

    private Request request() throws Exception {
        Request request = new RequestImpl();

        AttributeImpl attribute = new AttributeImpl();
        attribute.setAttributeStringValues(List.of("a"));
        attribute.setAttributeId(URI.create("urn:test"));

        ActionImpl action = new ActionImpl();
        action.setAttributes(List.of(attribute));
        request.setAction(action);

        EnvironmentImpl environment = new EnvironmentImpl();
        environment.setAttributes(List.of(attribute));
        request.setEnvironment(environment);

        ResourceImpl resource = new ResourceImpl();
        resource.setAttributes(List.of(attribute));
        request.setResources(List.of(resource));

        SubjectImpl subject = new SubjectImpl();
        subject.setSubjectCategory(URI.create("urn:testcategory"));
        subject.setAttributes(List.of(attribute));
        request.setSubjects(List.of(subject));

        return request;
    }

    /*
    <samlp:RequestAbstract xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" xsi:type="xacml-samlp:XACMLAuthzDecisionQuery" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xacml-samlp="urn:oasis:xacml:2.0:saml:protocol:schema:os" xacml-samlp:InputContextOnly="true" xacml-samlp:ReturnContext="true" ID="testID" Version="2.0" IssueInstant="1973-03-03T09:46:40Z" Destination="https://test.example.com" Consent="testConsent"><saml:Issuer>testIssuer</saml:Issuer><samlp:Extensions><foo>bar</foo> </samlp:Extensions><xacml-context:Request xmlns:xacml-context="urn:oasis:names:tc:xacml:2.0:context:schema:os" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:oasis:names:tc:xacml:2.0:context:schema:os http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd"><xacml-context:Subject SubjectCategory="urn:testcategory"><xacml-context:Attribute AttributeId="urn:test"><xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute></xacml-context:Subject><xacml-context:Resource><xacml-context:Attribute AttributeId="urn:test"><xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute></xacml-context:Resource><xacml-context:Action><xacml-context:Attribute AttributeId="urn:test"><xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute></xacml-context:Action><xacml-context:Environment><xacml-context:Attribute AttributeId="urn:test"><xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute></xacml-context:Environment></xacml-context:Request></samlp:RequestAbstract>
    <samlp:RequestAbstract xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol" xsi:type="xacml-samlp:XACMLAuthzDecisionQuery" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xacml-samlp="urn:oasis:xacml:2.0:saml:protocol:schema:os" xacml-samlp:InputContextOnly="true" xacml-samlp:ReturnContext="true" ID="testID" Version="2.0" IssueInstant="1973-03-03T09:46:40Z" Destination="https://test.example.com" Consent="testConsent"><saml:Issuer>testIssuer</saml:Issuer><samlp:Extensions><foo>bar</foo> </samlp:Extensions><xacml-context:Request xmlns:xacml-context="urn:oasis:names:tc:xacml:2.0:context:schema:os" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="urn:oasis:names:tc:xacml:2.0:context:schema:os http://docs.oasis-open.org/xacml/access_control-xacml-2.0-context-schema-os.xsd"><xacml-context:Subject SubjectCategory="urn:testcategory"><xacml-context:Attribute AttributeId="urn:test"><xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute></xacml-context:Subject><xacml-context:Resource><xacml-context:Attribute AttributeId="urn:test"><xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute></xacml-context:Resource><xacml-context:Action><xacml-context:Attribute AttributeId="urn:test"><xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute></xacml-context:Action><xacml-context:Environment><xacml-context:Attribute AttributeId="urn:test"><xacml-context:AttributeValue>a</xacml-context:AttributeValue></xacml-context:Attribute></xacml-context:Environment></xacml-context:Request></samlp:RequestAbstract>">

     */
}
