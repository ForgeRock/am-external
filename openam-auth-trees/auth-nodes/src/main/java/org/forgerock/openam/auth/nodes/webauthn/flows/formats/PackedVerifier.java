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
 * Copyright 2018-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
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
import org.forgerock.openam.auth.nodes.webauthn.Aaguid;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.metadata.AuthenticatorDetails;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataException;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataService;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataServiceCheckStatus;
import org.forgerock.openam.auth.nodes.webauthn.metadata.fido.blob.AuthenticatorStatus;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functionality for verifying a
 * <a href="https://www.w3.org/TR/webauthn/#packed-attestation">'packed' format attestation</a>.
 */
public class PackedVerifier extends TrustableAttestationVerifier {

    private static final String FIDO_OID = "1.3.6.1.4.1.45724.1.1.4";

    private final Logger logger = LoggerFactory.getLogger(PackedVerifier.class);
    private final FlowUtilities flowUtilities;
    private final MetadataService metadataService;

    /**
     * Construct a new PackedVerifier.
     *
     * @param flowUtilities   utilities used in webauthn flows
     * @param validator       the validator containing appropriate certs used for verification
     * @param metadataService the metadata service to use during verification
     */
    public PackedVerifier(FlowUtilities flowUtilities, TrustAnchorValidator validator,
            MetadataService metadataService) {
        super(validator);
        this.flowUtilities = flowUtilities;
        this.metadataService = metadataService;
    }

    /**
     * Verify the attestation using specific steps defined for the Packed attestation format.
     *
     * @param attestationObject the attestation object
     * @param clientDataHash    the hash of the client data
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

            MetadataServiceCheckStatus mdsCheckStatus = performMetadataServiceCheck(attestationObject);
            if (mdsCheckStatus == MetadataServiceCheckStatus.FAILED) {
                logger.error("webauthn authentication attestation failed metadata check");
                return VerificationResponse.failure();
            }

            return performCertAttestation(attestationObject, clientDataHash, mdsCheckStatus);
        } else if (attestationStatement.getEcdaaKeyId() != null) {
            logger.error("webauthn authentication attestation attempted using ECDAA - an unsupported algorithm");
            return VerificationResponse.failure();
        }

        return performSelfAttestation(attestationObject, clientDataHash);
    }

    @VisibleForTesting
    MetadataServiceCheckStatus performMetadataServiceCheck(AttestationObject attestationObject) {
        try {
            AuthenticatorDetails authenticatorDetails = metadataService.determineAuthenticatorStatus(
                    attestationObject.authData.attestedCredentialData.aaguid,
                    attestationObject.attestationStatement.getAttestnCerts());

            if (authenticatorDetails == null) {
                // value will only be null when a metadata service is not configured - see NoOpMetadataService
                return MetadataServiceCheckStatus.NOT_APPLICABLE;
            } else if (!authenticatorDetails.getMaxCertificationStatus().equals(AuthenticatorStatus.REVOKED)) {
                return MetadataServiceCheckStatus.PASSED;
            }
        } catch (MetadataException e) {
            logger.warn(e.getMessage());
        }
        return MetadataServiceCheckStatus.FAILED;
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

    private VerificationResponse performCertAttestation(AttestationObject attestationObject,
            byte[] clientDataHash, MetadataServiceCheckStatus mdsCheckStatus) {
        X509Certificate cert = attestationObject.attestationStatement.getAttestnCerts().get(0);
        if (cert.getVersion() != 3) {
            return VerificationResponse.failure();
        }
        byte[] extensionBytes = cert.getExtensionValue(FIDO_OID);
        if (extensionBytes != null) {
            // N.B [https://www.w3.org/TR/webauthn-2/#sctn-packed-attestation-cert-requirements] The spec states that
            // the "1.3.6.1.4.1.45724.1.1.4" extension is only needed in certain circumstances, but there appears to be
            // no feasible way to check this. Instead, we should avoid throwing a NPE if not present.
            byte[] octetStrings = new byte[] {0x04, 0x12, 0x04, 0x10};
            if (!MessageDigest.isEqual(octetStrings, Arrays.copyOfRange(extensionBytes, 0, 4))) {
                logger.warn("octect string wrapping missing");
                return VerificationResponse.failure();
            }
            byte[] idFidoGenCeAaguidBytes = Arrays.copyOfRange(extensionBytes, 4, 20);
            Aaguid idFidoGenCeAaguid = new Aaguid(idFidoGenCeAaguidBytes);
            if (!idFidoGenCeAaguid.equals(attestationObject.authData.attestedCredentialData.aaguid)) {
                logger.warn("certified aaguid doesn't match");
                return VerificationResponse.failure();
            }
        }

        boolean isSignatureValid = isSignatureValid(attestationObject, clientDataHash, cert.getPublicKey());
        AttestationType type;
        if (mdsCheckStatus == MetadataServiceCheckStatus.PASSED) {
            type = AttestationType.CA;
        } else {
            type = validator.getAttestationType(attestationObject.attestationStatement.getAttestnCerts());
        }
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
