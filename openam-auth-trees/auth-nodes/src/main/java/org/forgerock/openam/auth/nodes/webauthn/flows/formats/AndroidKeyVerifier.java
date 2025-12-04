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
 * Copyright 2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.formats;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functionality for verifying an 'android-key' format attestation.
 * <a href="https://www.w3.org/TR/webauthn-2/#sctn-android-key-attestation">...</a>
 */
public class AndroidKeyVerifier extends TrustableAttestationVerifier {

    private final Logger logger = LoggerFactory.getLogger(AndroidKeyVerifier.class);
    private final FlowUtilities flowUtilities;

    private static final String ANDROID_KEY_EXTENSION_DATA_OID = "1.3.6.1.4.1.11129.2.1.17";

    // The ASN.1 structure of the android-key attestation extension is defined here:
    // https://source.android.com/docs/security/features/keystore/attestation#schema
    // The following constants are to aid with parsing the attestation extension structure.

    // KeyDescription Sequence Indexes
    private static final int ATTESTATION_CHALLENGE_SEQUENCE_INDEX = 4;
    private static final int SOFTWARE_ENFORCED_AUTH_LIST_SEQUENCE_INDEX = 6;
    private static final int HARDWARE_ENFORCED_AUTH_LIST_SEQUENCE_INDEX = 7;

    // AuthorizationList Tags
    private static final int PURPOSE_TAG = 1;
    private static final int ORIGIN_TAG = 702;
    private static final int ALL_APPLICATIONS_TAG = 600;

    // Keymaster Enum Values
    private static final int KM_ORIGIN_GENERATED = 0;
    private static final int KM_PURPOSE_SIGN = 2;


    /**
     * The constructor.
     *
     * @param validator     the verifier containing appropriate certs
     * @param flowUtilities utilities used in webauthn flows
     */
    public AndroidKeyVerifier(TrustAnchorValidator validator, FlowUtilities flowUtilities) {
        super(validator);
        this.flowUtilities = flowUtilities;
    }

    /**
     * Verify the attestation according to the verification procedure specified for the Android Key attestation format.
     * <a href="https://www.w3.org/TR/webauthn-2/#sctn-android-key-attestation">...</a>
     *
     * @param attestationObject the provided attestation object
     * @param clientDataHash    the hash of the client data
     * @return a {@link VerificationResponse} reporting the validity of the attestation
     */
    @Override
    public VerificationResponse verify(AttestationObject attestationObject, byte[] clientDataHash) {
        try {
            List<X509Certificate> certs = attestationObject.attestationStatement.getAttestnCerts();
            if (certs.isEmpty()) {
                logger.error("No attestation certificate found");
                return VerificationResponse.failure();
            }

            // Verify that sig is a valid signature over the concatenation of authenticatorData and clientDataHash
            // using the public key in the first certificate in x5c with the algorithm specified in alg.
            if (!verifySignature(attestationObject, clientDataHash)) {
                logger.error("Signature verification failed");
                return VerificationResponse.failure();
            }

            // Verify that the public key in the first certificate in x5c matches the credentialPublicKey in the
            // attestedCredentialData in authenticatorData.
            X509Certificate credCert = certs.get(0);
            PublicKey publicKeyFromCert = credCert.getPublicKey();
            PublicKey publicKeyFromAuthData =
                    flowUtilities.getPublicKeyFromJWK(attestationObject.authData.attestedCredentialData.publicKey);

            if (publicKeyFromAuthData == null) {
                logger.error("Could not extract public key from authenticator data");
                return VerificationResponse.failure();
            }

            if (!Arrays.equals(publicKeyFromCert.getEncoded(), publicKeyFromAuthData.getEncoded())) {
                logger.error("Public key mismatch between certificate and credential data");
                return VerificationResponse.failure();
            }

            if (!verifyExtensionData(credCert, clientDataHash)) {
                logger.error("Attestation challenge verification failed");
                return VerificationResponse.failure();
            }

            return new VerificationResponse(AttestationType.BASIC, true, credCert);
        } catch (Exception e) {
            logger.error("An unexpected error occurred during Android Key verification", e);
            return VerificationResponse.failure();
        }
    }

    @VisibleForTesting
    boolean verifySignature(AttestationObject attestationObject, byte[] clientDataHash) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(attestationObject.authData.rawAuthenticatorData.length
                    + clientDataHash.length);
            buffer.put(attestationObject.authData.rawAuthenticatorData);
            buffer.put(clientDataHash);
            byte[] verificationData = buffer.array();

            String alg = attestationObject.attestationStatement.getAlg().getExactAlgorithmName();
            Signature signature = Signature.getInstance(alg);
            signature.initVerify(attestationObject.attestationStatement.getAttestnCerts().get(0).getPublicKey());
            signature.update(verificationData);

