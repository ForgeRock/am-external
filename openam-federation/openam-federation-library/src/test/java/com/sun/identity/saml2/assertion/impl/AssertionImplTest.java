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

package com.sun.identity.saml2.assertion.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.List;

import org.forgerock.openam.saml2.crypto.signing.SigningConfig;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;

public class AssertionImplTest {

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "ID=\"testId\" " +
                        "IssueInstant=\"1970-04-26T17:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<saml:Subject>" +
                        "<saml:NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">demo</saml:NameID>" +
                        "</saml:Subject>" +
                        "<some>statement</some>" +
                        "<saml:AuthnStatement AuthnInstant=\"1970-04-26T17:46:40Z\" " +
                        "SessionIndex=\"2\">" +
                        "<saml:AuthnContext>" +
                        "<saml:AuthnContextClassRef>bar</saml:AuthnContextClassRef>" +
                        "<saml:AuthnContextDeclRef>foo</saml:AuthnContextDeclRef>" +
                        "</saml:AuthnContext>" +
                        "</saml:AuthnStatement>" +
                        "<saml:AttributeStatement>" +
                        "<saml:Attribute Name=\"testAttr\">" +
                        "<saml:AttributeValue " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">a</saml:AttributeValue>" +
                        "<saml:AttributeValue " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">b</saml:AttributeValue>" +
                        "</saml:Attribute>" +
                        "</saml:AttributeStatement></saml:Assertion>" },
                { true, false, "<saml:Assertion " +
                        "ID=\"testId\" " +
                        "IssueInstant=\"1970-04-26T17:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<saml:Subject>" +
                        "<saml:NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">demo</saml:NameID>" +
                        "</saml:Subject>" +
                        "<some>statement</some>" +
                        "<saml:AuthnStatement AuthnInstant=\"1970-04-26T17:46:40Z\" " +
                        "SessionIndex=\"2\">" +
                        "<saml:AuthnContext>" +
                        "<saml:AuthnContextClassRef>bar</saml:AuthnContextClassRef>" +
                        "<saml:AuthnContextDeclRef>foo</saml:AuthnContextDeclRef>" +
                        "</saml:AuthnContext>" +
                        "</saml:AuthnStatement>" +
                        "<saml:AttributeStatement>" +
                        "<saml:Attribute Name=\"testAttr\">" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">a</saml:AttributeValue>" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">b</saml:AttributeValue>" +
                        "</saml:Attribute>" +
                        "</saml:AttributeStatement></saml:Assertion>" },
                { false, false, "<Assertion " +
                        "ID=\"testId\" " +
                        "IssueInstant=\"1970-04-26T17:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<Subject><NameID " +
                        "Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">demo</NameID>" +
                        "</Subject><some>statement</some>" +
                        "<AuthnStatement AuthnInstant=\"1970-04-26T17:46:40Z\" " +
                        "SessionIndex=\"2\">" +
                        "<AuthnContext>" +
                        "<AuthnContextClassRef>bar</AuthnContextClassRef>" +
                        "<AuthnContextDeclRef>foo</AuthnContextDeclRef>" +
                        "</AuthnContext></AuthnStatement>" +
                        "<AttributeStatement>" +
                        "<Attribute Name=\"testAttr\">" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">a</saml:AttributeValue>" +
                        "<saml:AttributeValue xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                        "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"xs:string\">b</saml:AttributeValue>" +
                        "</Attribute></AttributeStatement></Assertion>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        AssertionImpl assertion = assertion();

        // When
        String xml = assertion.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }
    
    @Test
    public void shouldRoundTripAsJson() throws Exception {
        // Given
        Assertion assertion = assertion();
        // Sign the assertion to reflect most common scenario
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        SigningConfig config = new SigningConfig(keyPair.getPrivate(), null,
                "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", "http://www.w3.org/2001/04/xmlenc#sha512");
        assertion.sign(config);
        // Configuration copied from CTSObjectMapperProvider
        ObjectMapper mapper = new ObjectMapper()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, true);
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        // When
        String json = mapper.writeValueAsString(assertion);
        System.out.println(json);
        Assertion result = mapper.readValue(json, Assertion.class);

        // Then
        assertThat(result.getID()).isEqualTo(assertion.getID());
    }

    private AssertionImpl assertion() throws Exception {
        AssertionImpl assertion = new AssertionImpl();
        assertion.setID("testId");
        assertion.setIssueInstant(new Date(10000000000L));
        assertion.setVersion(SAML2Constants.VERSION_2_0);
        Issuer issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        assertion.setIssuer(issuer);
        assertion.setAuthnStatements(List.of(authnStatement()));
        AttributeStatementImpl attributeStatement = new AttributeStatementImpl();
        AttributeImpl attribute = new AttributeImpl();
        attribute.setName("testAttr");
        attribute.setAttributeValueString(List.of("a", "b"));
        attributeStatement.setAttribute(List.of(attribute));
        assertion.setAttributeStatements(List.of(attributeStatement));
        SubjectImpl subject = new SubjectImpl();
        NameIDImpl nameID = new NameIDImpl();
        nameID.setValue("demo");
        nameID.setFormat(SAML2Constants.NAMEID_TRANSIENT_FORMAT);
        subject.setNameID(nameID);
        assertion.setSubject(subject);
        assertion.setStatements(List.of("<some>statement</some>"));
        return assertion;
    }

    private AuthnStatementImpl authnStatement() throws Exception {
        AuthnStatementImpl authnStatement = new AuthnStatementImpl();
        authnStatement.setAuthnInstant(new Date(10000000000L));
        authnStatement.setSessionIndex("2");
        AuthnContext authnContext = new AuthnContextImpl();
        authnContext.setAuthnContextDeclRef("foo");
        authnContext.setAuthnContextClassRef("bar");
        authnStatement.setAuthnContext(authnContext);
        return authnStatement;
    }
}