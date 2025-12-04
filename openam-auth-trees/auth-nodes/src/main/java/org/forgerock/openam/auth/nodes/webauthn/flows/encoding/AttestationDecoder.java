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
 * Copyright 2018-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.AttestationStatement;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.InvalidDataException;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataException;
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
 * Class to decode the packed bytes of the authentication registration
 * <a href="https://www.w3.org/TR/webauthn/#authenticatorattestationresponse">attestation response</a>.
 */
public class AttestationDecoder {

    private static final Logger logger = LoggerFactory.getLogger(AttestationDecoder.class);

    private final AuthDataDecoder authDataDecoder;

    /**
     * Construct a new attestation decoder.
     *
     * @param authDataDecoder             authentication data decoder
     */
    @Inject
    public AttestationDecoder(AuthDataDecoder authDataDecoder) {
        this.authDataDecoder = authDataDecoder;
    }

    /**
     * Decode the byte data, converting it into rich objects which can be reasoned about.
     *
     * @param attestationData the data as bytes.
     * @return the data as an AttestationObject.
     * @throws DecodingException if there's an error during decoding
     * @throws MetadataException if there's an error attempting to get the attestation verifier (likely due to an
     *              issue with configured metadata services, used when verifying packed attestations)
     * @throws InvalidDataException if invalid data is provided in the attestation statement
     */
    public AttestationObject decode(byte[] attestationData)
            throws DecodingException, MetadataException, InvalidDataException {
        ByteArrayInputStream bais = new ByteArrayInputStream(attestationData);
        List<DataItem> dataItems;
        try {
            dataItems = new CborDecoder(bais).decode();
        } catch (CborException e) {
            throw new DecodingException("Unable to decode provided attestation data", e);
        }
        String format = null;
        AuthData authData = null;
        AttestationStatement attestationStatement = null;
        Map attObjMap = (Map) dataItems.get(0);

        for (DataItem key : attObjMap.getKeys()) {
            if (key instanceof UnicodeString) {
                String keyString = ((UnicodeString) key).getString();
                switch (keyString) {
                case "fmt":
                    format = ((UnicodeString) attObjMap.get(key)).getString();
                    break;
                case "authData":
                    authData = authDataDecoder.decode(((ByteString) attObjMap.get(key)).getBytes());
                    break;
                case "attStmt":
                    attestationStatement = getAttestationStatement((Map) attObjMap.get(key));
                    break;
                default:
                    //Intentionally do nothing
                    break;
                }
            }
        }
        return new AttestationObject(format, authData, attestationStatement);
    }

    private AttestationStatement getAttestationStatement(Map attSmtMap) throws DecodingException, InvalidDataException {
        AttestationStatement attestationStatement = new AttestationStatement();

        for (DataItem attSmtKey : attSmtMap.getKeys()) {
            String keyString = ((UnicodeString) attSmtKey).getString();
            switch (keyString) {
            case "x5c":
                List<DataItem> x5cItems = ((Array) attSmtMap.get(attSmtKey)).getDataItems();
                attestationStatement.setAttestnCerts(decodeAttestnCerts(x5cItems));
                break;
            case "sig":
                attestationStatement.setSig(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
                break;
            case "pubArea":
                attestationStatement.setPubArea(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
                break;
            case "certInfo":
                attestationStatement.setCertInfo(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
                break;
            case "response":
                attestationStatement.setResponse(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
                break;
            case "alg":
                BigInteger value = ((Number) attSmtMap.get(attSmtKey)).getValue();
                CoseAlgorithm alg = CoseAlgorithm.fromCoseNumber(value.intValue());
                if (alg == null) {
                    throw new DecodingException("Invalid/unsupported algorithm required by attestation statement.");
                }
                attestationStatement.setAlg(alg);
                break;
            case "ecdaaKeyId":
                attestationStatement.setEcdaaKeyId(((ByteString) attSmtMap.get(attSmtKey)).getBytes());
                break;
            case "ver":
                attestationStatement.setVer(((UnicodeString) attSmtMap.get(attSmtKey)).getString());
                break;
            default:
                //Intentionally do nothing
                break;
            }
        }
        return attestationStatement;
    }

    private List<X509Certificate> decodeAttestnCerts(List<DataItem> x5cItems) throws InvalidDataException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (DataItem item : x5cItems) {
            byte[] bytes = ((ByteString) item).getBytes();
            baos.write(bytes, 0, bytes.length);
        }
        byte[] certsByte = baos.toByteArray();

        List<X509Certificate> certs = createCerts(certsByte);

        // cut the chain at the root CA cert if it exists (all formats except FIDOU2F)
        if (certs.size() > 1) {
            X509Certificate lastCert = certs.get(certs.size() - 1);
            if (lastCert.getIssuerX500Principal().equals(lastCert.getSubjectX500Principal())) {
                // self-signed root CA
                throw new InvalidDataException("root certificate must not be self-signed");
            }
        }

        return certs;
    }

    @SuppressWarnings("unchecked")
    private List<X509Certificate> createCerts(byte[] certData) {
        try {
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X509");
            return (List<X509Certificate>) cf.generateCertificates(new ByteArrayInputStream(certData));
        } catch (Exception e) {
            logger.warn("failed to convert certificate data into a certificate objects", e);
            return Collections.emptyList();
        }
    }
}
