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
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.jwk.JWK;
import org.forgerock.json.jose.jwk.OkpJWK;
import org.forgerock.json.jose.jwk.RsaJWK;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.openam.auth.nodes.webauthn.Aaguid;
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
     * @param authDataAsBytes the auth data as bytes
     * @return AuthData object.
     * @throws DecodingException        if there's an error during decoding
     */
    public AuthData decode(byte[] authDataAsBytes) throws DecodingException {
        byte[] rpIdHash = Arrays.copyOfRange(authDataAsBytes, 0, 32);

        BitSet flags = BitSet.valueOf(Arrays.copyOfRange(authDataAsBytes, 32, 33));
        AttestationFlags attestationFlags = new AttestationFlags(flags);

        byte[] rawSignCount = Arrays.copyOfRange(authDataAsBytes, 33, 37);
        ByteBuffer wrapped = ByteBuffer.wrap(rawSignCount);
        int signCount = wrapped.getInt();

        AttestedCredentialData attestedCredentialData = null;
        if (authDataAsBytes.length > 37 && attestationFlags.isAttestedDataIncluded()) {
            attestedCredentialData = getAttestedCredentialData(authDataAsBytes);
        }

        return new AuthData(rpIdHash, attestationFlags, signCount, attestedCredentialData, authDataAsBytes);
    }

    private AttestedCredentialData getAttestedCredentialData(byte[] authData)
            throws DecodingException {
        byte[] rawAaguid = Arrays.copyOfRange(authData, 37, 53);
        Aaguid aaguid = new Aaguid(rawAaguid);
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
        CoseAlgorithm algorithm = CoseAlgorithm.fromCoseNumber(alg);

        if (algorithm == null) {
            throw new DecodingException("Unable to find appropriate algorithm");
        }

        String algorithmName = algorithm.getExactAlgorithmName();

        JWK publicKey;

        switch (keyType) {
        case 1: // defined https://www.rfc-editor.org/rfc/rfc8152#section-13 - table 21
            publicKey = decodeAsOKPPublicKey(attObjMap, algorithm);
            break;
        case 2: // defined https://www.rfc-editor.org/rfc/rfc8152#section-13 - table 21
            publicKey = decodeAsECPublicKey(attObjMap);
            break;
        case 3: // defined https://www.rfc-editor.org/rfc/rfc8230#section-4 - table 3
            publicKey = decodeAsRSAPublicKey(attObjMap);
            break;
        default:
            throw new DecodingException("No appropriate key type.");
        }

        return new AttestedCredentialData(aaguid, credentialIdLength, credentialId, publicKey, algorithmName);
    }

    private JWK decodeAsOKPPublicKey(Map attributes, CoseAlgorithm algorithm) throws DecodingException {
        int curveNumber = ((Number) attributes.get(new NegativeInteger(-1))).getValue().intValue();
        byte[] x = ((ByteString) attributes.get(new NegativeInteger(-2))).getBytes();
        CoseCurve coseCurve = CoseCurve.fromCoseNumber(curveNumber);

        // 5.8.5 - "Keys with algorithm EdDSA (-8) MUST specify Ed25519 (6) as the crv parameter."
        if (coseCurve == null || (CoseAlgorithm.EDDSA.equals(algorithm) && !CoseCurve.ED25519.equals(coseCurve))) {
            throw new DecodingException("No appropriate curve");
        }

        try {
            return OkpJWK.builder()
                    .curve(SupportedEllipticCurve.forName(coseCurve.getName()))
                    .x(Base64url.encode(x))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new DecodingException("No appropriate curve");
        }
    }

    private JWK decodeAsECPublicKey(Map attributes) throws DecodingException {
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

    private JWK decodeAsRSAPublicKey(Map attributes) {
        byte[] modulus = ((ByteString) attributes.get(new NegativeInteger(-1))).getBytes();
        byte[] exponent = ((ByteString) attributes.get(new NegativeInteger(-2))).getBytes();

        return RsaJWK.builder(Base64url.encode(modulus), Base64url.encode(exponent)).build();
    }
}
