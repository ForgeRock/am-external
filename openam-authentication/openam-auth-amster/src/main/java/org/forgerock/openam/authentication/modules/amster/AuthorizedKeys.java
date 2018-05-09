/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.amster;

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
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.guava.common.io.CharStreams;
import org.forgerock.json.jose.jws.SupportedEllipticCurve;
import org.forgerock.json.jose.jws.handlers.ECDSASigningHandler;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.util.SignatureUtil;
import org.forgerock.util.encode.Base64;

import com.sun.identity.shared.debug.Debug;

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

    private final Debug debug;

    AuthorizedKeys(Debug debug) {
        this.debug = debug;
    }

    Set<Key> read(InputStream authorizedKeys) {
        Set<Key> keys = new HashSet<>();

        for (String line : readLines(authorizedKeys)) {
            try {
                AuthorizedKey key = parseKey(line);
                if (key != null) {
                    keys.add(key);
                }
            } catch (IOException | GeneralSecurityException e) {
                debug.warning("AmsterAuthLoginModule#init: Could not read key " + line, e);
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
            debug.message("AuthorizedKeys: Not matched as a key line: {}", line);
            return null;
        }
        String keyOptions = matcher.group(1);
        String keyType = matcher.group(2);
        String encodedKey = matcher.group(3);
        debug.message("AuthorizedKeys: Read public key of type {} with options {}: {}", keyType,
                keyOptions, encodedKey);

        DataInputStream dio = new DataInputStream(new ByteArrayInputStream(Base64.decode(encodedKey)));
        if (!keyType.equals(new String(readData(dio), US_ASCII))) {
            throw new IOException("Malformed key - didn't start with type " + keyType);
        }
        SigningHandler signingHandler;
        if (keyType.equals("ssh-dss")) {
            debug.message("AuthorizedKeys: Found DSA key, ignoring as unsupported by JWT");
            return null;
        } else if (keyType.equals("ssh-rsa")) {
            signingHandler = createRsaSigningHandler(dio);
        } else if (keyType.startsWith("ecdsa-sha2-")) {
            signingHandler = createEcdsaSigningHandler(keyType, dio);
        } else {
            throw new IOException("Unsupported key type: " + keyType);
        }
        return new AuthorizedKey(encodedKey, signingHandler, keyOptions, debug);
    }

    private SigningHandler createRsaSigningHandler(DataInputStream dio) throws IOException, GeneralSecurityException {
        PublicKey key;// https://tools.ietf.org/html/rfc4253#section-6.6
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
            debug.message("AuthorizedKeys#createEcdsaSigningHandler: blob did not start with identifier {}: {}",
                    curveIdentifier, keyTypeIdentifier);
            throw new IOException("Misformatted key - blob should start with curve identifier");
        }
        ECPoint w = SecgUtils.getDecoded(readData(dio), curve.getParameters().getCurve());
        ECPublicKeySpec keySpec = new ECPublicKeySpec(w, curve.getParameters());
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
