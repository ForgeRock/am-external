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
package org.forgerock.openam.auth.nodes.webauthn.flows;

import static org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities.base64UrlDecode;
import static org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities.getHash;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.openam.auth.nodes.webauthn.UserVerificationRequirement;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnRegistrationNode;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationResponse;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AttestationDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AttestationVerifierFactory;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.DecodingException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.AttestationFailedException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.InvalidDataException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.UnsupportedAttestationFormatException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.UserNotVerifiedException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.WebAuthnRegistrationException;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.VerificationResponse;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataException;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.secrets.NoSuchSecretException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

/**
 * An implementation of https://www.w3.org/TR/webauthn/#registering-a-new-credential
 * Decodes the data and performs verification.
 */
public class RegisterFlow {

    private final Logger logger = LoggerFactory.getLogger(RegisterFlow.class);

    private final AttestationDecoder attestationDecoder;
    private final AttestationVerifierFactory attestationVerifierFactory;
    private final Realm realm;
    private final WebAuthnRegistrationNode.Config config;
    private final FlowUtilities flowUtilities;

    /**
     * The register flow constructor.
     *
     * @param realm the realm
     * @param config the configuration of the node which is creating this flow
     * @param flowUtilities copy of the flowUtilities utils
     * @param attestationDecoder copy of the attestation decoder utils
     * @param attestationVerifierFactory the attestation verifier factory
     */
    @Inject
    public RegisterFlow(AttestationDecoder attestationDecoder,
            FlowUtilities flowUtilities, AttestationVerifierFactory attestationVerifierFactory,
            @Assisted Realm realm, @Assisted WebAuthnRegistrationNode.Config config) {
        this.realm = realm;
        this.config = config;
        this.flowUtilities = flowUtilities;
        this.attestationDecoder = attestationDecoder;
        this.attestationVerifierFactory = attestationVerifierFactory;
    }

    /**
     * Takes several items of data as input, performs the registration flow / ceremony as defined in the spec.
     * If successful, it returns the AttestationObject which can be verified further.
     *
     * @param clientData the client data
     * @param attestationData the encoded attestation data
     * @param challengeBytes the challenge as bytes
     * @param rpId the relying party ID
     * @param origins the origin URLs, one of which the device should claim
     * @return AttestationResponse the attestation data and its attestation type
     * @throws WebAuthnRegistrationException the Exception indicating the failure type
     */
    public AttestationResponse accept(String clientData, byte[] attestationData, byte[] challengeBytes,
            String rpId, Set<String> origins) throws WebAuthnRegistrationException {
        // 7.1 Registering a New Credential - https://www.w3.org/TR/webauthn-2/#sctn-registering-a-new-credential
        UserVerificationRequirement userVerificationRequirement = config.userVerificationRequirement();

        Map<String, Object> map;
        try {
            // 7.1.6
            map = JsonValueBuilder.getObjectMapper().readValue(clientData, Map.class);
        } catch (IOException e) {
            logger.error("Failed to parse client data response.");
            throw new InvalidDataException("failed to parse the client data response.", e);
        }

        if (CollectionUtils.isEmpty(map)) {
            logger.error("client data parsed, but was empty.");
            throw new InvalidDataException("client data parsed, but was empty.");
        }

        // 7.1.7
        if (!("webauthn.create").equals(map.get("type"))) {
            logger.warn("Invalid webauthn type, expected webauthn.create, received {}", map.get("type"));
            throw new InvalidDataException("client data type was incorrect, expecting webauthn.create");
        }

        // 7.1.8
        if (map.get("challenge") == null
                || !MessageDigest.isEqual(challengeBytes, base64UrlDecode(map.get("challenge").toString()))) {
            throw new InvalidDataException("challenge in response not valid for the challenge sent");
        }

        // 7.1.9
        String origin = Optional.ofNullable(map.get("origin")).map(Object::toString).orElse(null);
        if (origin == null || !flowUtilities.isOriginValid(realm, origins, origin)) {
            logger.warn("origin in response not valid for the actual origin. Origin provided was {} but origins"
                    + " allowed are: {}", origin, origins);
            throw new InvalidDataException("origin in response not valid for the actual origin");
        }

        // 7.1.10
        //todo: token binding -- currently not supported

        // 7.1.11
        byte[] clientDataHash = getHash(clientData);

        // 7.1.12
        AttestationObject attestationObject;
        try {
            attestationObject = attestationDecoder.decode(attestationData);
        } catch (DecodingException | MetadataException e) {
            throw new InvalidDataException("could not decode attestation object", e);
        }

        // 7.1.13
        if (!MessageDigest.isEqual(getHash(rpId), attestationObject.authData.rpIdHash)) {
            throw new InvalidDataException("rpId hashes did not match");
        }

        // 7.1.14
        if (!attestationObject.authData.attestationFlags.isUserPresent()) {
            throw new InvalidDataException("user present bit not set");
        }

        // 6.1 verify the isAttestedDataIncluded flag is set
        if (!attestationObject.authData.attestationFlags.isAttestedDataIncluded()) {
            throw new InvalidDataException("attested data bit not set");
        }

        // 6.1 verify the attestedCredentialData is non-null
        if (attestationObject.authData.attestedCredentialData == null) {
            throw new InvalidDataException("attested credential data not present");
        }

        // 7.1.15
        if (userVerificationRequirement == UserVerificationRequirement.REQUIRED) {
            if (!attestationObject.authData.attestationFlags.isUserVerified()) {
                throw new UserNotVerifiedException("user verified bit required and not set");
            }
        }

        // 7.1.16 Verify that the "alg" parameter in the credential public key in authData matches
        // the alg attribute of one of the items in options.pubKeyCredParams.
        //todo: alg -- currently not supported

        // 7.1.4 & 7.1.17
        //todo: extensions -- currently not supported

        // 7.1.18 Determine the attestation statement format by performing a USASCII case-sensitive match on fmt
        // against the set of supported WebAuthn Attestation Statement Format Identifier values.
        // This is handled by the AttestationDecoder, which determines the verifier.

        // 7.1.19 Verify that attStmt is a correct attestation statement, conveying a valid attestation signature,
        // by using the attestation statement format fmt’s verification procedure given attStmt, authData and hash.
        AttestationVerifier attestationVerifier;
        try {
            attestationVerifier = attestationVerifierFactory.create(realm, config, attestationObject.format);
        } catch (DecodingException e) {
            throw new UnsupportedAttestationFormatException("attestation claims to be in an unsupported format");
        } catch (MetadataException e) {
            throw new UnsupportedAttestationFormatException("failed to resolve metadata service");
        } catch (NoSuchSecretException e) {
            throw new UnsupportedAttestationFormatException("failed to load required certificates");
        }

        // 7.1.20 and 7.1.21 are both handled if a PackedVerifier is used
        VerificationResponse verificationResponse = attestationVerifier.verify(attestationObject, clientDataHash);
        logger.debug("attestationType: {}", verificationResponse.getAttestationType());

        // 7.1.22 and 7.1.23
        //todo: verify credentialId not already registered -- currently not supported

        // 7.1.24
        if (!verificationResponse.isValid()) {
            throw new AttestationFailedException("Attestation failed.");
        }

        return new AttestationResponse(attestationObject, verificationResponse.getAttestationType());
    }

}
