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
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import static org.forgerock.openam.auth.nodes.webauthn.flows.CertificateFactory.createCert;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.inject.Inject;

import org.forgerock.openam.auth.nodes.webauthn.AttestationPreference;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.FidoU2fVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.NoneVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.PackedVerifier;

import com.google.common.collect.Lists;

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

    private final AuthDataDecoder authDataDecoder;
    private final FlowUtilities flowUtilities;

    /**
     * Construct a new attestation decoder.
     *
     * @param authDataDecoder authentication data decoder.
     * @param flowUtilities Utilities for webauthn.
     */
    @Inject
    public AttestationDecoder(AuthDataDecoder authDataDecoder, FlowUtilities flowUtilities) {
        this.authDataDecoder = authDataDecoder;
        this.flowUtilities = flowUtilities;
    }

    /**
     * Decode the byte data, converting it into rich objects which can be reasoned about.
     *
     * @param attestationData the data as bytes.
     * @param attestationPreference the type of attestation the server expects
     * @throws DecodingException if there's an error during decoding
     * @return the data as an AttestationObject.
     */
    public AttestationObject decode(byte[] attestationData,
                                    AttestationPreference attestationPreference) throws DecodingException {
        ByteArrayInputStream bais = new ByteArrayInputStream(attestationData);
        List<DataItem> dataItems = null;
        try {
            dataItems = new CborDecoder(bais).decode();
        } catch (CborException e) {
            e.printStackTrace();
        }
        AttestationVerifier attestationVerifier = null;
        AuthData authData = null;
        AttestationStatement attestationStatement = null;
        Map attObjMap = (Map) dataItems.get(0);
        for (DataItem key : attObjMap.getKeys()) {
            if (key instanceof UnicodeString) {
                if (((UnicodeString) key).getString().equals("fmt")) {
                    UnicodeString value = (UnicodeString) attObjMap.get(key);
                    attestationVerifier = getAttestationVerifier(value.getString(), attestationPreference);
                    if (attestationVerifier == null) {
                        throw new DecodingException("Unacceptable attestation format provided.");
                    }
                }
                if (((UnicodeString) key).getString().equals("authData")) {
                    byte[] rawAuthData = ((ByteString) attObjMap.get(key)).getBytes();
                    authData = authDataDecoder.decode(rawAuthData);
                }
                if (((UnicodeString) key).getString().equals("attStmt")) {
                    Map attSmtMap = (Map) attObjMap.get(key);
                    if (attestationVerifier != null) {
                        attestationStatement = getAttestationStatment(attSmtMap);
                    }
                }
            }
        }
        return new AttestationObject(attestationVerifier, authData, attestationStatement);
    }

    private AttestationStatement getAttestationStatment(Map attSmtMap) {
        AttestationStatement attestationStatement = new AttestationStatement();

        for (DataItem attSmtKey : attSmtMap.getKeys()) {
            if (((UnicodeString) attSmtKey).getString().equals("x5c")) {
                List<DataItem> items = ((Array) attSmtMap.get(attSmtKey)).getDataItems();
                byte[] attestnCert = ((ByteString) items.get(0)).getBytes();
                if (items.size() > 1) {
                    attestationStatement.setCaCert(((ByteString) items.get(1)).getBytes());
                }
                X509Certificate cert = createCert(attestnCert);
                if (cert != null) {
                    attestationStatement.setAttestnCerts(Lists.newArrayList(cert));
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
                attestationStatement.setAlg(CoseAlgorithm.fromCoseNumber(value.intValue()));
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

    private AttestationVerifier getAttestationVerifier(String fmt, AttestationPreference attestationPreference) {
        if ("none".equals(fmt)) {
            if (attestationPreference == AttestationPreference.DIRECT) {
                return null;
            }
            return new NoneVerifier();
        }
        if ("fido-u2f".equals(fmt)) {
            return new FidoU2fVerifier();
        }
        if ("android-safetynet".equals(fmt)) {
            return null;
        }
        if ("android-key".equals(fmt)) {
            return null;
        }
        if ("tpm".equals(fmt)) {
            return null;
        }
        if ("packed".equals(fmt)) {
            return new PackedVerifier(flowUtilities);
        }
        return null;
    }
}
