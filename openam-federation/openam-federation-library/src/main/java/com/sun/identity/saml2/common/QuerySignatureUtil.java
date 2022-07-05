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
 * $Id: QuerySignatureUtil.java,v 1.2 2008/06/25 05:47:45 qcheng Exp $
 *
 * Portions Copyrighted 2015-2021 ForgeRock AS.
 */
package com.sun.identity.saml2.common;

import static org.forgerock.http.util.Uris.urlDecodeQueryParameterNameOrValue;
import static org.forgerock.http.util.Uris.urlEncodeQueryParameterNameOrValue;
import static org.forgerock.openam.shared.security.crypto.SignatureSecurityChecks.sanityCheckDerEncodedEcdsaSignatureValue;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.xml.security.algorithms.JCEMapper;
import org.forgerock.openam.federation.util.XmlSecurity;
import org.forgerock.openam.saml2.crypto.signing.SigningConfig;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.identity.shared.encode.Base64;

/**
 * The <code>QuerySignatureUtil</code> provides methods to
 * sign query string and to verify signature on query string
 */
public class QuerySignatureUtil {

    private static final Logger logger = LoggerFactory.getLogger(QuerySignatureUtil.class);
    private static final String SIGNATURE = "Signature";

    static {
        XmlSecurity.init();
    }

    private QuerySignatureUtil() {
    }

    /**
     * Signs the query string.
     * @param queryString Query String
     * @param signingConfig The signing configuration.
     * @return String signed query string
     * @exception SAML2Exception if the signing fails
     */
    public static String sign(String queryString, SigningConfig signingConfig) throws SAML2Exception {
        if (StringUtils.isEmpty(queryString)) {
            logger.error("Input query string was null.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullInput"));
        }
        logger.debug("Input query string:\n{}", queryString);

        Signature sig;
        try {
            sig = Signature.getInstance(JCEMapper.translateURItoJCEID(signingConfig.getSigningAlgorithm()));
        } catch (NoSuchAlgorithmException nsae) {
            throw new SAML2Exception(nsae);
        }

        if (queryString.charAt(queryString.length() - 1) != '&') {
            queryString = queryString + "&";
        }
        queryString += SAML2Constants.SIG_ALG + "="
                + urlEncodeQueryParameterNameOrValue(signingConfig.getSigningAlgorithm());
        logger.debug("Final string to be signed:\n{}", queryString);

        byte[] sigBytes;
        try {
            sig.initSign((PrivateKey) signingConfig.getSigningKey());
            sig.update(queryString.getBytes());
            sigBytes = sig.sign();
        } catch (GeneralSecurityException gse) {
            throw new SAML2Exception(gse);
        }

        if (sigBytes == null || sigBytes.length == 0) {
            logger.error("Generated signature is null");
            throw new SAML2Exception(SAML2Utils.bundle.getString("nullSigGenerated"));
        }
        String encodedSig = Base64.encode(sigBytes);
        queryString += "&" + SAML2Constants.SIGNATURE + "=" + urlEncodeQueryParameterNameOrValue(encodedSig);

