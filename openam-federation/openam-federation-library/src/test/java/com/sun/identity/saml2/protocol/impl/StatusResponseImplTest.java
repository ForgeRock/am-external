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

package com.sun.identity.saml2.protocol.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.List;

import org.forgerock.openam.saml2.crypto.signing.SigningConfig;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.sun.identity.saml2.assertion.impl.IssuerImpl;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;

public class StatusResponseImplTest {
    @Test
    public void shouldEscapeSpecialCharactersInXmlInResponseTo() throws Exception {
        // Given
        String inResponseTo = "foo\" oops=\"bar";
        StatusResponseImpl statusResponse = new StubStatusResponse();
        statusResponse.setID("test");
        statusResponse.setVersion("2.0");
        statusResponse.setIssueInstant(new Date());
        statusResponse.setStatus(new StatusImpl());
        statusResponse.setInResponseTo(inResponseTo);

        // When
        String xml = statusResponse.toXMLString(true, true);
        Document doc = XMLUtils.toDOMDocument(xml);

        // Then
        assertThat(doc.getDocumentElement().hasAttribute("oops")).isFalse();
        assertThat(doc.getDocumentElement().getAttribute("InResponseTo")).isEqualTo(inResponseTo);
    }

    @DataProvider
    public Object[][] xmlTestCases() {
        return new Object[][] {
                { true, true, "<samlp:test xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "InResponseTo=\"testInResponseTo\" " +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" " +
                        "Version=\"2.0\"" +
                        ">" +
                        "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                        "<samlp:Status>" +
                        "<samlp:StatusCode Value=\"testCode\"/>" +
                        "<samlp:StatusMessage>" +
                        "testMessage" +
                        "</samlp:StatusMessage>" +
                        "<samlp:StatusDetail>" +
                        "a" +
                        "b" +
                        "</samlp:StatusDetail>" +
                        "</samlp:Status></samlp:test>" },
                { true, false, "<samlp:test " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "InResponseTo=\"testInResponseTo\"" +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" " +
                        "Version=\"2.0\" " +
                        ">" +
                        "<saml:Issuer>testIssuer</saml:Issuer>" +
                        "<samlp:Status>" +
                        "<samlp:StatusCode Value=\"testCode\"/>" +
                        "<samlp:StatusMessage>" +
                        "testMessage" +
                        "</samlp:StatusMessage>" +
                        "<samlp:StatusDetail>" +
                        "a" +
                        "b" +
                        "</samlp:StatusDetail>" +
                        "</samlp:Status></samlp:test>" },
                { false, false, "<test " +
                        "Consent=\"testConsent\" " +
                        "Destination=\"https://test.example.com/\" " +
                        "ID=\"testID\" " +
                        "InResponseTo=\"testInResponseTo\"" +
                        "IssueInstant=\"2001-09-09T01:46:40Z\" " +
                        "Version=\"2.0\">" +
                        "<Issuer>testIssuer</Issuer>" +
                        "<Status>" +
                        "<StatusCode Value=\"testCode\"/>" +
                        "<StatusMessage>" +
                        "testMessage" +
                        "</StatusMessage>" +
                        "<StatusDetail>" +
                        "a" +
                        "b" +
                        "</StatusDetail>" +
                        "</Status></test>" }
        };
    }

    @Test(dataProvider = "xmlTestCases")
    public void testToXmlString(boolean includeNS, boolean declareNS, String expectedXml) throws Exception {
        // Given
        StatusResponseImpl statusResponse = statusResponse();

        // When
        String xml = statusResponse.toXMLString(includeNS, declareNS);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace(expectedXml);
    }

