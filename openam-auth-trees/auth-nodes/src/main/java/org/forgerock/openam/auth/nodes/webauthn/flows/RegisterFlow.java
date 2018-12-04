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
 * Copyright 2018 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows;

import static org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities.base64Decode;
import static org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities.getHash;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.auth.nodes.webauthn.AttestationPreference;
import org.forgerock.openam.auth.nodes.webauthn.UserVerificationRequirement;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AttestationDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.DecodingException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.AttestationFailedException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.InvalidDataException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.UnsupportedAttestationFormatException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.UserNotVerifiedException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.WebAuthnRegistrationException;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.VerificationResponse;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;

/**
 * An implementation of https://www.w3.org/TR/webauthn/#registering-a-new-credential
 * Decodes the data and performs verification.
 */
@Singleton
public class RegisterFlow {

    private AttestationDecoder attestationDecoder;
    private final FlowUtilities flowUtilities;

    /**
     * The register flow constructor. The class is a singleton as constructor requires an expensive call to construct
     * The bouncy castle provider.
     *
     * @param flowUtilities copy of the flowUtilities utils
     * @param attestationDecoder copy of the attestation decoder utils
     */
    @Inject
    public RegisterFlow(FlowUtilities flowUtilities, AttestationDecoder attestationDecoder) {
        this.flowUtilities = flowUtilities;
        this.attestationDecoder = attestationDecoder;
    }

    /**
     * Takes several pieces of data as input, performs the registration flow / ceremony as defined in the spec.
     * If successful, it returns the AttestationObject which can be verified further.
     *
     * @param clientData the client data.
     * @param attestationData the encoded attestation data.
     * @param challengeBytes the challenge as bytes.
     * @param rpId the replying party ID.
     * @param userVerificationRequirement if required, the user must be verified.
     * @param credentialId the credential ID associated with the user.
     * @param originUrl the origin url the device should claim
     * @param attestationPreference configured preference of attestation.
     * @return AttestationObject The attestation data.
     * @throws WebAuthnRegistrationException the exception indicating the failure type.
     */
    public AttestationObject accept(String clientData, byte[] attestationData, byte[] challengeBytes, String rpId,
                                    UserVerificationRequirement userVerificationRequirement, String credentialId,
                                    String originUrl, AttestationPreference attestationPreference)
            throws WebAuthnRegistrationException {
        // 7.1.1 & 7.1.2
        Map<String, Object> map;
        try {
            map = JsonValueBuilder.getObjectMapper().readValue(clientData, Map.class);
        } catch (IOException e) {
            throw new InvalidDataException("failed to parse the client data response.", e);
        }

        if (CollectionUtils.isEmpty(map)) {
            throw new InvalidDataException("client data parsed, but was empty.");
        }

        // 7.1.3
        if (!("webauthn.create").equals(map.get("type"))) {
            throw new InvalidDataException("client data type was incorrect, expecting webauth.create");
        }

        //7.1.4
        if (map.get("challenge") == null
                || !MessageDigest.isEqual(challengeBytes, base64Decode(map.get("challenge").toString()))) {
            throw new InvalidDataException("challenge in response not valid for the challenge sent");
        }

        //7.1.5
        if (map.get("origin") == null || !flowUtilities.originsMatch(originUrl, map.get("origin").toString())) {
            throw new InvalidDataException("origin in response not valid for the actual origin");
        }

        //7.1.6
        //todo: token binding -- currently not supported

        //7.1.7
        byte[] clientDataHash = getHash(clientData);

        //7.1.8
        AttestationObject attestationObject;
        try {
            attestationObject = attestationDecoder.decode(attestationData, attestationPreference);
        } catch (DecodingException e) {
            throw new InvalidDataException("could not decode attestation object", e);
        }

        //7.1.9
        if (!MessageDigest.isEqual(getHash(rpId), attestationObject.authData.rpIdHash)) {
            throw new InvalidDataException("rpId hashes did not match");
        }

        //7.1.10
        if (!attestationObject.authData.attestationFlags.isUserPresent()) {
            throw new InvalidDataException("user present bit not set");
        }

        //7.1.11
        if (userVerificationRequirement == UserVerificationRequirement.REQUIRED) {
            if (!attestationObject.authData.attestationFlags.isUserVerified()) {
                throw new UserNotVerifiedException("user verified bit required and not set");
            }
        }

        //7.1.12
        //todo: extensions -- currently not supported

        //7.1.13 & 7.1.14
        if (attestationObject.attestationVerifier == null) {
            throw new UnsupportedAttestationFormatException("attestation claims to be in an unsupported format");
        }

        VerificationResponse verificationResponse = attestationObject.attestationVerifier
                .verify(attestationObject, clientDataHash);
        if (!verificationResponse.isValid()) {
            throw new AttestationFailedException("Attestation failed.");
        }

        //7.1.15
        //todo: trust anchors -- currently not supported

        //7.1.16
        //todo: attestation trustworthiness -- currently not supported

        //7.1.17
        //todo: verify credentialId -- currently not supported

        //7.1.18 & 7.1.19 handled based on return value
        return attestationObject;
    }

}
