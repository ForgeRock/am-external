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
 * Copyright 2013-2020 ForgeRock AS.
 */
package com.sun.identity.saml.xmlsig;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.xml.XMLUtils;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class AMSignatureProviderTest {

    private static final String DEFAULT_PRIVATE_KEY_ALIAS = "defaultkey";
    private static final String ID_ATTRIBUTE_VALUE = "signme";
    private static final String PRIVATE_KEY_PASS = "keypass";
    private static final String PRIVATE_KEY_ALIAS = "privatekey";
    private static final String RESPONSE_ID = "ResponseID";
    private static final String SIGNED_XML_DOCUMENT_RESPONSEID = "signeddocument-responseid.xml";
    private static final String SIGNED_XML_DOCUMENT = "signeddocument.xml";
    private static final String XML_DOCUMENT_TO_SIGN = "documenttosign.xml";

    private AMSignatureProvider signatureProvider;

    @BeforeClass
    public void setUp() {
        signatureProvider = new AMSignatureProvider();
        signatureProvider.initialize(new JKSKeyProvider());
    }

    @Test
    public void signXMLWithPrivateKeyUsingPassword() throws Exception {
        Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
        String encodedPrivatePass = AccessController.doPrivileged(new EncodeAction(PRIVATE_KEY_PASS));
        Element signature = signatureProvider.signXMLUsingKeyPass(documentToSign, PRIVATE_KEY_ALIAS,
                encodedPrivatePass, null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);

        assertThat(signature).isNotNull();
        NodeList nodes = documentToSign.getElementsByTagName("ds:Signature");
        assertThat(nodes.getLength()).isGreaterThan(0);
        assertThat(signature.isEqualNode(nodes.item(0))).isTrue();
    }

    @Test(expectedExceptions = XMLSignatureException.class)
    public void signXMLWithPrivateKeyAndNullPassword() throws Exception {
        Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
        signatureProvider.signXMLUsingKeyPass(documentToSign, PRIVATE_KEY_ALIAS,
                null, null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);
    }

    @Test(expectedExceptions = XMLSignatureException.class)
    public void signXMLWithPrivateKeyUsingDefaultPassword() throws Exception {
        Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
        signatureProvider.signXML(documentToSign, PRIVATE_KEY_ALIAS,
                null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);
    }

    @Test
    public void signXMLWithDefaultPrivateKeyAndNullPassword() throws Exception {
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
    public void signXMLWithDefaultPrivateKey() throws Exception {
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
    public void signXmlUsingPrivateKey() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        final KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        Document documentToSign = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN));
        Element signature =  signatureProvider.signXmlUsingPrivateKey(documentToSign, privateKey,
                createX509Certificate(privateKey, publicKey), ID_ATTRIBUTE_VALUE, null);
        assertThat(signature).isNotNull();
        NodeList nodes = documentToSign.getElementsByTagName("ds:Signature");
        assertThat(nodes.getLength()).isGreaterThan(0);
        assertThat(signature.isEqualNode(nodes.item(0))).isTrue();
    }

    @Test
    public void verifyDocumentResponseID() throws Exception {
        // Test that a signed document can be verified with an ID
        // from the set of "AssertionID", "RequestID", "ResponseID"
        Document signedDocument =
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT_RESPONSEID));
        assertThat(signatureProvider.verifyXMLSignature(signedDocument.getDocumentElement(),
                RESPONSE_ID, DEFAULT_PRIVATE_KEY_ALIAS)).isTrue();
    }

    @Test
    public void verifyDocument() throws Exception {
        // Test that a signed document can be verified
        Document signedDocument = XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT));
        assertThat(signatureProvider.verifyXMLSignature(signedDocument.getDocumentElement(),
                    SAML2Constants.ID, DEFAULT_PRIVATE_KEY_ALIAS)).isTrue();
    }

    private X509Certificate createX509Certificate(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        X509CertInfo info = new X509CertInfo();

        Date from = new Date();
        Date to = new Date(from.getTime() + 1000L * 24L * 60L * 60L);

        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name("cn=self signed");
        AlgorithmId sigAlgId = new AlgorithmId(AlgorithmId.sha256WithRSAEncryption_oid);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(serialNumber));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, owner);
        info.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(sigAlgId));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl certificate = new X509CertImpl(info);
        certificate.sign(privateKey, "SHA256withRSA");

        return certificate;
    }
}
