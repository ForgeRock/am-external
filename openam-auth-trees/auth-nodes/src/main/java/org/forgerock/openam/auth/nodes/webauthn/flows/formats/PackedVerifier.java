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
 * Copyright 2018-2023 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import static org.forgerock.openam.shared.security.crypto.SignatureSecurityChecks.sanityCheckDerEncodedEcdsaSignatureValue;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.Arrays;
import java.util.List;

import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functionality for verifying a 'packed' format attestation.
 * https://www.w3.org/TR/webauthn/#packed-attestation.
 */
public class PackedVerifier extends TrustableAttestationVerifier {

    private final Logger logger = LoggerFactory.getLogger(PackedVerifier.class);
    private static final String FIDO_OID = "1.3.6.1.4.1.45724.1.1.4";

    private final FlowUtilities flowUtilities;

    /**
     * Construct a new PackedVerifier.
     *
     * @param flowUtilities Utilities used in webauthn flows.
     * @param validator the validator containing appropriate certs.
     */
    public PackedVerifier(FlowUtilities flowUtilities, TrustAnchorValidator validator) {
        super(validator);
        this.flowUtilities = flowUtilities;
    }

    /**
     * Verify the attestation using specific steps defined for the Packed attestation format.
     *
     * @param attestationObject the attestation object.
     * @param clientDataHash the hash of the client data.
     * @return a {@link VerificationResponse} reporting the validity of the attestation.
     */
    @Override
    public VerificationResponse verify(AttestationObject attestationObject, byte[] clientDataHash) {
        AttestationStatement attestationStatement = attestationObject.attestationStatement;
        List<X509Certificate> attestnCerts = attestationStatement.getAttestnCerts();
        if (attestnCerts != null) {
            if (attestnCerts.isEmpty()) {
                logger.error("webauthn authentication attestation certificates could not be found");
                return VerificationResponse.failure();
            }
            return performCertAttestation(attestationObject, clientDataHash);
        } else if (attestationStatement.getEcdaaKeyId() != null) {
            logger.error("webauthn authentication attestation attempted using ECDAA - an unsupported algorithm");
            return VerificationResponse.failure();
        }
        return performSelfAttestation(attestationObject, clientDataHash);
    }

    private VerificationResponse performSelfAttestation(AttestationObject attestationObject, byte[] clientDataHash) {

        CoseAlgorithm coseAlg =
                CoseAlgorithm.fromCoseNumber(attestationObject.attestationStatement.getAlg().getCoseNumber());

        if (coseAlg == null) {
            return VerificationResponse.failure();
        }

        if (!attestationObject.authData.attestedCredentialData.algorithm.equals(coseAlg.getExactAlgorithmName())) {
            logger.warn("public key algorithm doesn't match attested algorithm");
            return VerificationResponse.failure();
        }

        PublicKey publicKey =
                flowUtilities.getPublicKeyFromJWK(attestationObject.authData.attestedCredentialData.publicKey);

        boolean isSignatureValid = isSignatureValid(attestationObject, clientDataHash, publicKey);
        return new VerificationResponse(AttestationType.SELF, isSignatureValid, null);
    }

    private VerificationResponse performCertAttestation(AttestationObject attestationObject, byte[] clientDataHash) {
        X509Certificate cert = attestationObject.attestationStatement.getAttestnCerts().get(0);
        if (cert.getVersion() != 3) {
            return VerificationResponse.failure();
        }
        byte[] extensionBytes = cert.getExtensionValue(FIDO_OID);
        byte[] octetStrings = new byte[]{ 0x04, 0x12, 0x04, 0x10 };
        if (!MessageDigest.isEqual(octetStrings, Arrays.copyOfRange(extensionBytes, 0, 4))) {
            logger.warn("octect string wrapping missing");
            return VerificationResponse.failure();
        }
        if (!MessageDigest.isEqual(attestationObject.authData.attestedCredentialData.aaguid,
                Arrays.copyOfRange(extensionBytes, 4, 20))) {
            logger.warn("certified aaguid doesn't match");
            return VerificationResponse.failure();
        }

        boolean isSignatureValid = isSignatureValid(attestationObject, clientDataHash, cert.getPublicKey());
        AttestationType type = validator.getAttestationType(attestationObject.attestationStatement.getAttestnCerts());
        return new VerificationResponse(type, isSignatureValid, cert);
    }

    @VisibleForTesting
    boolean isSignatureValid(AttestationObject attestationObject, byte[] clientDataHash, PublicKey publicKey) {
        ByteBuffer buffer = ByteBuffer.allocate(attestationObject.authData.rawAuthenticatorData.length
                + clientDataHash.length);
        buffer.put(attestationObject.authData.rawAuthenticatorData);
        buffer.put(clientDataHash);
        byte[] verificationData = buffer.array();

        try {
            CoseAlgorithm algorithm = attestationObject.attestationStatement.getAlg();
            byte[] sig = attestationObject.attestationStatement.getSig();
            if (algorithm.getKeyType() == KeyType.EC) {
                ECParameterSpec curveParams = ((ECPublicKey) publicKey).getParams();
                sanityCheckDerEncodedEcdsaSignatureValue(sig, curveParams);
            }

            Signature sign = Signature.getInstance(algorithm.getExactAlgorithmName());
            sign.initVerify(publicKey);
            sign.update(verificationData);
            return sign.verify(sig);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("failed to validate webauthn attestation signature", e);
            return false;
        }
    }
}
