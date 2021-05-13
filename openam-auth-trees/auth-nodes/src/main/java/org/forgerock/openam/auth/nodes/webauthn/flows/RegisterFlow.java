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
package org.forgerock.openam.auth.nodes.webauthn.flows;

import static org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities.base64UrlDecode;
import static org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities.getHash;
import static org.forgerock.secrets.Purpose.purpose;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.forgerock.openam.auth.nodes.webauthn.AttestationPreference;
import org.forgerock.openam.auth.nodes.webauthn.UserVerificationRequirement;
import org.forgerock.openam.auth.nodes.webauthn.WebAuthnRegistrationNode;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationResponse;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AttestationDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.DecodingException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.AttestationFailedException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.InvalidDataException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.UnsupportedAttestationFormatException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.UserNotVerifiedException;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.WebAuthnRegistrationException;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.VerificationResponse;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.Secrets;
import org.forgerock.openam.secrets.SecretsProviderFacade;
import org.forgerock.openam.shared.secrets.Labels;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.secrets.keys.VerificationKey;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of https://www.w3.org/TR/webauthn/#registering-a-new-credential
 * Decodes the data and performs verification.
 */
@Singleton
public class RegisterFlow {

    private final Logger logger = LoggerFactory.getLogger(RegisterFlow.class);

    private final AttestationDecoder attestationDecoder;
    private final FlowUtilities flowUtilities;
    private final Secrets secrets;

    /**
     * The register flow constructor.
     *
     * @param flowUtilities copy of the flowUtilities utils
     * @param attestationDecoder copy of the attestation decoder utils
     * @param secrets access to the secret service
     */
    @Inject
    public RegisterFlow(FlowUtilities flowUtilities, AttestationDecoder attestationDecoder, Secrets secrets) {
        this.flowUtilities = flowUtilities;
        this.attestationDecoder = attestationDecoder;
        this.secrets = secrets;
    }

    /**
     * Takes several pieces of data as input, performs the registration flow / ceremony as defined in the spec.
     * If successful, it returns the AttestationObject which can be verified further.
     *
     * @param realm the current realm.
     * @param clientData the client data.
     * @param attestationData the encoded attestation data.
     * @param challengeBytes the challenge as bytes.
     * @param rpId the relying party ID.
     * @param credentialId the credential ID associated with the user.
     * @param origins the origin urls one of which the device should claim
     * @param config the config for the node from which the request came.
     * @return AttestationResponse The attestation data and its attestation type.
     * @throws WebAuthnRegistrationException the exception indicating the failure type.
     */
    public AttestationResponse accept(Realm realm, String clientData, byte[] attestationData, byte[] challengeBytes,
                                      String rpId, String credentialId,  Set<String> origins,
                                      WebAuthnRegistrationNode.Config config)
            throws WebAuthnRegistrationException {

        AttestationPreference attestationPreference = config.attestationPreference();
        UserVerificationRequirement userVerificationRequirement = config.userVerificationRequirement();
        Promise<Stream<VerificationKey>, NeverThrowsException> secretSource = null;

        if (config.trustStoreAlias().isPresent()) {
            SecretsProviderFacade spf = secrets.getRealmSecrets(realm);
            secretSource = spf.getValidSecrets(purpose(String.format(Labels.WEBAUTHN_TRUST_STORE,
                    config.trustStoreAlias().get()), VerificationKey.class));
        }

        // 7.1.1 & 7.1.2
        Map<String, Object> map;
        try {
            map = JsonValueBuilder.getObjectMapper().readValue(clientData, Map.class);
        } catch (IOException e) {
            logger.error("Failed to parse client data response.");
            throw new InvalidDataException("failed to parse the client data response.", e);
        }

        if (CollectionUtils.isEmpty(map)) {
            logger.error("client data parsed, but was empty.");
            throw new InvalidDataException("client data parsed, but was empty.");
        }

        // 7.1.3
        if (!("webauthn.create").equals(map.get("type"))) {
            logger.warn("Invalid webauthn type, expected webauthn.create, recieved {}", map.get("type"));
            throw new InvalidDataException("client data type was incorrect, expecting webauth.create");
        }

        //7.1.4
        if (map.get("challenge") == null
                || !MessageDigest.isEqual(challengeBytes, base64UrlDecode(map.get("challenge").toString()))) {
            throw new InvalidDataException("challenge in response not valid for the challenge sent");
        }

        //7.1.5
        String origin = Optional.ofNullable(map.get("origin")).map(Object::toString).orElse(null);
        if (origin == null || !flowUtilities.isOriginValid(realm, origins, origin)) {
            logger.warn("origin in response not valid for the actual origin. Origin provided was {} but origins"
                    + " allowed are: {}", origin, origins);
            throw new InvalidDataException("origin in response not valid for the actual origin");
        }

        //7.1.6
        //todo: token binding -- currently not supported

        //7.1.7
        byte[] clientDataHash = getHash(clientData);

        //7.1.8
        AttestationObject attestationObject;
        try {
            attestationObject = attestationDecoder.decode(attestationData, attestationPreference, secretSource,
                    config.enforceRevocationCheck());
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

        //7.1.15 & 7.1.16 verifier-appropriate implementation
        VerificationResponse verificationResponse = attestationObject.attestationVerifier
                .verify(attestationObject, clientDataHash);
        logger.debug("attestationType: {}", verificationResponse.getAttestationType());

        if (!verificationResponse.isValid()) {
            throw new AttestationFailedException("Attestation failed.");
        }

        //7.1.17
        //todo: verify credentialId -- currently not supported

        //7.1.18 & 7.1.19 handled based on return value
        return new AttestationResponse(attestationObject, verificationResponse.getAttestationType());
    }

}
