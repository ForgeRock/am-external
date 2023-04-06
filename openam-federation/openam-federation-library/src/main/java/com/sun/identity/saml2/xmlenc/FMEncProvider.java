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
 * $Id: FMEncProvider.java,v 1.5 2008/06/25 05:48:03 qcheng Exp $
 *
 * Portions Copyrighted 2014-2022 ForgeRock AS.
 */
package com.sun.identity.saml2.xmlenc;

import static org.forgerock.openam.utils.CollectionUtils.entry;
import static org.forgerock.openam.utils.CollectionUtils.immutableOrderedMap;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.forgerock.openam.federation.util.XmlSecurity;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.key.EncryptionConfig;
import com.sun.identity.saml2.key.RsaOaepConfig;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xmlenc.EncryptionConstants;

/**
 * <code>FMEncProvier</code> is a class for encrypting and
 * decrypting XML documents, it implements <code>EncProvider</code>.
 */
public final class FMEncProvider implements EncProvider {

    private static final Logger logger = LoggerFactory.getLogger(FMEncProvider.class);
    /**
     * A hidden property to switch between two encryption formats. If true, will have a ds:KeyInfo Element inside
     * xenc:EncryptedData which will include the xenc:EncryptedKey Element (as defined in XML Encryption Specification).
     * If false, will have xenc:EncryptedKey Element parallels to xenc:EncryptedData (as defined in SAML2 profile
     * specification). Default to true if not specified.
     */
    private static boolean encryptedKeyInKeyInfo = true;

    /**
     * Lists the SAML 2.0 data encryption algorithms supported by this provider, along with the key size. Note: for
     * TripleDES, the keysize is specified as -1 to avoid checking the key size for backwards compatibility. The
     * entries are listed in order of preference, strongest algorithm first.
     */
    public static final Map<String, Integer> SUPPORTED_DATA_ENCRYPTION_ALGORITHMS = immutableOrderedMap(
            entry(XMLCipher.AES_256_GCM, 256),
            entry(XMLCipher.AES_192_GCM, 192),
            entry(XMLCipher.AES_128_GCM, 128),
            entry(XMLCipher.AES_256, 256),
            entry(XMLCipher.AES_192, 192),
            entry(XMLCipher.AES_128, 128),
            entry(XMLCipher.TRIPLEDES, -1) // Skip key size check for 3DES for backwards compatibility
    );

    static {
        XmlSecurity.init();
        String tmp = SystemConfigurationUtil.getProperty(
                "com.sun.identity.saml.xmlenc.encryptedKeyInKeyInfo");
        if ((tmp != null) && (tmp.equalsIgnoreCase("false"))) {
            encryptedKeyInKeyInfo = false;
        }
    }

    @Override
    public Element encrypt(String xmlString, EncryptionConfig encryptionConfig, String recipientEntityID,
            String outerElementName) throws SAML2Exception {
        return encrypt(xmlString, null, encryptionConfig, recipientEntityID, outerElementName);
    }