            return signature.verify(attestationObject.attestationStatement.getSig());
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.error("Error during signature validation", e);
            return false;
        }
    }

    private boolean verifyExtensionData(X509Certificate cert, byte[] clientDataHash) {
        byte[] extensionBytes = cert.getExtensionValue(ANDROID_KEY_EXTENSION_DATA_OID);
        if (extensionBytes == null) {
            logger.error("Certificate provides no data in the android key extension.");
            return false;
        }

        try {
            ASN1Sequence extensionSequence = getAsn1SequenceFromExtensionValue(extensionBytes);

            if (!verifyExtensionChallenge(clientDataHash, extensionSequence)) {
                logger.error("Attestation challenge does not match client data hash");
                return false;
            }

            if (!verifyExtensionAuthLists(extensionSequence)) {
                logger.error("Authorization list verification failed");
                return false;
            }

            return true;
        } catch (IOException e) {
            logger.error("Failed to parse attestation certificate extension", e);
            return false;
        }
    }

    private boolean verifyExtensionChallenge(byte[] clientDataHash, ASN1Sequence extensionSequence) {
        // Verify that the attestationChallenge field in the attestation certificate extension data is securely
        // bound to the clientDataHash.
        ASN1OctetString challengeOctetString =
                ASN1OctetString.getInstance(extensionSequence.getObjectAt(ATTESTATION_CHALLENGE_SEQUENCE_INDEX));
        return Arrays.equals(clientDataHash, challengeOctetString.getOctets());
    }

    private boolean verifyExtensionAuthLists(ASN1Sequence extensionSequence) {
        ASN1Sequence softwareEnforcedList =
                ASN1Sequence.getInstance(extensionSequence.getObjectAt(SOFTWARE_ENFORCED_AUTH_LIST_SEQUENCE_INDEX));
        ASN1Sequence hardwareEnforcedList =
                ASN1Sequence.getInstance(extensionSequence.getObjectAt(HARDWARE_ENFORCED_AUTH_LIST_SEQUENCE_INDEX));

        // For the following, use only the teeEnforced authorization list if the RP wants to accept only keys from
        // a trusted execution environment, otherwise use the union of teeEnforced and softwareEnforced.
        List<ASN1Sequence> combinedAuthLists = List.of(softwareEnforcedList, hardwareEnforcedList);

        // The AuthorizationList.allApplications field is not present on either authorization list (softwareEnforced
        // nor hardwareEnforced), since PublicKeyCredential MUST be scoped to the RP ID.
        if (getTagFromLists(combinedAuthLists, ALL_APPLICATIONS_TAG).findFirst().isPresent()) {
            logger.error("AuthorizationList: allApplications tag must not be present.");
            return false;
        }

        // The value in the AuthorizationList.origin field is equal to KM_ORIGIN_GENERATED.
        if (!checkOrigin(combinedAuthLists)) {
            logger.error("AuthorizationList: origin check failed.");
            return false;
        }

        // The value in the AuthorizationList.purpose field is equal to KM_PURPOSE_SIGN.
        if (!checkPurpose(combinedAuthLists)) {
            logger.error("AuthorizationList: purpose check failed.");
            return false;
        }

        return true;
    }

    private boolean checkOrigin(List<ASN1Sequence> authLists) {
        return getTagFromLists(authLists, ORIGIN_TAG)
                .map(t -> ASN1Integer.getInstance(t, true).getValue().intValue())
                .allMatch(origin -> origin == KM_ORIGIN_GENERATED);
    }

    private boolean checkPurpose(List<ASN1Sequence> authLists) {
        return getTagFromLists(authLists, PURPOSE_TAG)
                .map(t -> ASN1Set.getInstance(t, true))
                .flatMap(set -> StreamSupport.stream(set.spliterator(), false))
                .map(item -> ASN1Integer.getInstance(item).getValue().intValue())
                .allMatch(purpose -> purpose == KM_PURPOSE_SIGN);
    }

    private Stream<ASN1TaggedObject> getTagFromLists(List<ASN1Sequence> lists, int tag) {
        return lists.stream()
                .flatMap(list -> StreamSupport.stream(list.spliterator(), false))
                .filter(e -> e instanceof ASN1TaggedObject taggedObject && taggedObject.getTagNo() == tag)
                .map(e -> (ASN1TaggedObject) e);
    }

    private ASN1Sequence getAsn1SequenceFromExtensionValue(byte[] extensionValue) throws IOException {
        ASN1Primitive outerPrimitive = ASN1Primitive.fromByteArray(extensionValue);
        if (outerPrimitive instanceof ASN1OctetString octetString) {
            ASN1Primitive innerPrimitive = ASN1Primitive.fromByteArray(octetString.getOctets());
            if (innerPrimitive instanceof ASN1Sequence sequence) {
                return sequence;
            }
        }
        throw new IOException("Unexpected ASN.1 structure in extension");
    }
}
