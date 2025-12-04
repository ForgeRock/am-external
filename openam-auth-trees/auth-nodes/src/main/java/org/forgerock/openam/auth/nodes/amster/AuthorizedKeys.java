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
 * Copyright 2016-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.amster;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static org.forgerock.json.jose.jws.SupportedEllipticCurve.P256;
import static org.forgerock.json.jose.jws.SupportedEllipticCurve.P384;
import static org.forgerock.json.jose.jws.SupportedEllipticCurve.P521;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.json.jose.jws.handlers.ECDSASigningHandler;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.util.SignatureUtil;
import org.forgerock.util.encode.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;

/**
 * Class for loading an SSH authorized keys file.
 */
class AuthorizedKeys {
    private static final Map<String, SupportedEllipticCurve> SSH_ELLIPTIC_CURVES
            = ImmutableMap.of("nistp256", P256, "nistp384", P384, "nistp521", P521);

    private static final String AUTHORIZED_KEYS_REGEX =
            "^([a-z0-9-]+(?:=\".+\")?(?:,[a-z0-9-]+(?:=\".+\")?)* )?"
                    + "(ecdsa-sha2-nistp256|ecdsa-sha2-nistp384|ecdsa-sha2-nistp521|ssh-ed25519|ssh-dss|ssh-rsa) "
                    + "([A-Za-z0-9+/=]+).*$";
    private static final Pattern AUTHORIZED_KEYS_PATTERN = Pattern.compile(AUTHORIZED_KEYS_REGEX);

    private final Logger debug = LoggerFactory.getLogger(AuthorizedKeys.class);

    Set<Key> read(InputStream authorizedKeys) {
        Set<Key> keys = new HashSet<>();

        for (String line : readLines(authorizedKeys)) {
            try {
                AuthorizedKey key = parseKey(line);
                if (key != null) {
                    keys.add(key);
                }
            } catch (IOException | GeneralSecurityException e) {
                debug.warn("AmsterAuthLoginModule#init: Could not read key " + line, e);
            }
        }
        return keys;
    }

    private List<String> readLines(InputStream authorizedKeys) {
        try (InputStreamReader reader = new InputStreamReader(authorizedKeys, UTF_8)) {
            return CharStreams.readLines(reader);
        } catch (IOException e) {
            debug.error("Could not read authorized keys data");
            return emptyList();
        }
    }

    @VisibleForTesting
    AuthorizedKey parseKey(String line) throws IOException, GeneralSecurityException {
        Matcher matcher = AUTHORIZED_KEYS_PATTERN.matcher(line);
        if (!matcher.find()) {
            debug.debug("AuthorizedKeys: Not matched as a key line: {}", line);
            return null;
        }
        String keyOptions = matcher.group(1);
        String keyType = matcher.group(2);
        String encodedKey = matcher.group(3);
        debug.debug("AuthorizedKeys: Read public key of type {} with options {}: {}", keyType,
                keyOptions, encodedKey);

        byte[] decoded = Base64.decode(encodedKey);
        if (decoded == null) {
            throw new IOException("Invalid Base64 key data");
        }
        DataInputStream dio = new DataInputStream(new ByteArrayInputStream(decoded));
        if (!keyType.equals(new String(readData(dio), US_ASCII))) {
            throw new IOException("Malformed key - didn't start with type " + keyType);
        }
        SigningHandler signingHandler;
        if (keyType.equals("ssh-dss")) {
            debug.debug("AuthorizedKeys: Found DSA key, ignoring as unsupported by JWT");
            return null;
        } else if (keyType.equals("ssh-rsa")) {
            signingHandler = createRsaSigningHandler(dio);
        } else if (keyType.startsWith("ecdsa-sha2-")) {
            signingHandler = createEcdsaSigningHandler(keyType, dio);
        } else {
            throw new IOException("Unsupported key type: " + keyType);
        }
        return new AuthorizedKey(encodedKey, signingHandler, keyOptions);
    }

    private SigningHandler createRsaSigningHandler(DataInputStream dio) throws IOException, GeneralSecurityException {
        PublicKey key; // https://tools.ietf.org/html/rfc4253#section-6.6
        BigInteger exponent = new BigInteger(readData(dio));
        BigInteger modulus = new BigInteger(readData(dio));
        key = KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        return new RSASigningHandler(key, SignatureUtil.getInstance());
    }

    private SigningHandler createEcdsaSigningHandler(String keyType, DataInputStream dio)
            throws IOException, GeneralSecurityException {
        // https://tools.ietf.org/html/rfc5656#section-6.1
        String curveIdentifier = keyType.substring("ecdsa-sha2-".length());
        SupportedEllipticCurve curve = SSH_ELLIPTIC_CURVES.get(curveIdentifier);
        if (curve == null) {
            throw new GeneralSecurityException("Unsupported elliptic curve key type: " + keyType);
        }

        // https://tools.ietf.org/html/rfc5656#section-3.1
        String keyTypeIdentifier = new String(readData(dio), US_ASCII);
        if (!keyTypeIdentifier.equals(curveIdentifier)) {
            debug.debug("AuthorizedKeys#createEcdsaSigningHandler: blob did not start with identifier {}: {}",
                    curveIdentifier, keyTypeIdentifier);
            throw new IOException("Misformatted key - blob should start with curve identifier");
        }
        ECPoint w = SecgUtils.getDecoded(readData(dio), ((ECParameterSpec) curve.getParameters()).getCurve());
        ECPublicKeySpec keySpec = new ECPublicKeySpec(w, ((ECParameterSpec) curve.getParameters()));
        ECPublicKey key = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(keySpec);
        return new ECDSASigningHandler(key);
    }

    private static byte[] readData(DataInputStream dio) throws IOException {
        int length = dio.readInt();
        byte[] data = new byte[length];
        if (dio.read(data) != length) {
            throw new IllegalStateException("Cannot read enough bytes from key");
        }
        return data;
    }
}
