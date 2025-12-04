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

package com.sun.identity.saml2.assertion.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.forgerock.openam.saml2.crypto.signing.SigningConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Element;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.xmlsig.SigManager;
import com.sun.identity.shared.xml.XMLUtils;

public class AssertionImplTest {

    private static final String SIGNING_KEY =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDXSIZDmXkh2oE/\n" +
            "WXnBvkEMFmlLIuzjL9Sb6Qa/+x7QGRGTAxlq8YP82qOrzHf58o0jjmdTsTsI4CHy\n" +
            "COfosJd+6DnsRB+LSA6AqE6FVwuYqogRc+ap5qC8yd2xdYjLqTxY8/AHNKQ3k8Ms\n" +
            "oFcob9fUHBW84T3bvqevHqUmrcKA1uaISKGi823/qDmkLGJJqsiTa5obdPdV7qFQ\n" +
            "mszRbflCaqs0z2sveVo+Be0K7Piwkk3esadCr/blhtRrBb0vUxNVuorFaLI3lZte\n" +
            "hZSDy2jUOGhCdfMPKYgyEVrjk1MuHG10f0IOmtdZdYFpbOjyYgV6znIjgAlbhlKD\n" +
            "U887ZQlJAgMBAAECggEAamvam14pyDdovvUvQDwZka0efdsU+TWyxyPJh2vPDpXl\n" +
            "3yOgFAKx+XPnhsy73l4tsQ77Ox8YjMmnXitS4O8y5LRNteLzKPoE2UraDgY6oExk\n" +
            "mSQPOZvdh5XOtqPgbLULNPnZhOZb63FrAQt+Kmonah48DLPXzWIRKHtgrp8k0GUD\n" +
            "VMQGFPtDWxOCqtFyqiYBpO8EjYi4ojz4P9mw7H41j9K4vZKgDRrse0NRXBdY3ne8\n" +
            "RDIRfIrhFmd3r6IG8Fbk9E/52VuqFtfXDEf2FE+3HhW418vc27vpoMLijF9I4u9g\n" +
            "9a4YL2MBbXxiTLX5BuRLlu5n2w8qIB+sY1ZpmfH+0QKBgQDq2nYqWFVXVAbzUtnP\n" +
            "nufBqlahjLi/8gv0ul/xsTuORBLEf27+3q3caT0zRwrsylHlM5H8YNLAM4Qo8ZWk\n" +
            "T43sTxCenBWrf9qQ/sRsaadTXYK9P/0bfutUs8gVBbJOp7yBjL+ZXXNU/TfdQgcV\n" +
            "J4pKNcau5yhXx1thMrgydEPjzQKBgQDqqvV1BAK/wQeRUg54Tyw4OB674cRUkoy4\n" +
            "exA8Crvxfh2s0P27A29Rf+smS6IYn4Ah5iQOPJ2E0eHwbRbhI33Fi7Wv8Bok9Frl\n" +
            "7eET5wxn+kVPfi39TGwVwsK3PfniFBgoHCFgMArn01k+ltRCZbHqtA+WV0nAzbgn\n" +
            "leLPQrw3bQKBgCEaxklVxkHXEFvAM/+2MIj2D9wZz/kOj2zh0KBrETAOnG6iX+0B\n" +
            "SpSlKQFPZFTYeA7M5CklJM/+8wQqeaN9q03CH1+cNNnF3fNOVjXJ9tIjfkha+ryj\n" +
            "eVypMuzzrpyzvDiutFtT9uvl/bfslL8AodewGN+SqfclnNXoplpGVUOJAoGBANG8\n" +
            "RnF1Sje0AF2Dp2cj6/O7RMzLdbvY12iypMnlFiE6hK9GguA4q6990t+BTbkQJWcm\n" +
            "2CbZSjfBllxaQ86o9+oteg9rWxKYSv2h5D7zjAUKjBQGRHhVa2zvizRXchT7vLNs\n" +
            "oO/lQHn+TY9BIyjM131bvWqzTTnDI8pNjk+L58jpAoGAEBBi/Oekc3aHJhwbs3qt\n" +
            "dZ3Rc5b0mEr0BfGzdurpDiLOLb83wj2q+gnVRp8MhEaj34LNDp6nOrxofrRZRBFQ\n" +
            "Ei1c5w4uqDVd7JMV6al3J00OvVFczSFdD0OGk6S9Bb69U6tO6m3yIgY6HvBVLYi6\n" +
            "o9lMPYdpXTxgbh2ZlzINwfM=\n" +
            "-----END PRIVATE KEY-----\n";

