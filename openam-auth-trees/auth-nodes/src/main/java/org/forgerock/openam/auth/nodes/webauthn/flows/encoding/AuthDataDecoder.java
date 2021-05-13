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
 * Copyright 2018-2019 ForgeRock AS.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseCurve;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationFlags;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import co.nstant.in.cbor.model.NegativeInteger;
import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.UnsignedInteger;

/**
 * https://www.w3.org/TR/webauthn/#authenticator-data.
 */
public final class AuthDataDecoder {

    private static final Logger logger = LoggerFactory.getLogger(AuthDataDecoder.class);

    /**
     * Decodes the Auth Data.
     *
     * @param authDataAsBytes the auth data as bytes.
     * @throws DecodingException if there's an error during decoding
     * @return AuthData object.
     */
    public AuthData decode(byte[] authDataAsBytes) throws DecodingException {
        byte[] rpIdHash = Arrays.copyOfRange(authDataAsBytes, 0, 32);

        BitSet flags = BitSet.valueOf(Arrays.copyOfRange(authDataAsBytes, 32, 33));
        AttestationFlags attestationFlags = new AttestationFlags(flags);

        byte[] rawSignCount = Arrays.copyOfRange(authDataAsBytes, 33, 37);
        ByteBuffer wrapped = ByteBuffer.wrap(rawSignCount);
        int signCount = wrapped.getInt();

        AttestedCredentialData attestedCredentialData = null;
        if (authDataAsBytes.length > 37) {
            attestedCredentialData = getAttestedCredentialData(authDataAsBytes);
        }

        return new AuthData(rpIdHash, attestationFlags, signCount, attestedCredentialData, authDataAsBytes);
    }

    private AttestedCredentialData getAttestedCredentialData(byte[] authData) throws DecodingException {
        byte[] aaguid = Arrays.copyOfRange(authData, 37, 53);
        byte[] rawCredentialIdLength = Arrays.copyOfRange(authData, 53, 55);
        ByteBuffer wrapped = ByteBuffer.wrap(rawCredentialIdLength);
        int credentialIdLength = wrapped.getShort();

        int index = 55;
        byte[] credentialId = null;
        if (credentialIdLength > 0) {
            credentialId = Arrays.copyOfRange(authData, 55, 55 + credentialIdLength);
            index = index + credentialIdLength;
        }

        byte[] publicKeyBytes = Arrays.copyOfRange(authData, index, authData.length);

        List<DataItem> dataItems;
        try {
            dataItems = new CborDecoder(new ByteArrayInputStream(publicKeyBytes)).decode();
        } catch (CborException e) {
            logger.error("failed to decode data in CBOR format", e);
            throw new DecodingException("Unable to read data via CBOR decoding");
        }

        Map attObjMap = (Map) dataItems.get(0);

        int keyType = ((Number) attObjMap.get(new UnsignedInteger(1))).getValue().intValue();
        int alg =  ((Number) attObjMap.get(new UnsignedInteger(3))).getValue().intValue();
        CoseAlgorithm algorithmName = CoseAlgorithm.fromCoseNumber(alg);

        if (algorithmName == null) {
            throw new DecodingException("Unable to find appropriate algorithm");
        }

        String algorithm = algorithmName.getExactAlgorithmName();

        JWK publicKey;

        switch (keyType) {
        case 2:
            publicKey = decodeAsEC(attObjMap);
            break;
        case 3:
            publicKey = decodeAsRSA(attObjMap);
            break;
        default:
            throw new DecodingException("No appropriate key type.");
        }

        return new AttestedCredentialData(aaguid, credentialIdLength, credentialId, publicKey, algorithm);
    }

    private JWK decodeAsEC(Map attributes) throws DecodingException {

        int curve = ((Number) attributes.get(new NegativeInteger(-1))).getValue().intValue();
        byte[] xpos = ((ByteString) attributes.get(new NegativeInteger(-2))).getBytes();
        byte[] ypos = ((ByteString) attributes.get(new NegativeInteger(-3))).getBytes();
        CoseCurve curveName = CoseCurve.fromCoseNumber(curve);

        if (curveName == null) {
            throw new DecodingException("No appropriate curve");
        }

        return EcJWK.builder(curveName.getName(), Base64url.encode(xpos),
                Base64url.encode(ypos)).build();
    }

    private JWK decodeAsRSA(Map attributes) {
        byte[] modulus = ((ByteString) attributes.get(new NegativeInteger(-1))).getBytes();
        byte[] exponent = ((ByteString) attributes.get(new NegativeInteger(-2))).getBytes();

        return RsaJWK.builder(Base64url.encode(modulus), Base64url.encode(exponent)).build();
    }
}