    @Override
    public Element encrypt(String xmlString, SecretKey secretKey, EncryptionConfig encryptionConfig,
            String recipientEntityID, String outerElementName) throws SAML2Exception {

        Key recipientPublicKey = encryptionConfig.getWrappingKey();
        String dataEncAlgorithm = encryptionConfig.getDataEncAlgorithm();
        int dataEncStrength = encryptionConfig.getDataEncStrength();
        String keyTransportAlgorithm = encryptionConfig.getKeyTransportAlgorithm();

        if (logger.isDebugEnabled()) {
            logger.debug("Data encryption algorithm = '{}'", dataEncAlgorithm);
            logger.debug("Data encryption strength = '{}'", dataEncStrength);
            logger.debug("Unique identifier of the recipient = '{}'", recipientEntityID);
        }

        // checking the input parameters
        if (StringUtils.isEmpty(xmlString) || recipientPublicKey == null || StringUtils.isEmpty(dataEncAlgorithm)
                || StringUtils.isEmpty(outerElementName)) {
            logger.error("Null input parameter(s).");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("nullInput"));
        }
        Integer expectedDataEncStrength = SUPPORTED_DATA_ENCRYPTION_ALGORITHMS.get(dataEncAlgorithm);
        if (expectedDataEncStrength == null) {
            logger.error("The encryption algorithm '{}' is not supported", dataEncAlgorithm);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("unsupportedKeyAlg"));
        }
        if (expectedDataEncStrength != -1 && dataEncStrength != expectedDataEncStrength) {
            logger.error("Data encryption algorithm '{}' and strength '{}' mismatch.", dataEncAlgorithm,
                    dataEncStrength);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("algSizeMismatch"));
        }
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            logger.error("the XML '{}' String can't be parsed.", xmlString);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        if (dataEncStrength <= 0) {
            dataEncStrength = 128;
        }
        Element rootElement = doc.getDocumentElement();
        if (rootElement == null) {
            logger.error("the XML '{}' String is empty.", xmlString);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("emptyDoc"));
        }
        // start of obtaining secret key
        if (secretKey == null) {
            if (recipientEntityID != null) {
                secretKey = generateSecretKey(dataEncAlgorithm, dataEncStrength);
            } else {
                secretKey = generateSecretKey(dataEncAlgorithm, dataEncStrength);
            }
            if (secretKey == null) {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorGenerateKey"));
            }
        }
        // end of obtaining secret key

        XMLCipher cipher;
        // start of encrypting the secret key with public key
        String publicKeyEncAlg = recipientPublicKey.getAlgorithm();
        /* note that the public key encryption algorithm could only
         * have three possible values here: "RSA", "AES", "DESede"
         */
        Optional<RsaOaepConfig> rsaOaepConfig = encryptionConfig.getRsaOaepConfig();
        try {
            logger.debug("public key encryption algorithm '{}'", publicKeyEncAlg);

            if (StringUtils.isEmpty(keyTransportAlgorithm)) {
                switch (publicKeyEncAlg) {
                case EncryptionConstants.RSA:
                    keyTransportAlgorithm = SystemPropertiesManager.get(SAML2Constants.RSA_KEY_TRANSPORT_ALGORITHM,
                            XMLCipher.RSA_OAEP);
                    rsaOaepConfig = RsaOaepConfig.getDefaultConfigForKeyTransportAlgorithm(keyTransportAlgorithm);
                    break;
                case EncryptionConstants.TRIPLEDES:
                    keyTransportAlgorithm = XMLCipher.TRIPLEDES_KeyWrap;
                    break;
                case EncryptionConstants.AES:
                    keyTransportAlgorithm = SystemPropertiesManager.get(SAML2Constants.AES_KEY_WRAP_ALGORITHM,
                            XMLCipher.AES_256_KeyWrap);
                    break;
                default:
                    logger.error("public key encryption algorithm '{}' unsupported", publicKeyEncAlg);
                    throw new SAML2Exception(SAML2SDKUtils.bundle.getString("unsupportedKeyAlg"));
                }
            }

            cipher = XMLCipher.getInstance(keyTransportAlgorithm, null,
                    rsaOaepConfig.map(RsaOaepConfig::getDigestMethod).orElse(null));
        } catch (XMLEncryptionException ex) {
            logger.error("Unable to obtain cipher with public key algorithm '{}'.", publicKeyEncAlg, ex);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noCipherForPublicKeyAlg"));
        }
        try {
            cipher.init(XMLCipher.WRAP_MODE, recipientPublicKey);
        } catch (XMLEncryptionException xe2) {
            logger.error("Failed to initialize cipher with public key", xe2);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedInitCipherWithPublicKey"));
        }
        EncryptedKey encryptedKey = null;
        try {
            if (XMLCipher.RSA_OAEP.equalsIgnoreCase(keyTransportAlgorithm) || !rsaOaepConfig.isPresent()) {
                encryptedKey = cipher.encryptKey(doc, secretKey);
            } else {
                String maskGenerationFunction = rsaOaepConfig.map(RsaOaepConfig::getMaskGenerationFunction)
                        .filter(mgf -> !EncryptionConstants.ENC_KEY_ENC_METHOD_MGF_MGF1_SHA1.equals(mgf))
                        .orElse(null);
                encryptedKey = cipher.encryptKey(doc, secretKey, maskGenerationFunction,
                        rsaOaepConfig.map(RsaOaepConfig::getOaepParams).orElse(null));
            }
        } catch (XMLEncryptionException xe3) {
            logger.error("Failed to encrypt secret key with public key", xe3);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedEncryptingSecretKeyWithPublicKey"));
        }
        // end of encrypting the secret key with public key

        // start of doing data encryption
        try {
            cipher = XMLCipher.getInstance(dataEncAlgorithm);
        } catch (XMLEncryptionException xe4) {
            logger.error("Failed to obtain a cipher for data encryption algorithm {}", dataEncAlgorithm, xe4);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("cipherNotAvailableForDataEncAlg"));
        }
        try {
            cipher.init(XMLCipher.ENCRYPT_MODE, secretKey);
        } catch (XMLEncryptionException xe5) {
            logger.error("Failed to initialize cipher with secret key.", xe5);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedInitCipherWithSecretKey"));
        }
        Document resultDoc = null;
        try {
            resultDoc = cipher.doFinal(doc, rootElement);
        } catch (Exception e) {
            logger.error("Failed to do the final data encryption.", e);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedEncryptingData"));
        }
        // end of doing data encryption

        // add the EncryptedKey element
        Element ek = null;
        try {
            ek = cipher.martial(doc, encryptedKey);
        } catch (Exception xe6) {
            logger.error("Failed to martial the encrypted key", xe6);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedMartialingEncryptedKey"));
        }

        String outerElemNS = SAML2Constants.ASSERTION_NAMESPACE_URI;
        String outerElemPrefix = "saml";
        if (outerElementName.equals("NewEncryptedID")) {
            outerElemNS = SAML2Constants.PROTOCOL_NAMESPACE;
            outerElemPrefix = "samlp";
        }
        Element outerElement = resultDoc.createElementNS(outerElemNS, outerElemPrefix + ":" + outerElementName);
        outerElement.setAttributeNS(SAML2Constants.NS_XML, "xmlns:" + outerElemPrefix, outerElemNS);
        Element ed = resultDoc.getDocumentElement();
        resultDoc.replaceChild(outerElement, ed);
        outerElement.appendChild(ed);
        if (encryptedKeyInKeyInfo) {
            // create a ds:KeyInfo Element to include the EncryptionKey
            Element dsElement = resultDoc.createElementNS(SAML2Constants.NS_XMLSIG, "ds:KeyInfo");
            dsElement.setAttributeNS(SAML2Constants.NS_XML, "xmlns:ds", SAML2Constants.NS_XMLSIG);
            dsElement.appendChild(ek);
            // find the xenc:CipherData Element inside the encrypted data
            NodeList nl = ed.getElementsByTagNameNS(SAML2Constants.NS_XMLENC, "CipherData");
            if (nl == null || nl.getLength() == 0) {
                logger.error("Unable to find required xenc:CipherData Element.");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedEncryptingData"));
            }
            Element cipherDataElement = (Element) nl.item(0);
            // insert the EncryptedKey before the xenc:CipherData Element
            ed.insertBefore(dsElement, cipherDataElement);
        } else {
            outerElement.appendChild(ek);
        }
        return resultDoc.getDocumentElement();
    }

    @Override
    public SecretKey getSecretKey(String xmlString, Set<PrivateKey> privateKeys) throws SAML2Exception {
        String classMethod = "FMEncProvider.getSecretKey: ";
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod + "Entering ...");
        }

        if (xmlString == null || xmlString.length() == 0 || privateKeys == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("nullInput"));
        }
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        Element rootElement = doc.getDocumentElement();
        if (rootElement == null) {
            logger.error(classMethod + "Empty document.");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("emptyDoc"));
        }
        Element firstChild = getNextElementNode(rootElement.getFirstChild());
        if (firstChild == null) {
            logger.error(classMethod + "Missing the EncryptedData element.");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missingElementEncryptedData"));
        }
        Element secondChild = getNextElementNode(firstChild.getNextSibling());
        if (secondChild == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod + "looking for encrytion key inside first child.");
            }
            NodeList nl = firstChild.getElementsByTagNameNS(SAML2Constants.NS_XMLENC, "EncryptedKey");
            if ((nl == null) || (nl.getLength() == 0)) {
                logger.error(classMethod + "Missing the EncryptedKey element.");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missingElementEncryptedKey"));
            } else {
                // use the first EncryptedKey found
                secondChild = (Element) nl.item(0);
            }
        }

        XMLCipher cipher = null;
        try {
            cipher = XMLCipher.getInstance();
        } catch (XMLEncryptionException xe1) {
            logger.error(classMethod + "Unable to get a cipher instance.", xe1);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noCipher"));
        }
        try {
            cipher.init(XMLCipher.DECRYPT_MODE, null);
        } catch (XMLEncryptionException xe2) {
            logger.error(classMethod + "Failed to initialize cipher for decryption mode", xe2);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedInitCipherForDecrypt"));
        }
        EncryptedData encryptedData = null;
        try {
            encryptedData = cipher.loadEncryptedData(doc, firstChild);
        } catch (XMLEncryptionException xe3) {
            logger.error(classMethod + "Failed to load encrypted data", xe3);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedLoadingEncryptedData"));
        }
        EncryptedKey encryptedKey = null;
        try {
            encryptedKey = cipher.loadEncryptedKey(doc, secondChild);
        } catch (XMLEncryptionException xe4) {
            logger.error(classMethod + "Failed to load encrypted key", xe4);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedLoadingEncryptedKey"));
        }
        if ((encryptedKey != null) && (encryptedData != null)) {
            XMLCipher keyCipher;
            try {
                keyCipher = XMLCipher.getInstance();
            } catch (XMLEncryptionException xe5) {
                logger.error(classMethod + "Failed to get a cipher instance for decrypting secret key.", xe5);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noCipher"));
            }

            return (SecretKey) getEncryptionKey(keyCipher, privateKeys, encryptedKey,
                    encryptedData.getEncryptionMethod().getAlgorithm());
        }

        return null;
    }

    @Override
    public Element decrypt(String xmlString, Set<PrivateKey> privateKeys) throws SAML2Exception {
        String classMethod = "FMEncProvider.decrypt: ";
        if (logger.isDebugEnabled()) {
            logger.debug(classMethod + "Entering ...");
        }
        if (StringUtils.isEmpty(xmlString)) {
            logger.error(classMethod + "The xmlString to decrypt was empty.");
            throw new SAML2Exception(SAML2SDKUtils.BUNDLE_NAME, "emptyInputMessage", new String[]{"xmlString"});
        }
        if (CollectionUtils.isEmpty(privateKeys)) {
            logger.error(classMethod + "The set of private keys for decryption was empty.");
            throw new SAML2Exception(SAML2SDKUtils.BUNDLE_NAME, "emptyInputMessage", new String[]{"private key set"});
        }
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        Element rootElement = doc.getDocumentElement();
        if (rootElement == null) {
            logger.error(classMethod + "Empty document.");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("emptyDoc"));
        }
        Element firstChild = getNextElementNode(rootElement.getFirstChild());
        if (firstChild == null) {
            logger.error(classMethod + "Missing the EncryptedData element.");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missingElementEncryptedData"));
        }
        Element secondChild = getNextElementNode(firstChild.getNextSibling());
        if (secondChild == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(classMethod + "looking for encrytion key inside first child.");
            }
            NodeList nl = firstChild.getElementsByTagNameNS(SAML2Constants.NS_XMLENC, "EncryptedKey");
            if ((nl == null) || (nl.getLength() == 0)) {
                logger.error(classMethod + "Missing the EncryptedKey element.");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("missingElementEncryptedKey"));
            } else {
                // use the first EncryptedKey found
                secondChild = (Element) nl.item(0);
            }
        }
        XMLCipher cipher = null;
        try {
            cipher = XMLCipher.getInstance();
        } catch (XMLEncryptionException xe1) {
            logger.error(classMethod + "Unable to get a cipher instance.", xe1);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noCipher"));
        }
        try {
            cipher.init(XMLCipher.DECRYPT_MODE, null);
        } catch (XMLEncryptionException xe2) {
            logger.error(classMethod + "Failed to initialize cipher for decryption mode", xe2);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedInitCipherForDecrypt"));
        }
        EncryptedData encryptedData = null;
        try {
            encryptedData = cipher.loadEncryptedData(doc, firstChild);
        } catch (XMLEncryptionException xe3) {
            logger.error(classMethod + "Failed to load encrypted data", xe3);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedLoadingEncryptedData"));
        }
        EncryptedKey encryptedKey = null;
        try {
            encryptedKey = cipher.loadEncryptedKey(doc, secondChild);
        } catch (XMLEncryptionException xe4) {
            logger.error(classMethod + "Failed to load encrypted key", xe4);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedLoadingEncryptedKey"));
        }
        Document decryptedDoc = null;
        if (encryptedKey != null && encryptedData != null) {
            XMLCipher keyCipher = null;
            try {
                keyCipher = XMLCipher.getInstance();
            } catch (XMLEncryptionException xe5) {
                logger.error(classMethod + "Failed to get a cipher instance for decrypting secret key.", xe5);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noCipher"));
            }

            Key encryptionKey = getEncryptionKey(keyCipher, privateKeys, encryptedKey,
                    encryptedData.getEncryptionMethod().getAlgorithm());

            cipher = null;
            try {
                cipher = XMLCipher.getInstance();
            } catch (XMLEncryptionException xe8) {
                logger.error(classMethod + "Failed to get cipher instance for final data decryption.", xe8);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("noCipher"));
            }
            try {
                cipher.init(XMLCipher.DECRYPT_MODE, encryptionKey);
            } catch (XMLEncryptionException xe9) {
                logger.error(classMethod + "Failed to initialize cipher with secret key.", xe9);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedInitCipherForDecrypt"));
            }
            try {
                decryptedDoc = cipher.doFinal(doc, firstChild);
            } catch (Exception e) {
                logger.error(classMethod + "Failed to decrypt data.", e);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedDecryptingData"));
            }
        }
        Element root = decryptedDoc.getDocumentElement();
        Element child = getNextElementNode(root.getFirstChild());
        if (child == null) {
            logger.error(classMethod + "decrypted document contains empty element.");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("failedDecryptingData"));
        }
        root.removeChild(child);
        decryptedDoc.replaceChild(child, root);
        return decryptedDoc.getDocumentElement();
    }

    /**
     * Returns the next Element node, return null if no such node exists.
     */
    private Element getNextElementNode(Node node) {
        while (true) {
            if (node == null) {
                return null;
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) node;
            } else {
                node = node.getNextSibling();
            }
        }
    }

    /**
     * Generates secret key for a given algorithm and key strength.
     */
    private SecretKey generateSecretKey(String algorithm, int keyStrength)
            throws SAML2Exception {
        KeyGenerator keygen = null;
        try {
            if (!SUPPORTED_DATA_ENCRYPTION_ALGORITHMS.containsKey(algorithm)) {
                logger.error("generateSecretKey : unsupported algorithm '{}'", algorithm);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString("unsupportedKeyAlg"));
            }
            if (algorithm.equals(XMLCipher.TRIPLEDES)) {
                keygen = KeyGenerator.getInstance("TripleDES");
            } else {
                keygen = KeyGenerator.getInstance("AES");
            }

            if (keyStrength != 0) {
                keygen.init(keyStrength);
            }
        } catch (NoSuchAlgorithmException ne) {
            logger.error("generateSecretKey : can't find algorithm '{}'", algorithm);
            throw new SAML2Exception(ne);
        }

        return keygen.generateKey();
    }

    private Key getEncryptionKey(XMLCipher cipher, Set<PrivateKey> privateKeys, EncryptedKey encryptedKey,
            String algorithm) throws SAML2Exception {
        final String classMethod = "FMEncProvider.getEncryptionKey";
        logger.debug("{} : algorithm '{}'", classMethod, algorithm);

        String firstErrorCode = null;
        for (Key privateKey : privateKeys) {
            try {
                cipher.init(XMLCipher.UNWRAP_MODE, privateKey);
            } catch (XMLEncryptionException xee) {
                logger.warn(classMethod + "Failed to initialize cipher in unwrap mode with private key", xee);
                if (firstErrorCode == null) {
                    firstErrorCode = "noCipherForUnwrap";
                }
                continue;
            }
            try {
                return cipher.decryptKey(encryptedKey, algorithm);
            } catch (XMLEncryptionException xee) {
                logger.error(classMethod + "Failed to decrypt the secret key", xee);
                if (firstErrorCode == null) {
                    firstErrorCode = "failedDecryptingSecretKey";
                }
            }
        }
        throw new SAML2Exception(SAML2SDKUtils.bundle.getString(firstErrorCode));
    }
}