    private static final String VERIFICATION_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDdzCCAl+gAwIBAgIES3eb+zANBgkqhkiG9w0BAQsFADBsMRAwDgYDVQQGEwdV\n" +
            "bmtub3duMRAwDgYDVQQIEwdVbmtub3duMRAwDgYDVQQHEwdVbmtub3duMRAwDgYD\n" +
            "VQQKEwdVbmtub3duMRAwDgYDVQQLEwdVbmtub3duMRAwDgYDVQQDEwdVbmtub3du\n" +
            "MB4XDTE2MDUyNDEzNDEzN1oXDTI2MDUyMjEzNDEzN1owbDEQMA4GA1UEBhMHVW5r\n" +
            "bm93bjEQMA4GA1UECBMHVW5rbm93bjEQMA4GA1UEBxMHVW5rbm93bjEQMA4GA1UE\n" +
            "ChMHVW5rbm93bjEQMA4GA1UECxMHVW5rbm93bjEQMA4GA1UEAxMHVW5rbm93bjCC\n" +
            "ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANdIhkOZeSHagT9ZecG+QQwW\n" +
            "aUsi7OMv1JvpBr/7HtAZEZMDGWrxg/zao6vMd/nyjSOOZ1OxOwjgIfII5+iwl37o\n" +
            "OexEH4tIDoCoToVXC5iqiBFz5qnmoLzJ3bF1iMupPFjz8Ac0pDeTwyygVyhv19Qc\n" +
            "FbzhPdu+p68epSatwoDW5ohIoaLzbf+oOaQsYkmqyJNrmht091XuoVCazNFt+UJq\n" +
            "qzTPay95Wj4F7Qrs+LCSTd6xp0Kv9uWG1GsFvS9TE1W6isVosjeVm16FlIPLaNQ4\n" +
            "aEJ18w8piDIRWuOTUy4cbXR/Qg6a11l1gWls6PJiBXrOciOACVuGUoNTzztlCUkC\n" +
            "AwEAAaMhMB8wHQYDVR0OBBYEFMm4/1hF4WEPYS5gMXRmmH0gs6XjMA0GCSqGSIb3\n" +
            "DQEBCwUAA4IBAQDVH/Md9lCQWxbSbie5lPdPLB72F4831glHlaqms7kzAM6IhRjX\n" +
            "md0QTYq3Ey1J88KSDf8A0HUZefhudnFaHmtxFv0SF5VdMUY14bJ9UsxJ5f4oP4CV\n" +
            "h57fHK0w+EaKGGIw6TQEkL5L/+5QZZAywKgPz67A3o+uk45aKpF3GaNWjGRWEPqc\n" +
            "GkyQ0sIC2o7FUTV+MV1KHDRuBgreRCEpqMoY5XGXe/IJc1EJLFDnsjIOQU1rrUzf\n" +
            "M+WP/DigEQTPpkKWHJpouP+LLrGRj2ziYVbBDveP8KtHvLFsnexA/TidjOOxChKS\n" +
            "LT9LYFyQqsvUyCagBb4aLs009kbW6inN8zA6\n" +
            "-----END CERTIFICATE-----\n";

