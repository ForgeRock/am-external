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
 * Copyright 2018-2020 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the FIDO U2F format attestation.
 */
public class FidoU2fVerifier extends TrustableAttestationVerifier {

    private final Logger logger = LoggerFactory.getLogger(FidoU2fVerifier.class);

    /**
     * The constructor.
     *
     * @param validator the verifier containing appropriate certs.
     */
    public FidoU2fVerifier(TrustAnchorValidator validator) {
        super(validator);
    }

    /**
     * Verify the attestation using specific steps defined for the FIDO U2F attestation format.
     *
     * @param attestationObject the attestation object.
     * @param clientDataHash the hash of the client data.
     * @return a {@link VerificationResponse} reporting the validity of the attestation.
     */
    @Override
    public VerificationResponse verify(AttestationObject attestationObject, byte[] clientDataHash) {

        if (attestationObject.attestationStatement.getAttestnCerts().size() != 1) {
            return VerificationResponse.failure();
        }

        // verify cert
        X509Certificate cert = attestationObject.attestationStatement.getAttestnCerts().get(0);

        // verify cert type and details
        if (!"EC".equals(cert.getPublicKey().getAlgorithm())) {
            return VerificationResponse.failure();
        }

        // extract algorithm used
        String alg = attestationObject.authData.attestedCredentialData.algorithm;

        // calculate rpIdHash
        byte[] rpIdHash = attestationObject.authData.rpIdHash;
        byte[] credentialId = attestationObject.authData.attestedCredentialData.credentialId;
        JWK credentialPublicKey = attestationObject.authData.attestedCredentialData.publicKey;
        EcJWK ecJwk = EcJWK.parse(credentialPublicKey.toJsonValue());

        // extract public key
        byte[] publicKeyU2F = getKeyBytes(ecJwk);

        // construct verification data
        ByteBuffer buffer = ByteBuffer.allocate(rpIdHash.length
                + clientDataHash.length
                + credentialId.length
                + publicKeyU2F.length + 1);
        buffer.put((byte) 0x00);
        buffer.put(rpIdHash);
        buffer.put(clientDataHash);
        buffer.put(credentialId);
        buffer.put(publicKeyU2F);
        byte[] verificationData = buffer.array();

        // verify sig using public key
        boolean isValid;
        try {
            Signature ecdsaSign = Signature.getInstance(alg);
            ecdsaSign.initVerify(cert);
            ecdsaSign.update(verificationData);
            isValid = ecdsaSign.verify(attestationObject.attestationStatement.getSig());
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("error occurred when attempting to verify webauthn attestation signature", e);
            isValid = false;
        }

        AttestationType type = validator.getAttestationType(attestationObject.attestationStatement.getAttestnCerts());
        return new VerificationResponse(type, isValid, cert);
    }

    private byte[] getKeyBytes(EcJWK keyData) {
        byte[] x = Base64url.decode(keyData.getX());
        byte[] y = Base64url.decode(keyData.getY());

        ByteBuffer buffer = ByteBuffer.allocate(x.length + y.length + 1);
        buffer.put((byte) 0x04);
        buffer.put(x);
        buffer.put(y);
        return buffer.array();
    }
}
