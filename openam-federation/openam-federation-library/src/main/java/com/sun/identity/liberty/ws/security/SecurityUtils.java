/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SecurityUtils.java,v 1.5 2009/06/08 23:42:33 madan_ranganath Exp $
 *
 * Portions Copyrighted 2014-2025 Ping Identity Corporation.
 */
package com.sun.identity.liberty.ws.security;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.keyvalues.DSAKeyValue;
import org.apache.xml.security.keys.content.keyvalues.RSAKeyValue;
import org.apache.xml.security.utils.Constants;
import org.forgerock.guice.core.InjectorHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.liberty.ws.common.wsse.BinarySecurityToken;
import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml.xmlsig.SignatureProvider;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class has common utility methods .
 */
public class SecurityUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    private static KeyProvider keystore;

    static {
        keystore = InjectorHolder.getInstance(SignatureProvider.class).getKeyProvider();
    }

    /**
     * Get Certificate from X509 Security Token Profile document.
     *
     * @param binarySecurityToken the Security Token.
     * @return X509 Certificate object.
     */
    public static java.security.cert.Certificate getCertificate(
            BinarySecurityToken binarySecurityToken) {
        
        java.security.cert.Certificate cert = null;
        
        try {
            String certString = binarySecurityToken.getTokenValue();

            StringBuilder xml = new StringBuilder(100);
            xml.append(WSSEConstants.BEGIN_CERT);
            xml.append(certString);
            xml.append(WSSEConstants.END_CERT);

            byte[] barr;
            barr = (xml.toString()).getBytes();
            
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(barr);
            
            QName valueType = binarySecurityToken.getValueType();
            if (valueType.equals(BinarySecurityToken.PKCS7)) { // PKCS7 format
                Collection<? extends Certificate> c = cf.generateCertificates(bais);
                for (Certificate certificate : c) {
                    cert = certificate;
                }
            } else { //X509:v3 format
                while (bais.available() > 0) {
                    cert = cf.generateCertificate(bais);
                }
            }
        } catch (Exception e) {
            // Certificate encoding error!
            logger.error("WSSecurityManager:getX509Certificate", e);
        }
        return cert;
    }
    
    /**
     * Gets the  Certificate from the <code>Assertion</code>.
     *
     * @param assertion the SAML <code>Assertion</code>.
     * @return <code>X509Certificate</code> object.
     */
    public static java.security.cert.Certificate getCertificate(
            SecurityAssertion assertion) {
        
        
        if (logger.isDebugEnabled()) {
            logger.debug("SecurityAssertion = " + assertion.toString());
        }
        try {
            Set<Statement> statements = assertion.getStatement();
            if (statements !=null && !(statements.isEmpty())) {
                for (Statement statement : statements) {
                    int stype = statement.getStatementType();
                    Subject subject = null;
                    if (stype == Statement.AUTHENTICATION_STATEMENT) {
                        subject = ((AuthenticationStatement) statement).getSubject();
                    }

                    if (subject != null) {
                        SubjectConfirmation subConfirm = subject.getSubjectConfirmation();
                        if (subConfirm.getConfirmationMethod().contains(
                                SAMLConstants.CONFIRMATION_METHOD_HOLDEROFKEY)) {
                            Element keyinfo = subConfirm.getKeyInfo();
                            return getCertificate(keyinfo);
                        }
                    }
                }
            } else {
                logger.error("Assertion does not contain any Statement.");
            }
        } catch (Exception e) {
            logger.error("getCertificate Exception: ", e);
        }
        return null;
    }
    
    /**
     * Returns the <code>X509Certificate</code> object.
     *
     * @param keyinfo the <code>KeyInfo</code> Document Element.
     * @return the <code>X509Certificate</code> object.
     */
    private static X509Certificate getCertificate(Element keyinfo) {
        
        X509Certificate cert = null;
        
        if (logger.isDebugEnabled()) {
            logger.debug("KeyInfo = " + XMLUtils.print(keyinfo));
        }
        
        Element x509 = (Element) keyinfo.getElementsByTagNameNS(
                Constants.SignatureSpecNS,
                SAMLConstants.TAG_X509CERTIFICATE).item(0);
        
        if (x509 == null) { // no cert found. try DSA/RSA key
            try {
                PublicKey pk = getPublicKey(keyinfo);
                cert = (X509Certificate) keystore.getCertificate(pk);
            } catch (Exception e) {
                logger.error("getCertificate Exception: ", e);
            }
            
        } else {
            String certString = x509.getChildNodes().item(0).getNodeValue();
            cert = getCertificate(certString, null);
        }
        
        return cert;
    }
    
    /**
     * Returns the <code>PublicKey</code>.
     */
    private static PublicKey getPublicKey(Element reference)
    throws XMLSignatureException {
        
        PublicKey pubKey = null;
        Document doc = reference.getOwnerDocument();
        Element dsaKey = (Element) reference.getElementsByTagNameNS(
                Constants.SignatureSpecNS,
                SAMLConstants.TAG_DSAKEYVALUE).item(0);
        if (dsaKey != null) { // It's DSAKey
            NodeList nodes = dsaKey.getChildNodes();
            int nodeCount = nodes.getLength();
            if (nodeCount > 0) {
                BigInteger p=null, q=null, g=null, y=null;
                for (int i = 0; i < nodeCount; i++) {
                    Node currentNode = nodes.item(i);
                    if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                        String tagName = currentNode.getLocalName();
                        Node sub = currentNode.getChildNodes().item(0);
                        String value = sub.getNodeValue();
                        value = SAMLUtils.removeNewLineChars(value);
                        BigInteger v = new BigInteger(Base64.decode(value));
                        if (tagName.equals("P")) {
                            p = v;
                        } else if (tagName.equals("Q")) {
                            q = v;
                        } else if (tagName.equals("G")) {
                            g = v;
                        } else if (tagName.equals("Y")) {
                            y = v;
                        } else {
                            logger.error("Wrong tag name in DSA key.");
                            throw new XMLSignatureException(
                                    SAMLUtils.bundle.getString("errorObtainPK"));
                        }
                    }
                }
                DSAKeyValue dsaKeyValue = new DSAKeyValue(doc, p, q, g, y);
                try {
                    pubKey = dsaKeyValue.getPublicKey();
                } catch (XMLSecurityException xse) {
                    logger.error("Could not get Public Key from" +
                            " DSA key value.");
                    throw new XMLSignatureException(
                            SAMLUtils.bundle.getString("errorObtainPK"));
                }
            }
        } else {
            Element rsaKey =
                    (Element) reference.getElementsByTagNameNS(
                    Constants.SignatureSpecNS,
                    SAMLConstants.TAG_RSAKEYVALUE).item(0);
            if (rsaKey != null) { // It's RSAKey
                NodeList nodes = rsaKey.getChildNodes();
                int nodeCount = nodes.getLength();
                BigInteger m=null, e=null;
                if (nodeCount > 0) {
                    for (int i = 0; i < nodeCount; i++) {
                        Node currentNode = nodes.item(i);
                        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                            String tagName = currentNode.getLocalName();
                            Node sub = currentNode.getChildNodes().item(0);
                            String value = sub.getNodeValue();
                            value = SAMLUtils.removeNewLineChars(value);
                            BigInteger v =new BigInteger(Base64.decode(value));
                            if (tagName.equals("Exponent")) {
                                e = v;
                            } else if (tagName.equals("Modulus")){
                                m = v;
                            } else {
                                logger.error("Wrong tag name from " +
                                        "RSA key element.");
                                throw new XMLSignatureException(
                                        SAMLUtils.bundle.getString("errorObtainPK"));
                            }
                        }
                    }
                }
                RSAKeyValue rsaKeyValue =
                        new RSAKeyValue(doc,m, e);
                try {
                    pubKey = rsaKeyValue.getPublicKey();
                } catch (XMLSecurityException ex) {
                    logger.error("Could not get Public Key from" +
                            " RSA key value.");
                    throw new XMLSignatureException(
                            SAMLUtils.bundle.getString("errorObtainPK"));
                }
            }
        }
        return pubKey;
    }
    
    /**
     * Returns the <code>X509Certificate</code> object.
     *
     * @param certString the Certificate String.
     * @param format the Certificate's format.
     * @return the <code>X509Certificate</code> object.
     */
    private static X509Certificate getCertificate(String certString,
            String format) {
        X509Certificate cert = null;
        
        try {
            
            if (logger.isDebugEnabled()) {
                logger.debug("getCertificate(Assertion) : " +
                        certString);
            }
            
            StringBuilder xml = new StringBuilder(100);
            xml.append(SAMLConstants.BEGIN_CERT);
            xml.append(certString);
            xml.append(SAMLConstants.END_CERT);
            
            byte[] barr;
            barr = (xml.toString()).getBytes();
            
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream bais = new ByteArrayInputStream(barr);
            
            if ((format !=null) &&
                    format.equals(SAMLConstants.TAG_PKCS7)) { // PKCS7 format
                Collection<? extends Certificate> c = cf.generateCertificates(bais);
                for (Certificate certificate : c) {
                    cert = (X509Certificate) certificate;
                }
            } else { //X509:v3 format
                while (bais.available() > 0) {
                    cert = (java.security.cert.X509Certificate)
                    cf.generateCertificate(bais);
                }
            }
        } catch (Exception e) {
            logger.error("getCertificate Exception: ", e);
        }
        
        return cert;
    }
}
