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
 * Copyright 2021-2022 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;

import javax.net.ssl.SSLException;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.webtoken.JsonWebSignature;

/**
 * A representation of https://www.w3.org/TR/webauthn/#android-safetynet-attestation.
 */
public class AndroidSafetyNetVerifier implements AttestationVerifier {

    private static final DefaultHostnameVerifier HOSTNAME_VERIFIER = new DefaultHostnameVerifier();
    private static final String ATTEST_ANDROID_COM = "attest.android.com";
    private static final String CTS_PROFILE_MATCH = "ctsProfileMatch";
    private static final String NONCE = "nonce";
    private static final String SHA_256 = "SHA-256";
    private final Logger logger;
    private static final Provider PROVIDER = new BouncyCastleProvider();

    /**
     * Construct a new instance of {@link AndroidSafetyNetVerifier}.
     *
     * @param logger The logger to log any error and debug messages to.
     */
    @VisibleForTesting
    public AndroidSafetyNetVerifier(Logger logger) {
        this.logger = logger;
    }

    /**
     * Construct a new instance of {@link AndroidSafetyNetVerifier}.
     */
    public AndroidSafetyNetVerifier() {
        this(LoggerFactory.getLogger(AndroidSafetyNetVerifier.class));
    }

    @Override
    public VerificationResponse verify(AttestationObject attestationObject, byte[] clientDataHash) {

        try {
            verifyVersion(attestationObject);
            JsonWebSignature jws = parseJWS(attestationObject);
            X509Certificate cert = verifySignature(jws);
            verifyHostname(cert);
            verifyIntegrity(attestationObject, clientDataHash, jws);
            verifyCtsProfileMatch(jws);
            //Success
            return new VerificationResponse(AttestationType.BASIC, true, cert);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return failure();
        }
    }

    /**
     * Verify that response is a valid SafetyNet response of version ver.
     *
     * @param attestationObject The attestation object.
     */
    protected void verifyVersion(AttestationObject attestationObject) {
        String version = attestationObject.attestationStatement.getVer();

        //Verify that response is a valid SafetyNet response of version ver.
        if (version == null) {
            throw new IllegalArgumentException("Missing Version");
        }
    }

    /**
     * Parse the response as JWS (https://www.w3.org/TR/webauthn/#android-safetynet-attestation).
     * The response is UTF-8 encoded result of the getJwsResult() call of the SafetyNet API.
     * This value is a JWS [RFC7515] object (see SafetyNet online documentation) in Compact Serialization.
     *
     * @param attestationObject The attestation object.
     * @return The SafetyNet Json Web Signature.
     */
    protected JsonWebSignature parseJWS(AttestationObject attestationObject) {
        try {
            return JsonWebSignature.parser(GsonFactory.getDefaultInstance())
                    .parse(new String(attestationObject.attestationStatement.getResponse(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse the attestation statement response as a valid JWS", e);
        }
    }

    /**
     * Verify the JWS Signature.
     *
     * @param jws The Json Web Signature (https://developer.android.com/training/safetynet/attestation).
     * @return The Signing Certificate.
     */
    protected X509Certificate verifySignature(JsonWebSignature jws) {
        try {
            X509Certificate cert = jws.verifySignature();
            if (cert == null) {
                throw new CertificateException("Invalid Cert");
            }
            return cert;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to verify JWS signature", e);
        }
    }

    /**
     * Verify that attestationCert is issued to the hostname "attest.android.com".
     *
     * @param certificate The Certificate.
     */
    protected void verifyHostname(X509Certificate certificate) {
        try {
            HOSTNAME_VERIFIER.verify(ATTEST_ANDROID_COM, certificate);
        } catch (SSLException e) {
            throw new IllegalArgumentException("Host verification failed", e);
        }
    }


    /**
     * Verify that the nonce in the response is identical to the Base64 encoding of the SHA-256 hash
     * of the concatenation of authenticatorData and clientDataHash.
     *
     * @param attestationObject The Attestation Object.
     * @param clientDataHash    The hash of the client data.
     * @param jws               The response Json Web Signature.
     */
    protected void verifyIntegrity(AttestationObject attestationObject, byte[] clientDataHash, JsonWebSignature jws) {
        try {
            byte[] signedData = new byte[attestationObject.authData.rawAuthenticatorData.length
                    + clientDataHash.length];
            System.arraycopy(attestationObject.authData.rawAuthenticatorData, 0,
                    signedData, 0, attestationObject.authData.rawAuthenticatorData.length);
            System.arraycopy(clientDataHash, 0,
                    signedData, attestationObject.authData.rawAuthenticatorData.length, clientDataHash.length);

            byte[] signedDataHash = MessageDigest.getInstance(SHA_256, PROVIDER).digest(signedData);
            String nonce = (String) jws.getPayload().get(NONCE);
            int result = Arrays.compare(signedDataHash, Base64.getDecoder().decode(nonce));
            if (result != 0) {
                throw new IllegalArgumentException("Android SafetyNet attestation - "
                        + "The response is not identical to the Base64 encoding of the SHA-256 hash"
                        + " of the concatenation of authenticatorData and clientDataHash.");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Android SafetyNet attestation - "
                    + "SHA-256 hash is not supported", e);
        }
    }

    /**
     * Verify that the ctsProfileMatch attribute in the payload of response is true.
     *
     * @param jws The Json Web Signature.
     */
    protected void verifyCtsProfileMatch(JsonWebSignature jws) {
        if (!(Boolean) jws.getPayload().get(CTS_PROFILE_MATCH)) {
            throw new IllegalArgumentException("Android SafetyNet attestation - ctsProfileMatch is False");
        }
    }

    /**
     * Returns a default failed response.
     *
     * @return a default failed response.
     */
    private VerificationResponse failure() {
        return new VerificationResponse(AttestationType.BASIC, false, null);
    }
}
