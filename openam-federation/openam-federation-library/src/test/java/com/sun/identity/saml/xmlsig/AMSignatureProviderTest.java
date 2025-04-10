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
 * Copyright 2013-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package com.sun.identity.saml.xmlsig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.shared.testsupport.CertificateFactory.aTestX509Certificate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.xml.XMLUtils;

public class AMSignatureProviderTest {

    private static final String DEFAULT_PRIVATE_KEY_ALIAS = "defaultkey";
    private static final String ID_ATTRIBUTE_VALUE = "signme";
    private static final String PRIVATE_KEY_PASS = "keypass";
    private static final String PRIVATE_KEY_ALIAS = "privatekey";
    private static final String RESPONSE_ID = "ResponseID";
    private static final String SIGNED_XML_DOCUMENT_RESPONSEID = "signeddocument-responseid.xml";
    private static final String SIGNED_XML_DOCUMENT = "signeddocument.xml";
    private static final String XML_DOCUMENT_TO_SIGN = "documenttosign.xml";

    private static AMSignatureProvider signatureProvider;

    @BeforeAll
    static void setUp() {
        signatureProvider = new AMSignatureProvider();
        signatureProvider.initialize(new JKSKeyProvider());
    }

    @Test
    void signXMLWithPrivateKeyUsingPassword() throws Exception {
        Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
        String encodedPrivatePass = new EncodeAction(PRIVATE_KEY_PASS).run();
        Element signature = signatureProvider.signXMLUsingKeyPass(documentToSign, PRIVATE_KEY_ALIAS,
                encodedPrivatePass, null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);

        assertThat(signature).isNotNull();
        NodeList nodes = documentToSign.getElementsByTagName("ds:Signature");
        assertThat(nodes.getLength()).isGreaterThan(0);
        assertThat(signature.isEqualNode(nodes.item(0))).isTrue();
    }

    @Test
    void signXMLWithPrivateKeyAndNullPassword() throws Exception {
        assertThatThrownBy(() -> {
            Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
            signatureProvider.signXMLUsingKeyPass(documentToSign, PRIVATE_KEY_ALIAS,
                    null, null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);
        }).isInstanceOf(XMLSignatureException.class);
    }

    @Test
    void signXMLWithPrivateKeyUsingDefaultPassword() throws Exception {
        assertThatThrownBy(() -> {
            Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
            signatureProvider.signXML(documentToSign, PRIVATE_KEY_ALIAS,
                    null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);
        }).isInstanceOf(XMLSignatureException.class);
    }

    @Test
    void signXMLWithDefaultPrivateKeyAndNullPassword() throws Exception {
        Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
        // Passing null for password should trigger using default keystore password to load private key
        Element signature = signatureProvider.signXMLUsingKeyPass(documentToSign, DEFAULT_PRIVATE_KEY_ALIAS,
                null, null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);

        assertThat(signature).isNotNull();
        NodeList nodes = documentToSign.getElementsByTagName("ds:Signature");
        assertThat(nodes.getLength()).isGreaterThan(0);
        assertThat(signature.isEqualNode(nodes.item(0))).isTrue();
    }

    @Test
    void signXMLWithDefaultPrivateKey() throws Exception {
        Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
        // Should trigger using default keystore password to load private key
        Element signature = signatureProvider.signXML(documentToSign, DEFAULT_PRIVATE_KEY_ALIAS,
                null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);

        assertThat(signature).isNotNull();
        NodeList nodes = documentToSign.getElementsByTagName("ds:Signature");
        assertThat(nodes.getLength()).isGreaterThan(0);
        assertThat(signature.isEqualNode(nodes.item(0))).isTrue();
    }

    @Test
    void signXmlUsingPrivateKey() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        final KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
        Element signature =  signatureProvider.signXmlUsingPrivateKey(documentToSign, privateKey,
                aTestX509Certificate(privateKey, publicKey), ID_ATTRIBUTE_VALUE, null);
        assertThat(signature).isNotNull();
        NodeList nodes = documentToSign.getElementsByTagName("ds:Signature");
        assertThat(nodes.getLength()).isGreaterThan(0);
        assertThat(signature.isEqualNode(nodes.item(0))).isTrue();
    }

    @Test
    void verifyDocumentResponseID() throws Exception {
        // Test that a signed document can be verified with an ID
        // from the set of "AssertionID", "RequestID", "ResponseID"
        Document signedDocument =
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT_RESPONSEID));
        assertThat(signatureProvider.verifyXMLSignature(signedDocument.getDocumentElement(),
                RESPONSE_ID, DEFAULT_PRIVATE_KEY_ALIAS)).isTrue();
    }

    @Test
    void verifyDocument() throws Exception {
        // Test that a signed document can be verified
        Document signedDocument = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT));
        assertThat(signatureProvider.verifyXMLSignature(signedDocument.getDocumentElement(),
                    SAML2Constants.ID, DEFAULT_PRIVATE_KEY_ALIAS)).isTrue();
    }
}