        logger.debug("Signed query string:\n{}", queryString);
        return queryString;
    }
    
    /**
     * Verifies the query string signature.
     *
     * @param queryString Signed query String.
     * @param verificationCerts Verification certificates.
     * @return boolean whether the verification is successful or not.
     * @throws SAML2Exception if there is an error during verification.
     */
    public static boolean verify(
        String queryString, 
        Set<X509Certificate> verificationCerts
    ) throws SAML2Exception {
        
        String classMethod =
            "QuerySignatureUtil.verify: ";
        if (queryString == null ||
            queryString.length() == 0 || verificationCerts.isEmpty()) {
            
            logger.error(
                classMethod +
                "Input query string or certificate is null");       
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullInput"));
        }
        if (logger.isDebugEnabled()) {
            logger.debug(
                classMethod +
                "Query string to be verifed:\n" + queryString);
        }
        StringTokenizer st = new
            StringTokenizer(queryString, "&");
        String token = null;
        String samlReq = null;
        String samlRes = null;
        String relay = null;
        String sigAlg = null;
        String encSig = null;
        while (st.hasMoreTokens()) {
            token = st.nextToken();
            if (token.startsWith(SAML2Constants.SAML_REQUEST)) {
                samlReq=token;
            } else if (token.startsWith(SAML2Constants.SAML_RESPONSE)) {
                samlRes=token;
            } else if (token.startsWith(SAML2Constants.RELAY_STATE)) {
                relay=token;
            } else if (token.startsWith(SAML2Constants.SIG_ALG)) {
                sigAlg = token;
            } else if (token.startsWith(SAML2Constants.SIGNATURE)) {
                encSig = token;
            }
        }
        if (sigAlg == null || sigAlg.equals("")) {
            logger.error(
                classMethod +
                "Null SigAlg query parameter.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSigAlg"));         
        }
        if (encSig == null || encSig.equals("")) {
            logger.error(
                classMethod +
                "Null Signature query parameter.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSig"));            
        }       
        // The following manipulation is necessary because
        // other implementations could send the query
        // parameters out of order, i.e., not in the same
        // order when signature is produced
        String newQueryString = null;
        if (samlReq != null) {
            newQueryString = samlReq;
        } else {
            newQueryString = samlRes;
        }
        if (relay != null) {
            newQueryString += "&"+relay;
        }
        newQueryString += "&"+sigAlg;
        if (logger.isDebugEnabled()) {
            logger.debug(
                classMethod+
                "Query string to be verifed (re-arranged):\n" +
                newQueryString);
        }
        int sigAlgValueIndex = sigAlg.indexOf('=');
        String sigAlgValue =
            sigAlg.substring(sigAlgValueIndex+1);
        if (sigAlgValue == null || sigAlgValue.equals("")) {
            logger.error(
                classMethod +
                "Null SigAlg query parameter value.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSigAlg"));         
        }
        sigAlgValue = urlDecodeQueryParameterNameOrValue(sigAlgValue);
        if (logger.isDebugEnabled()) {
            logger.debug(
                classMethod +
                "SigAlg query parameter value: " +
                sigAlgValue);
        }
        int encSigValueIndex = encSig.indexOf('=');
        String encSigValue =
            encSig.substring(encSigValueIndex+1);
        if (encSigValue == null || encSigValue.equals("")) {
            logger.debug(
                classMethod +
                "Null Signature query parameter value.");
            throw new SAML2Exception( 
                SAML2Utils.bundle.getString("nullSig"));            
        }
        encSigValue = urlDecodeQueryParameterNameOrValue(encSigValue);
        if (logger.isDebugEnabled()) {
            logger.debug(
                classMethod +
                "Signature query parameter value:\n" +
                encSigValue);
        }
        // base-64 decode the signature value
        byte[] signature = null;
        Base64 decoder = new Base64();
        signature = decoder.decode(encSigValue);

        // get Signature instance based on algorithm
        if (!SIGNATURE.equals(JCEMapper.getAlgorithmClassFromURI(sigAlgValue))) {
            logger.error(classMethod + "Signature algorithm " + sigAlgValue + " is not supported.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("algNotSupported"));
        }

        Signature sig;
        try {
            sig = Signature.getInstance(JCEMapper.translateURItoJCEID(sigAlgValue));
        } catch (NoSuchAlgorithmException nsae) {
            throw new SAML2Exception(nsae);
        }
        return isValidSignature(sig, verificationCerts, newQueryString.getBytes(), signature);
    }

    @VisibleForTesting
    static boolean isValidSignature(Signature sig, Set<X509Certificate> certificates, byte[] queryString,
            byte[] signature) throws SAML2Exception {
        final String classMethod = "QuerySignatureUtil.isValidSignature: ";
        Exception firstException = null;
        for (X509Certificate certificate : certificates) {
            try {
             	logger.debug("{} using cert for signature verification {}"
						, classMethod
						, SAML2Utils.getDebugInfoFromCertificate(certificate));

                if (certificate.getPublicKey() instanceof ECPublicKey) {
                    ECParameterSpec curveParams = ((ECPublicKey) certificate.getPublicKey()).getParams();
                    sanityCheckDerEncodedEcdsaSignatureValue(signature, curveParams);
                }
                sig.initVerify(certificate);
                sig.update(queryString);
                if (sig.verify(signature)) {
                    return true;
                }
            } catch (InvalidKeyException | SignatureException ex) {
                logger.warn(classMethod + "Signature validation failed due to " + ex);
                if (firstException == null) {
                    firstException = ex;
                }
            }
        }
        if (firstException != null) {
            throw new SAML2Exception(firstException);
        }

        return false;
    }
}
