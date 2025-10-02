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
 * Copyright 2020-2025 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm;

import static org.forgerock.openam.shared.security.crypto.SignatureSecurityChecks.sanityCheckDerEncodedEcdsaSignatureValue;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x509.GeneralName;
import org.forgerock.json.jose.jwk.KeyType;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationType;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.TrustableAttestationVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.VerificationResponse;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.exceptions.InvalidTpmsAttestException;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.exceptions.InvalidTpmtPublicException;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.util.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functionality for verifying a 'TPM' (Trusted Platform Module) format attestation.
 * https://www.w3.org/TR/webauthn/#tpm-attestation.
 *
 * Note that this IN NO WAY attempts to replicate all of the features of TPM, or adds support for
 * TPM. Rather, it only implements the specific features necessary to perform attestation for the
 * WebAuthn process.
 *
 * {@see https://trustedcomputinggroup.org/wp-content/uploads/TPM-Rev-2.0-Part-2-Structures-01.38.pdf}
 */
public class TpmVerifier extends TrustableAttestationVerifier {

    private static final int TPM_GENERATED_MAGIC_VALUE = 0xFF544347;
    private static final String SUBJECT_ALTERNATIVE_NAME = "2.5.29.17";
    private static final String TPM_MANUFACTURER_OID = "2.23.133.2.1";
    private static final String TC_KKP_AIK_CERTIFICATE_OID = "2.23.133.8.3";
    private static final String ID_FIDO_GEN_CE_AAGUID = "1.3.6.1.4.1.45724.1.1.4";

    private final Logger logger = LoggerFactory.getLogger(TpmVerifier.class);

    /**
     * Constructor taking the trustable attestation validator configured with appropriate certs for this instance.
     *
     * @param validator the validator containing appropriate certs.
     */
    public TpmVerifier(TrustAnchorValidator validator) {
        super(validator);
    }

    /**
     * Verify the attestation using specific steps defined for the TPM attestation format.
     *
     * @param attestationObject the attestation object.
     * @param clientDataHash the hash of the client data.
     * @return a {@link VerificationResponse} reporting the validity of the attestation.
     */
    @Override
    public VerificationResponse verify(AttestationObject attestationObject, byte[] clientDataHash) {

        TpmtPublic tpmtPublic;
        TpmsAttest tpmCertInfo;

        try {
            tpmtPublic = TpmtPublic.toTpmtPublic(attestationObject.attestationStatement.getPubArea());
            tpmCertInfo = TpmsAttest.toTpmsAttest(attestationObject.attestationStatement.getCertInfo());
        } catch (InvalidTpmsAttestException e) {
            logger.error("unable to parse tpmsAttest from the certInfo");
            return VerificationResponse.failure();
        } catch (InvalidTpmtPublicException e) {
            logger.error("unable to parse tpmtPublic from the pubArea");
            return VerificationResponse.failure();
        }

        if (!attestationObject.attestationStatement.getVer().equals("2.0")) {
            logger.error("attestation statement claims an invalid version of tpm spec, must be 2.0, is {}",
                    attestationObject.attestationStatement.getVer());
            return VerificationResponse.failure();
        }

        // Concatenate authenticatorData and clientDataHash to form attToBeSigned.
        ByteBuffer buffer = ByteBuffer.allocate(attestationObject.authData.rawAuthenticatorData.length
                + clientDataHash.length);
        buffer.put(attestationObject.authData.rawAuthenticatorData);
        buffer.put(clientDataHash);
        byte[] attToBeSigned = buffer.array();

        // Verify that the public key specified by the parameters and unique fields of pubArea is identical to the
        // credentialPublicKey in the attestedCredentialData in authenticatorData.
        if (!tpmtPublic.typeVerifier.verify(attestationObject.authData.attestedCredentialData.publicKey,
                tpmtPublic.unique)) {
            logger.error("public key in the pubArea is not identical to the public key in the authData");
            return VerificationResponse.failure();
        }

        if (tpmCertInfo.magic != TPM_GENERATED_MAGIC_VALUE) {
            logger.error("invalid certInfo tpm magic value, must indicate fully generated (TPM_GENERATED_VALUE)");
            return VerificationResponse.failure();
        }

        if (tpmCertInfo.type != TpmSt.TPM_ST_ATTEST_CERTIFY) {
            logger.error("invalid certInfo type, must be TPM_ST_ATTEST_CERTIFY");
            return VerificationResponse.failure();
        }

        // Verify that extraData is set to the hash of attToBeSigned using the hash algorithm employed in "alg".
        if (!verifyHash(attToBeSigned, attestationObject.attestationStatement.getAlg(), tpmCertInfo.extraData)) {
            logger.error("unable to verify hash of attToBeSigned using the algorithm specified");
            return VerificationResponse.failure();
        }

        // Verify that attested contains a TPMS_CERTIFY_INFO structure whose name field contains a valid Name for
        // pubArea, as computed using the algorithm in the nameAlg field of pubArea
        if (!verifyHash(attestationObject.attestationStatement.getPubArea(), tpmCertInfo.attested.nameAlg.getCoseAlg(),
                tpmCertInfo.attested.name)) {
            logger.error("unable to verify hash of pubArea against the attested name");
            return VerificationResponse.failure();
        }

        if (attestationObject.attestationStatement.getAttestnCerts().size() < 1) {
            logger.error("ECDAA currently unsupported");
            return VerificationResponse.failure();
        }

        return verifyX5c(attestationObject, attestationObject.attestationStatement.getCertInfo());
    }

    @VisibleForTesting
    VerificationResponse verifyX5c(AttestationObject attestationObject, byte[] certInfo) {
        //must match basic cert requirements https://w3c.github.io/webauthn/#sctn-tpm-cert-requirements
        X509Certificate cert = attestationObject.attestationStatement.getAttestnCerts().get(0);

        //must be version 3
        if (cert.getVersion() != 3) {
            logger.error("certificate version must be equal to 3");
            return VerificationResponse.failure();
        }

        //must have empty subject
        if (!cert.getSubjectDN().getName().isEmpty()) {
            logger.error("certificate subject must be empty");
            return VerificationResponse.failure();
        }

        // must have critical extension subjectAltName which must conform to
        // https://www.trustedcomputinggroup.org/wp-content/uploads/Credential_Profile_EK_V2.0_R14_published.pdf
        if (!cert.getCriticalExtensionOIDs().contains(SUBJECT_ALTERNATIVE_NAME)) {
            logger.error("certificate does not contain the required critical extension SubjectAlternativeName");
            return VerificationResponse.failure();
        } else {
            // value MUST be found within the registry located at
            // https://trustedcomputinggroup.org/wp-content/uploads/Vendor_ID_Registry_0-8_clean.pdf
            if (!verifySubjectAlternateNameExtension(cert)) {
                logger.error("Unable to verify subject alternate name extension matches an expected manufacturer");
                return VerificationResponse.failure();
            }
        }

        //must have extension extKeyUsage which must contain the Tcg-kp-AIKCertificate OID
        if (!verifyExtKeyUsageValue(cert)) {
            logger.error("Unable to verify the extKeyUsage contains Tcg-kp-AIKCertificate OID");
            return VerificationResponse.failure();
        }

        // If certificate contains id-fido-gen-ce-aaguid(1.3.6.1.4.1.45724.1.1.4) extension,
        // check that its value is set to the same AAGUID as in authData.
        if (!verifyAaguidExtension(cert, attestationObject.authData.attestedCredentialData.aaguid)) {
            logger.error("Unable to verify the aaguid extension matches the AAGUID in authData");
            return VerificationResponse.failure();
        }

        // Verify the sig is a valid signature over certInfo using the attestation public key in aikCert with the
        // algorithm specified in alg.
        CoseAlgorithm alg = attestationObject.attestationStatement.getAlg();
        byte[] sig = attestationObject.attestationStatement.getSig();

        if (alg.getKeyType() == KeyType.EC) {
            try {
                ECParameterSpec curveParams = ((ECPublicKey) cert.getPublicKey()).getParams();
                sanityCheckDerEncodedEcdsaSignatureValue(sig, curveParams);
            } catch (SignatureException e) {
                logger.error("ECDSA Signature value is invalid");
                return VerificationResponse.failure();
            }
        }

        // verify sig using public key
        boolean isValid;
        try {
            Signature signer = Signature.getInstance(alg.getExactAlgorithmName());
            signer.initVerify(cert);
            signer.update(certInfo);
            isValid = signer.verify(sig);
        } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
            logger.error("error occurred when attempting to verify webauthn attestation signature", e);
            return VerificationResponse.failure();
        }

        AttestationType type = validator.getAttestationType(attestationObject.attestationStatement.getAttestnCerts());
        return new VerificationResponse(type, isValid, cert);
    }

    private boolean verifyHash(byte[] toHash, CoseAlgorithm alg, byte[] claimedHash) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(alg.getHashAlg());
        } catch (NoSuchAlgorithmException e) {
            logger.error("unable to locate hashing algorithm with name {}", alg.getHashAlg());
            return false;
        }
        byte[] encodedHash = digest.digest(toHash);
        return MessageDigest.isEqual(encodedHash, claimedHash);
    }

    private boolean verifySubjectAlternateNameExtension(X509Certificate cert) {
        logger.debug("Validating certificate's alternative names against validated list of TPM manufacturers.");
        Collection<List<?>> altNames;

        try {
            altNames = cert.getSubjectAlternativeNames();
        } catch (CertificateParsingException e) {
            logger.warn("Unable to read certificate's subject alternative names.", e);
            return false;
        }

        if (altNames == null) {
            logger.warn("Certificate's subject alternative provided was null");
            return false;
        }

        try {
            for (List<?> altName : altNames) {
                Integer type = (Integer) altName.get(0);
                if (type == 4) { // 4 is 'directory Name'
                    // elements of this sequence can be lists themselves
                    DERSequence asn1Encodables = (DERSequence) (new GeneralName(4, (String) altName.get(1)))
                            .getName().toASN1Primitive();

                    for (int i = 0; i < asn1Encodables.size(); i++) {
                        ASN1Encodable val = asn1Encodables.getObjectAt(i);
                        // Iterate if this element is actually a list in itself
                        if (((RDN) val).isMultiValued()) {
                            for (AttributeTypeAndValue typeAndValue : ((RDN) val).getTypesAndValues()) {
                                if (typeAndValue.getType().getId().equals(TPM_MANUFACTURER_OID)) {
                                    TpmManufacturer manufacturer = TpmManufacturer.getTpmManufacturer(
                                            ((DERUTF8String) typeAndValue.getValue()).getString());
                                    if (manufacturer != null) {
                                        logger.debug("Manufacturer type " + manufacturer.getName());
                                        return true;
                                    }
                                }
                            }
                        } else {
                            DERSet set = (DERSet) asn1Encodables.getObjectAt(i).toASN1Primitive();
                            DERSequence seq2 = (DERSequence) set.getObjectAt(0).toASN1Primitive();
                            ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) seq2.getObjectAt(0).toASN1Primitive();
                            if (oid.getId().equals(TPM_MANUFACTURER_OID)) {
                                DERUTF8String value = (DERUTF8String) seq2.getObjectAt(1).toASN1Primitive();
                                TpmManufacturer manufacturer = TpmManufacturer.getTpmManufacturer(value.getString());
                                if (manufacturer != null) {
                                    logger.debug("Manufacturer type " + manufacturer.getName());
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        } catch (ClassCastException cce) {
            logger.error("Unable to read the set of alternative names correctly.");
            return false;
        }

        logger.error("SAN was not an expected TPM manufacturer.");
        return false;
    }

    private boolean verifyExtKeyUsageValue(X509Certificate cert) {
        List<String> extKeyUsages;
        try {
            extKeyUsages = cert.getExtendedKeyUsage();
        } catch (CertificateParsingException e) {
            logger.error("Certificate extended uses unable to be read.");
            return false;
        }

        return extKeyUsages.contains(TC_KKP_AIK_CERTIFICATE_OID);
    }

    private boolean verifyAaguidExtension(X509Certificate cert, byte[] authDataAaguid) {
        byte[] idFidoGenCeAaguidValue = cert.getExtensionValue(ID_FIDO_GEN_CE_AAGUID);
        if (idFidoGenCeAaguidValue == null) {
            logger.warn("Certificate provides no AAGUID value in the appropriate extension.");
            return true;
        }

        return Arrays.equals(idFidoGenCeAaguidValue, authDataAaguid);
    }

}