    public static Object[][] xmlTestCases() {
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

    @ParameterizedTest
    @MethodSource("xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        AssertionImpl assertion = assertion();

        // When
        String xml = assertion.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    void testSignedToXmlString() throws Exception {
        // Given
        String id = "_d71a3a8e9fcc45c9e9d248ef7049393fc8f04e5f75";
        String origXml =
                "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" " +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"" +
                " ID=\"" + id + "\" Version=\"2.0\" IssueInstant=\"2014-07-17T01:01:48Z\">\n" +
                "    <saml:Issuer>http://idp.example.com/metadata.php</saml:Issuer>\n" +
                "    <saml:Subject>\n" +
                "      <saml:NameID SPNameQualifier=\"http://sp.example.com/demo1/metadata.php\"" +
                " Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:transient\">" +
                "_ce3d2948b4cf20146dee0a0b3dd6f69b6cf86f62d7</saml:NameID>\n" +
                "      <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n" +
                "        <saml:SubjectConfirmationData NotOnOrAfter=\"2024-01-18T06:21:48Z\"" +
                " Recipient=\"http://sp.example.com/demo1/index.php?acs\""+
                " InResponseTo=\"ONELOGIN_4fee3b046395c4e751011e97f8900b5273d56685\"/>\n" +
                "      </saml:SubjectConfirmation>\n" +
                "    </saml:Subject>\n" +
                "    <saml:Conditions NotBefore=\"2014-07-17T01:01:18Z\" NotOnOrAfter=\"2024-01-18T06:21:48Z\">\n" +
                "      <saml:AudienceRestriction>\n" +
                "        <saml:Audience>http://sp.example.com/demo1/metadata.php</saml:Audience>\n" +
                "      </saml:AudienceRestriction>\n" +
                "    </saml:Conditions>\n" +
                "    <saml:AuthnStatement AuthnInstant=\"2014-07-17T01:01:48Z\"" +
                " SessionNotOnOrAfter=\"2024-07-17T09:01:48Z\"" +
                " SessionIndex=\"_be9967abd904ddcae3c0eb4189adbe3f71e327cf93\">\n" +
                "      <saml:AuthnContext>\n" +
                "        <saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</saml:AuthnContextClassRef>\n" +
                "      </saml:AuthnContext>\n" +
                "    </saml:AuthnStatement>\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "    <saml:AttributeStatement>\n" +
                "      <saml:Attribute Name=\"uid\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
                "        <saml:AttributeValue xsi:type=\"xs:string\">test</saml:AttributeValue>\n" +
                "      </saml:Attribute>\n" +
                "      <saml:Attribute Name=\"mail\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
                "        <saml:AttributeValue xsi:type=\"xs:string\">test@example.com</saml:AttributeValue>\n" +
                "      </saml:Attribute>\n" +
                "      <saml:Attribute Name=\"eduPersonAffiliation\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
                "        <saml:AttributeValue xsi:type=\"xs:string\">users</saml:AttributeValue>\n" +
                "        <saml:AttributeValue xsi:type=\"xs:string\">examplerole1</saml:AttributeValue>\n" +
                "      </saml:Attribute>\n" +
                "    </saml:AttributeStatement>\n" +
                "  </saml:Assertion>";
        SigningConfig signingConfig = new SigningConfig(getSigningKey(), null,
                "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512",
                "http://www.w3.org/2001/04/xmlenc#sha512");
        Element signatureElement = SigManager.getSigInstance().sign(origXml, id, signingConfig);
        String signedXml = XMLUtils.print(signatureElement.getOwnerDocument().getDocumentElement(), "UTF-8");
        Assertion assertion = new AssertionImpl(signedXml);

        // When
        String xml = assertion.toXMLString(true, true);

        // Then
        assertThat(assertion.isSignatureValid(getX509VerificationCertificates())).isTrue();
        assertThat(xml).isEqualToIgnoringWhitespace(signedXml);
    }

    @Test
    void shouldRoundTripAsJson() throws Exception {
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

    private Set<X509Certificate> getX509VerificationCertificates() throws IOException {
        try {
            return Set.of((X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(
                            VERIFICATION_CERT.getBytes(StandardCharsets.UTF_8))));
        } catch (CertificateException e) {
            throw new IOException("Exception caught getting X509Certificate" + e.getMessage(), e);
        }
    }

    private PrivateKey getSigningKey() throws Exception {
        String privateBase64  = SIGNING_KEY
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] bytes = Base64.getDecoder().decode(privateBase64);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

}
