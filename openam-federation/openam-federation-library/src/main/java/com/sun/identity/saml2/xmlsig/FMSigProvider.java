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
 * $Id: FMSigProvider.java,v 1.5 2009/05/09 15:43:59 mallas Exp $
 *
 * Portions Copyrighted 2011-2021 ForgeRock AS.
 */
package com.sun.identity.saml2.xmlsig;

import static org.forgerock.openam.shared.security.crypto.SignatureSecurityChecks.sanityCheckRawEcdsaSignatureValue;

import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.Collections;
import java.util.Set;

import javax.xml.xpath.XPathException;

import org.apache.xml.security.algorithms.SignatureAlgorithm;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.keyresolver.KeyResolverException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.forgerock.openam.federation.util.XmlSecurity;
import org.forgerock.openam.saml2.crypto.signing.SigningConfig;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.xml.XPathAPI;

/**
 * <code>FMSigProvider</code> is an class for signing
 * and verifying XML documents, it implements <code>SigProvider</code>
 */
public final class FMSigProvider implements SigProvider {

    private static final Logger logger = LoggerFactory.getLogger(FMSigProvider.class);
    private static String c14nMethod;
    private static String transformAlg;
    // flag to check if the partner's signing cert included in
    // the XML doc is the same as the one in its meta data
    private static boolean checkCert = true;

    static {
        XmlSecurity.init();

        c14nMethod = SystemPropertiesManager.get(
                SAML2Constants.CANONICALIZATION_METHOD,
                Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        transformAlg = SystemPropertiesManager.get(
                SAML2Constants.TRANSFORM_ALGORITHM,
                Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);

        String valCert =
                SystemPropertiesManager.get(
                        "com.sun.identity.saml.checkcert",
                        "on");
        if (valCert != null &&
                valCert.trim().equalsIgnoreCase("off")) {
            checkCert = false;
        }
    }

    /**
     * Default Constructor
     */
    public FMSigProvider() {
    }

    @Override
    public Element sign(String xmlString, String idValue, SigningConfig signingConfig) throws SAML2Exception {
        String classMethod = "FMSigProvider.sign: ";
        if (StringUtils.isEmpty(xmlString)) {
            logger.error(classMethod + "The xml to sign was empty.");
            throw new SAML2Exception(SAML2SDKUtils.BUNDLE_NAME, "emptyInputMessage", "xml");
        }
        if (StringUtils.isEmpty(idValue)) {
            logger.error(classMethod + "The idValue was empty.");
            throw new SAML2Exception(SAML2SDKUtils.BUNDLE_NAME, "emptyInputMessage", "idValue");
        }
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        Element root = doc.getDocumentElement();
        XMLSignature sig;
        try {
            ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, SAMLConstants.PREFIX_DS);
        } catch (XMLSecurityException xse1) {
            throw new SAML2Exception(xse1);
        }
        root.setIdAttribute(SAML2Constants.ID, true);
        try {
            sig = new XMLSignature(doc, "", signingConfig.getSigningAlgorithm(), c14nMethod);
        } catch (XMLSecurityException xse2) {
            throw new SAML2Exception(xse2);
        }
        Node firstChild = root.getFirstChild();
        while (firstChild != null &&
                (firstChild.getLocalName() == null ||
                        !firstChild.getLocalName().equals("Issuer"))) {
            firstChild = firstChild.getNextSibling();
        }
        Node nextSibling = null;
        if (firstChild != null) {
            nextSibling = firstChild.getNextSibling();
        }
        if (nextSibling == null) {
            root.appendChild(sig.getElement());
        } else {
            root.insertBefore(sig.getElement(), nextSibling);
        }
        Transforms transforms = new Transforms(doc);
        try {
            transforms.addTransform(
                    Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        } catch (TransformationException te1) {
            throw new SAML2Exception(te1);
        }
        try {
            transforms.addTransform(transformAlg);
        } catch (TransformationException te2) {
            throw new SAML2Exception(te2);
        }
        String ref = "#" + idValue;
        try {
            sig.addDocument(ref, transforms, signingConfig.getDigestMethod());
        } catch (XMLSignatureException sige1) {
            throw new SAML2Exception(sige1);
        }
        if (signingConfig.getCertificate() != null) {
            try {
                sig.addKeyInfo(signingConfig.getCertificate());
            } catch (XMLSecurityException xse3) {
                throw new SAML2Exception(xse3);
            }
        }
        try {
            sig.sign(signingConfig.getSigningKey());
        } catch (XMLSignatureException sige2) {
            throw new SAML2Exception(sige2);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(
                    classMethod +
                            "Signing is successful.");
        }
        return sig.getElement();
    }

    @Override
    public boolean verify(String xmlString, String idValue, Set<X509Certificate> verificationCerts)
            throws SAML2Exception {
        return verify(xmlString, SAML2Constants.ID, idValue, verificationCerts);
    }

    @Override
    public boolean verify(String xmlString, String idAttribute, String idValue, Set<X509Certificate> verificationCerts)
            throws SAML2Exception {
        Reject.ifNull(idAttribute);
        String classMethod = "FMSigProvider.verify: ";
        if (StringUtils.isEmpty(xmlString) || StringUtils.isEmpty(idValue)) {
            logger.error("{}Either input xmlString or idValue is null.", classMethod);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("nullInput"));
        }
        Document doc = XMLUtils.toDOMDocument(xmlString);
        if (doc == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        if (CollectionUtils.isEmpty(verificationCerts)) {
            logger.debug("{}No certificates provided - certificates have to be read from document", classMethod);
        }
        Element nscontext = XMLUtils.createDSctx(doc, "ds", Constants.SignatureSpecNS);
        Element sigElement;
        try {
            sigElement = (Element) XPathAPI.selectSingleNode(doc, "/*/ds:Signature", nscontext);
        } catch (XPathException te) {
            throw new SAML2Exception(te);
        }
        Element reference;
        try {
            reference = (Element) XPathAPI.selectSingleNode(doc, "/*/ds:Signature/ds:SignedInfo/ds:Reference",
                    nscontext);
        } catch (XPathException te) {
            throw new SAML2Exception(te);
        }
        String refUri = reference.getAttribute("URI");
        String signedId = ((Element) sigElement.getParentNode()).getAttribute(idAttribute);
        if (StringUtils.isEmpty(refUri) || !idValue.equals(signedId) || !refUri.substring(1).equals(signedId)) {
            logger.error("{}Signature reference ID does not match with element ID", classMethod);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString("uriNoMatchWithId"));
        }

        doc.getDocumentElement().setIdAttribute(idAttribute, true);
        XMLSignature signature;
        try {
            signature = new XMLSignature(sigElement, "");
        } catch (XMLSecurityException xse) {
            throw new SAML2Exception(xse);
        }
        KeyInfo ki = signature.getKeyInfo();
        X509Certificate certToUse = null;
        if (ki != null && ki.containsX509Data()) {
            try {
                certToUse = ki.getX509Certificate();
            } catch (KeyResolverException kre) {
                logger.error("{}Could not obtain a certificate from inside the document.", classMethod);
                certToUse = null;
            }
            if (certToUse != null) {
                if (checkCert) {
                    if ((CollectionUtils.isNotEmpty(verificationCerts)) && !verificationCerts.contains(certToUse)) {
                        logger.error("{}The cert contained in the document is NOT trusted. {}",
                            classMethod, SAML2Utils.getDebugInfoFromCertificate(certToUse));
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString("invalidCertificate"));
                    }
                    logger.debug("{}The cert contained in the document is trusted", classMethod);
                } else {
                    logger.error("{}The cert contained in the document has NOT been checked, " +
                            "com.sun.identity.saml.checkcert should be enabled. {}",
                        classMethod, SAML2Utils.getDebugInfoFromCertificate(certToUse));
                }
            }
        }

