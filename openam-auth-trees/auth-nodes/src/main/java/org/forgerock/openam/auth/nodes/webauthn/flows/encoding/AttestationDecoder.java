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
 * Copyright 2018-2021 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import static org.forgerock.openam.auth.nodes.webauthn.flows.CertificateFactory.createCerts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.forgerock.openam.auth.nodes.webauthn.AttestationPreference;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AndroidSafetyNetVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.FidoU2fVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.NoneVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.PackedVerifier;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmVerifier;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.UnicodeString;

/**
 * Class to decode the packed bytes of the authentication registration attestation response.
 * https://www.w3.org/TR/webauthn/#authenticatorattestationresponse
 */
public class AttestationDecoder {

    private static final Logger logger = LoggerFactory.getLogger(AttestationDecoder.class);

    private final NoneVerifier noneVerifier;
    private final AuthDataDecoder authDataDecoder;
    private final FlowUtilities flowUtilities;
    private final TrustAnchorUtilities trustAnchorUtilities;
    private final TrustAnchorValidator.Factory trustAnchorValidatorFactory;

    /**
     * Construct a new attestation decoder.
     *
     * @param trustAnchorUtilities utilities for the manipulation of secrets into trust anchors.
     * @param trustAnchorValidatorFactory generates trust anchor validators.
     * @param authDataDecoder authentication data decoder.
     * @param flowUtilities Utilities for webauthn.
     * @param noneVerifier the basic none verifier that requires no state input.
     */
    @Inject
    public AttestationDecoder(TrustAnchorUtilities trustAnchorUtilities,
                              TrustAnchorValidator.Factory trustAnchorValidatorFactory,
                              AuthDataDecoder authDataDecoder, FlowUtilities flowUtilities, NoneVerifier noneVerifier) {
        this.trustAnchorUtilities = trustAnchorUtilities;
        this.trustAnchorValidatorFactory = trustAnchorValidatorFactory;
        this.authDataDecoder = authDataDecoder;
        this.flowUtilities = flowUtilities;
        this.noneVerifier = noneVerifier;
    }

    /**
     * Decode the byte data, converting it into rich objects which can be reasoned about.
     *
     * @param attestationData the data as bytes.
     * @param attestationPreference the type of attestation the server expects
     * @param secretSource the source of the verification keys used during attestation cert validation if required.
     * @param enforceRevocationCheck whether to enforce the checking of CRL/OCSP revocation checks
     * @throws DecodingException if there's an error during decoding
     * @return the data as an AttestationObject.
     */
    public AttestationObject decode(byte[] attestationData, AttestationPreference attestationPreference,
                                    Promise<Stream<VerificationKey>, NeverThrowsException> secretSource,
                                    boolean enforceRevocationCheck)
            throws DecodingException {
        ByteArrayInputStream bais = new ByteArrayInputStream(attestationData);
        List<DataItem> dataItems;
        try {
            dataItems = new CborDecoder(bais).decode();
        } catch (CborException e) {
            throw new DecodingException("Unable to decode provided attestation data", e);
        }
        AttestationVerifier attestationVerifier = null;
        AuthData authData = null;
        AttestationStatement attestationStatement = null;
        Map attObjMap = (Map) dataItems.get(0);
        for (DataItem key : attObjMap.getKeys()) {
            if (key instanceof UnicodeString) {
                if (((UnicodeString) key).getString().equals("fmt")) {
                    UnicodeString value = (UnicodeString) attObjMap.get(key);
                    attestationVerifier = getAttestationVerifier(value.getString(), attestationPreference, secretSource,
                            enforceRevocationCheck);
                }
                if (((UnicodeString) key).getString().equals("authData")) {
                    byte[] rawAuthData = ((ByteString) attObjMap.get(key)).getBytes();
                    authData = authDataDecoder.decode(rawAuthData);
                }
                if (((UnicodeString) key).getString().equals("attStmt")) {
                    Map attSmtMap = (Map) attObjMap.get(key);
                    if (attestationVerifier != null) {
                        attestationStatement = getAttestationStatement(attSmtMap);
                    }
                }
            }
        }
        return new AttestationObject(attestationVerifier, authData, attestationStatement);
    }