    @Test
    public void testToXmlStringWhenSigned() throws Exception {
        // Given
        StatusResponseImpl statusResponse = statusResponse();
        // Note: it's important to use a deterministic signature algorithm here to ensure the same signature value is
        // always produced:
        SigningConfig signingConfig = new SigningConfig(signingKey(), null,
                "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512", "http://www.w3.org/2001/04/xmlenc#sha512");

        // When
        statusResponse.sign(signingConfig);
        String xml = statusResponse.toXMLString(true, true);

        // Then
        assertThat(xml).isEqualToIgnoringWhitespace("<samlp:test xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" " +
                "Consent=\"testConsent\" Destination=\"https://test.example.com/\" ID=\"testID\" " +
                "InResponseTo=\"testInResponseTo\" IssueInstant=\"2001-09-09T01:46:40Z\" Version=\"2.0\">" +
                "<saml:Issuer xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">testIssuer</saml:Issuer>" +
                "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo>" +
                "<ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/>" +
                "<ds:SignatureMethod Algorithm=\"http://www.w3.org/2001/04/xmldsig-more#rsa-sha512\"/>" +
                "<ds:Reference URI=\"#testID\"><ds:Transforms>" +
                "<ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/>" +
                "<ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms>" +
                "<ds:DigestMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#sha512\"/>" +
                "<ds:DigestValue>" +
                "cN48BeeAlbCOaTNlf1S0c5bsEX0RB6WW0Kd9gWgZSV8ubiWGszZNW3eUlASJIgRDncoQNr3gAs6jd1dI/1Tjfg==" +
                "</ds:DigestValue></ds:Reference></ds:SignedInfo>" +
                "<ds:SignatureValue>cJJGiAB2RLa/U1lxIPvQbVHjUj9mxS3FykhjwGh9sGOkwPeBBrLS+GTrwvPOgPy+br0Bx" +
                "Iu0Gaco5aESD3z/DxbakyYXTk6EG9nSacO01c2GC/XCp54rhfw2kVTVUqH7TsCICDZJSrZ7Uy42iOsEot7dlTa7b" +
                "H5RhKTjhSvGDi7PKWh+xjyFIPv/NkRuw3Ih+fK8BKXud2MOPA9b1oDghTJUigQwUfqEaQ78rOersUwOfDQnJf7Zs" +
                "NYrCq2hs27F0rdcGORN2WXDatB/0ugVvgzmh2BLN2Wdix7EI93x5nzYQwGZslEFSOB9r6tlVSYFc2UmRu9cU2tz2" +
                "PG+EdFS2A==</ds:SignatureValue></ds:Signature>" +
                "<samlp:Status><samlp:StatusCode Value=\"testCode\"/>" +
                "<samlp:StatusMessage>testMessage</samlp:StatusMessage>" +
                "<samlp:StatusDetail>ab</samlp:StatusDetail></samlp:Status></samlp:test>");
    }

    private static class StubStatusResponse extends StatusResponseImpl {

        public StubStatusResponse() {
            super("test");
            isMutable = true;
        }
    }

    private StatusResponseImpl statusResponse() throws Exception {
        StatusResponseImpl statusResponse = new StubStatusResponse();
        statusResponse.setID("testID");
        statusResponse.setConsent("testConsent");
        statusResponse.setInResponseTo("testInResponseTo");
        statusResponse.setIssueInstant(new Date(1000000000000L));
        statusResponse.setVersion("2.0");
        statusResponse.setDestination("https://test.example.com/");
        IssuerImpl issuer = new IssuerImpl();
        issuer.setValue("testIssuer");
        statusResponse.setIssuer(issuer);
        StatusImpl status = new StatusImpl();
        StatusCodeImpl statusCode = new StatusCodeImpl();
        statusCode.setValue("testCode");
        status.setStatusCode(statusCode);
        status.setStatusMessage("testMessage");
        StatusDetailImpl statusDetail = new StatusDetailImpl();
        statusDetail.setAny(List.of("a", "b"));
        status.setStatusDetail(statusDetail);
        statusResponse.setStatus(status);
        return statusResponse;
    }

