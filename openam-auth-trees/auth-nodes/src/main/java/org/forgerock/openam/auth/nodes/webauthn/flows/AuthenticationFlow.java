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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.ArrayUtils;
import org.forgerock.openam.auth.nodes.webauthn.UserVerificationRequirement;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models a web authentication flow.
 */
@Singleton
public class AuthenticationFlow {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationFlow.class);

    private final FlowUtilities flowUtilities;

    /**
     * The authentication flow.
     *
     * @param flowUtilities copy of the flowUtilities utils
     */
    @Inject
    public AuthenticationFlow(FlowUtilities flowUtilities) {
        this.flowUtilities = flowUtilities;
    }

    /**
     * Performs the authentication ceremony, resulting in true if successful.
     *
     *
     * @param realm the realm.
     * @param clientData the client data.
     * @param authData the authenticator data.
     * @param signature the signature provided.
     * @param challengeBytes the challenge in bytes.
     * @param rpId the replying party ID.
     * @param device the authentication device.
     * @param origins the origin urls one of which the device should claim
     * @param userVerificationRequirement set to required if the user needs to be verified.
     * @return true if the attestation data is correct and the user can authenticate.
     */
    public boolean accept(Realm realm, String clientData, AuthData authData, byte[] signature,
            byte[] challengeBytes, String rpId, WebAuthnDeviceSettings device, Set<String> origins,
            UserVerificationRequirement userVerificationRequirement) {

        // 7.2.1 & 7.2.2 & 7.2.3 handled prior to calling this method

        // 7.2.4 & 7.2.5 & 7.2.6
        Map<String, Object> map;

        try {
            map = JsonValueBuilder.getObjectMapper().readValue(clientData, Map.class);
        } catch (IOException e) {
            logger.error("failed to parse the client data response.", e);
            return false;
        }

        if (CollectionUtils.isEmpty(map)) {
            logger.warn("failure to parse client data - map is empty");
            return false;
        }

        // 7.2.7
        if (!("webauthn.get").equals(map.get("type"))) {
            logger.warn("client data type was incorrect, expecting webauth.get");
            return false;
        }

        // 7.2.8
        if (map.get("challenge") == null
                || !MessageDigest.isEqual(challengeBytes, base64UrlDecode(map.get("challenge").toString()))) {
            logger.warn("challenge in response not valid for the challenge sent");
            return false;
        }

        //7.2.9
        String origin = Optional.ofNullable(map.get("origin")).map(Object::toString).orElse(null);
        if (origin == null || !flowUtilities.isOriginValid(realm, origins, origin)) {
            logger.warn("origin in response not valid for the actual origin. Origin provided was {} but origins"
                    + " allowed are: {}", origin, origins);
            return false;
        }

        // 7.2.10
        //todo: token binding -- currently not supported

        // 7.2.11
        if (!MessageDigest.isEqual(getHash(rpId), authData.rpIdHash)) {
            logger.warn("rpId hashes did not match");
            return false;
        }

        // 7.2.12
        if (!authData.attestationFlags.isUserPresent()) {
            logger.warn("user present bit not set in auth data");
            return false;
        }

        // 7.2.13
        if (userVerificationRequirement == UserVerificationRequirement.REQUIRED) {
            if (!authData.attestationFlags.isUserVerified()) {
                logger.warn("user verified bit required and not set in auth data");
                return false;
            }
        }

        // 7.2.14
        //todo: extensions -- currently not supported

        // 7.2.15
        byte[] cDataHash = getHash(clientData);
        byte[] concatBytes = ArrayUtils.addAll(authData.rawAuthenticatorData, cDataHash);

        // 7.2.16
        try {
            PublicKey publicKey = flowUtilities.getPublicKeyFromJWK(device.getKey());
            Signature sig = Signature.getInstance(device.getAlgorithm());
            sig.initVerify(publicKey);
            sig.update(concatBytes);
            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            logger.error("error in verifying webauthn signature", e);
        }

        // 7.2.17
        //todo: signature counter
        return false;

    }

}