        if (certToUse != null) {
            verificationCerts = Collections.singleton(certToUse);
        }

        if (!isValidSignature(signature, verificationCerts)) {
            logger.error("{}Signature verification failed.", classMethod);
            return false;
        }

        logger.debug("{}Signature verification successful.", classMethod);
        return true;
    }

    private boolean isValidSignature(XMLSignature signature, Set<X509Certificate> certificates) throws SAML2Exception {
        final String classMethod = "FMSigProvider.isValidSignature: ";
        XMLSignatureException firstException = null;
        for (X509Certificate certificate : certificates) {
            if (!SAML2Utils.validateCertificate(certificate)) {
                logger.error("{} Signing Certificate is validated as bad.{}"
                        , classMethod
                        , SAML2Utils.getDebugInfoFromCertificate(certificate));
            } else {
                try {
                    logger.debug("{} Using cert to validate signature {}", classMethod,
                            SAML2Utils.getDebugInfoFromCertificate(certificate));

                    if (isEcdsaSignature(signature.getSignedInfo().getSignatureAlgorithm())
                            && certificate.getPublicKey() instanceof ECPublicKey) {
                        byte[] sigBytes = signature.getSignatureValue();
                        ECParameterSpec params= ((ECPublicKey) certificate.getPublicKey()).getParams();
                        try {
                            sanityCheckRawEcdsaSignatureValue(sigBytes, params);
                        } catch (SignatureException e) {
                            throw new XMLSignatureException(e);
                        }
                    }

                    if (signature.checkSignatureValue(certificate)) {
                        return true;
                    }
                } catch (XMLSignatureException xse) {
                    logger.warn(classMethod + "XML signature validation failed due to " + xse);
                    if (firstException == null) {
                        firstException = xse;
                    }
                }
            }
        }
        if (firstException != null) {
            throw new SAML2Exception(firstException);
        }

        return false;
    }

    private static boolean isEcdsaSignature(SignatureAlgorithm algorithm) {
        switch (algorithm.getURI()) {
        case XMLSignature.ALGO_ID_SIGNATURE_ECDSA_RIPEMD160:
        case XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA1:
        case XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA224:
        case XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA256:
        case XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA384:
        case XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA512:
            return true;
        default:
            return false;
        }
    }
}