    private static PrivateKey signingKey() throws Exception {
        String keyData = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCwM9/Ii61itf" +
                "/iLuuNYM2MGpaOSbBMz3nsaJcO7MmUcz/wIlsFeV8CTAXEr464AtDAcGvWRBiRIL5Dnongs" +
                "taBD+fA+yRL8VoE6BLaHxsWViEZOLYZykVLLmw911a11dlSTwhs8C++C3Z+GctgJIBkNO1j" +
                "3sN4tpw9lOvt6zo4zLyz/EwjS+0LdK71lV+7AfCrKfHNLqoOFO6qmmcCZuH1YgUuIMMtqj0" +
                "cwTGtcoGF1D/KWbhYU6WvsC+L2uo8uoE4BOFQTGLLiDDwRmXNexPGu5UTmruF4dRKDz+MF5" +
                "UB59uxWyJoTEqPX6ubmP+NdxP4Ed0DV5ElUw0hzUyDArAvAgMBAAECggEAIc8iTlm2wC0J1" +
                "z2Hhw4fApYLc2viPyrESz9aVMvMdgaTKyF8c5VxTICHztCcwuQPaA1qM6fHSvmMaG8gJ5RJ" +
                "ImSJ5HaL5WJ/ElfxXhb/3I5UNYGzN0mfeCiFLmyGP8eztl5h0H1Zu855QwLyChWW6SNatYI" +
                "F4cCwRASyLGmpN0KH9pz6de4O/YR6kNfnURL6o8V/bJlznPddZr4drMM6rySOJ4bjnILo6B" +
                "oeVb5fNDz3tUFciGWJiFDnIoIXTuAOkzo+MBYAPQbdVh0aaaJE01tqv06NcAoyu7pEvc8ra" +
                "6kcn4gWldLtJV/YVKaAm8WW+Kec0YodDT+LYq6GCD4HaQKBgQDjG6l5fve9+bAyrvwNnR8y" +
                "KTRtOIABOb5dXTqFPifvgd9Oo0CO7zi7vHJxJkY95xCTGgaa8nFH3AXfht0GIhuERncnWna" +
                "ca8RO6l9VJNm4Wo9JJyZ2WMC3jH4izVgETYJS6ARI1i/0rBMLy5wQtxLZjI7WzGETZytZCp" +
                "tuh7o3mwKBgQDGnlnDaoW3VUjzykjIJusoZNgy149yUq3vnjR9pi3mAUcenDMOxv5Fe7A8p" +
                "xRirICd7L7P68GNFJPeebLgRwObliaLWr6+Wj7/8xU/D0K+rLkMdBeq5HWPrrSgj4UXwxrB" +
                "EBazLXit6LwrwzoflsTpsjPnbjcn8otH/F1wUY70/QKBgQCt4ykw2HFR6RLqy6Y7ujj/jCM" +
                "TGXaQ+ahzDgXXrU6giJz1NfQhPLMAs3ogfoC7tuau6vdxvf3UWne4vScQIh9VeSqUXCSDAd" +
                "SN48/Yfl2hVN1u0mYVqUDtiMmvVfB1Yu9NEU7ugei3+uSeXGiDN9lb7s4TUutlEtJS29dEm" +
                "vGOzwKBgH4BLb9M0AIMKBkJ5vybvCpHN+WhXY7QpypSOsjP6WR2wRDJVZb1ZZDdNGR2NveW" +
                "qo4GCNPNpGAFgv3sRTAFvWMycceMzV/jzv3/0CXOX7Cp0Uf2SqGPCIob8tm901jM3SdINq5" +
                "7lYj3EpHM7A3oT4pXFJipEnLUa8pCb2D3TPNdAoGAKLBlUF2fXECp85C4wHC4ZVMXFsCGkN" +
                "g4F7v9IMkx5i0PBMot/z7reB5UpWxooybKZzzKV1ydBV7Bwg43D+6E9TmpNqNW66udzAOch" +
                "rO+9o+jC0WXPJEKc8oqqDAExWpint8rWVq4whMwfgsdlZ5xhn7TubUFPUUhDcvmjgUvi3s=";
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(keyData)));
    }
}