    private AttestationStatement getAttestationStatement(Map attSmtMap) throws DecodingException {
        AttestationStatement attestationStatement = new AttestationStatement();

        for (DataItem attSmtKey : attSmtMap.getKeys()) {
            if (((UnicodeString) attSmtKey).getString().equals("x5c")) {
                //cut the last cert at the CA if it exists (all formats except FIDOU2F)
                List<DataItem> items = ((Array) attSmtMap.get(attSmtKey)).getDataItems();
                if (items.size() > 1) {
                    attestationStatement.setCaCert(((ByteString) items.remove(items.size() - 1)).getBytes());
                }

                //take the rest
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (DataItem item : items) {
                    byte[] bytes = ((ByteString) item).getBytes();
                    baos.write(bytes, 0, bytes.length);
                }
                byte[] certsByte = baos.toByteArray();

                List<X509Certificate> certs = createCerts(certsByte);
                if (certs != null) {
                    attestationStatement.setAttestnCerts(certs);
                }
            }
            if (((UnicodeString) attSmtKey).getString().equals("sig")) {
                attestationStatement.setSig(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
            }
            if (((UnicodeString) attSmtKey).getString().equals("pubArea")) {
                attestationStatement.setPubArea(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
            }
            if (((UnicodeString) attSmtKey).getString().equals("certInfo")) {
                attestationStatement.setCertInfo(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
            }
            if (((UnicodeString) attSmtKey).getString().equals("response")) {
                attestationStatement.setResponse(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
            }
            if (((UnicodeString) attSmtKey).getString().equals("alg")) {
                BigInteger value = ((Number) attSmtMap.get(attSmtKey)).getValue();
                CoseAlgorithm alg = CoseAlgorithm.fromCoseNumber(value.intValue());
                if (alg == null) {
                    throw new DecodingException("Invalid or unsupported algorithm required by attestation statement.");
                }
                attestationStatement.setAlg(alg);
            }
            if (((UnicodeString) attSmtKey).getString().equals("ecdaaKeyId")) {
                attestationStatement.setEcdaaKeyId(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
            }
            if (((UnicodeString) attSmtKey).getString().equals("ver")) {
                attestationStatement.setVer(((UnicodeString) attSmtMap.get(attSmtKey)).getString());
            }
        }
        return attestationStatement;
    }

    private AttestationVerifier getAttestationVerifier(String fmt, AttestationPreference attestationPreference,
                                                       Promise<Stream<VerificationKey>, NeverThrowsException> secrets,
                                                       boolean enforcedRevocation) throws DecodingException {
        switch (fmt) {
        case "none":
            if (attestationPreference == AttestationPreference.DIRECT) {
                logger.debug("direct attestation cannot be performed on the none attestation format");
                throw new DecodingException("Unacceptable attestation format provided - direct attestation required.");
            }
            logger.debug("none verifier selected");
            return noneVerifier;
        case "fido-u2f":
            logger.debug("fido-u2f verifier selected");
            return new FidoU2fVerifier(trustAnchorValidatorFactory.create(
                    trustAnchorUtilities.trustAnchorsFromSecrets(secrets), enforcedRevocation));
        case "android-safetynet":
            logger.warn("android-safetynet verifier selected");
            return new AndroidSafetyNetVerifier();
        case "android-key":
            logger.warn("android-key not a supported attestation format");
            throw new DecodingException("Unacceptable attestation format provided - android-key not supported.");
        case "tpm":
            logger.debug("tpm verifier selected");
            return new TpmVerifier(trustAnchorValidatorFactory.create(
                    trustAnchorUtilities.trustAnchorsFromSecrets(secrets), enforcedRevocation));
        case "packed":
            logger.debug("packed verifier selected");
            return new PackedVerifier(flowUtilities, trustAnchorValidatorFactory.create(
                    trustAnchorUtilities.trustAnchorsFromSecrets(secrets), enforcedRevocation));
        default:
            throw new DecodingException("Unacceptable attestation format provided.");
        }
    }

}