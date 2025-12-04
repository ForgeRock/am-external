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

package org.forgerock.openam.authentication.modules.amster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bouncycastle.util.Arrays.concatenate;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.ECDSASigningHandler;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Algorithm;
import org.forgerock.util.Pair;
import org.forgerock.util.SignatureUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;

@ExtendWith({MockitoExtension.class})
public class AuthorizedKeysTest {
    private static final Provider BC_PROVIDER = new BouncyCastleProvider();
    private static final JSch JSCH = new JSch();
    private static final Pattern SSH_KEY_PATTERN = Pattern.compile("(AAAA[A-Za-z0-9+/=]+)");
    private static final Mutable<Pair<PrivateKey, KeyPair>> rsaKeyPair = new MutableObject<>();
    private static final Mutable<Pair<PrivateKey, KeyPair>> ec256KeyPair = new MutableObject<>();
    private static final Mutable<Pair<PrivateKey, KeyPair>> ec384KeyPair = new MutableObject<>();
    private static final Mutable<Pair<PrivateKey, KeyPair>> ec521KeyPair = new MutableObject<>();
    private static final Mutable<Pair<PrivateKey, KeyPair>> dsaKeyPair = new MutableObject<>();
    private static final Mutable<SigningHandler> rsa = new MutableObject<>();
    private static final Mutable<SigningHandler> ec256 = new MutableObject<>();
    private static final Mutable<SigningHandler> ec384 = new MutableObject<>();
    private static final Mutable<SigningHandler> ec521 = new MutableObject<>();
    @Mock
    private Logger debug;

    @BeforeAll
    static void classSetup() throws Exception {
        Security.addProvider(BC_PROVIDER);
        rsaKeyPair.setValue(generateKeyPair(KeyPair.RSA, 2048));
        ec256KeyPair.setValue(generateKeyPair(KeyPair.ECDSA, 256));
        ec384KeyPair.setValue(generateKeyPair(KeyPair.ECDSA, 384));
        ec521KeyPair.setValue(generateKeyPair(KeyPair.ECDSA, 521));
        dsaKeyPair.setValue(generateKeyPair(KeyPair.DSA, 1024));
        rsa.setValue(new RSASigningHandler(rsaKeyPair.getValue().getFirst(), SignatureUtil.getInstance()));
        ec256.setValue(new ECDSASigningHandler((ECPrivateKey) ec256KeyPair.getValue().getFirst()));
        ec384.setValue(new ECDSASigningHandler((ECPrivateKey) ec384KeyPair.getValue().getFirst()));
        ec521.setValue(new ECDSASigningHandler((ECPrivateKey) ec521KeyPair.getValue().getFirst()));
    }

    @AfterAll
    static void removeProvider() {
        Security.removeProvider(BC_PROVIDER.getName());
    }

    private static Stream<Arguments> algorithms() {
        return Stream.of(
                Arguments.of(rsaKeyPair, rsa, JwsAlgorithm.RS256),
                Arguments.of(ec256KeyPair, ec256, JwsAlgorithm.ES256),
                Arguments.of(ec384KeyPair, ec384, JwsAlgorithm.ES384),
                Arguments.of(ec521KeyPair, ec521, JwsAlgorithm.ES512)
        );
    }

    static Pair<PrivateKey, KeyPair> generateKeyPair(int type, int size) throws Exception {
        KeyPair keyPair = KeyPair.genKeyPair(JSCH, type, size);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keyPair.writePrivateKey(baos);
        Reader keyReader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        PEMKeyPair key = (PEMKeyPair) new PEMParser(keyReader).readObject();
        PrivateKey privateKey = new JcaPEMKeyConverter().getPrivateKey(key.getPrivateKeyInfo());

        return Pair.of(privateKey, keyPair);
    }

    @Test
    void shouldReadAnEmptyStream() throws Exception {
        // Given
        InputStream stream = new ByteArrayInputStream(new byte[0]);

        // When
        Set<Key> result = new AuthorizedKeys(debug).read(stream);

        // Then
        verifyNoMoreInteractions(debug);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldWarnAboutInvalidKeys() throws Exception {
        // Given
        InputStream stream = new ByteArrayInputStream("ssh-rsa AAAAAwnotarealkey===\n".getBytes());

        // When
        Set<Key> result = new AuthorizedKeys(debug).read(stream);

        // Then
        verify(debug, atLeast(0)).debug(anyString());
        verify(debug, atLeast(0)).debug(anyString(), any(), any(), any());
        verify(debug, times(1)).warn(contains("Could not read key"), isA(Exception.class));
        verifyNoMoreInteractions(debug);
        assertThat(result).isEmpty();
    }

    @Test
    void initShouldReadAuthorizedKeys() throws Exception {
        // Given
        InputStream stream = new ByteArrayInputStream(makeAuthorizedKeys());

        // When
        Set<Key> result = new AuthorizedKeys(debug).read(stream);

        // Then
        verify(debug, times(1)).debug(contains("Found DSA key, ignoring as unsupported"));
        verify(debug, atLeast(0)).debug(anyString(), any(), any(), any());
        verifyNoMoreInteractions(debug);
        assertThat(result).hasSize(4);
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    public void shouldSetupCorrectSigningHandlerForAlgorithm(Mutable<Pair<PrivateKey, KeyPair>> keys,
            Mutable<SigningHandler> privateKeySigningHandler, Algorithm alg) throws Exception {
        // Given
        String line = new String(authorizedKeysLineFor(keys.getValue().getSecond(), alg.getAlgorithm() + " key"));
        AuthorizedKey key = new AuthorizedKeys(debug).parseKey(line);

        Matcher matcher = SSH_KEY_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new Exception("Could not match pattern with string: " + line);
        }
        String jwtString = new SignedJwtBuilderImpl(privateKeySigningHandler.getValue())
                .claims(new JwtClaimsSetBuilder().sub("fred").build())
                .headers().kid(matcher.group(1)).alg(alg).done()
                .build();
        SignedJwt realJwt = new JwtReconstruction().reconstructJwt(jwtString, SignedJwt.class);
        SignedJwt capturingJwt = mock(SignedJwt.class);
        given(capturingJwt.getHeader()).willReturn(realJwt.getHeader());
        given(capturingJwt.verify(isA(SigningHandler.class))).willReturn(true);

        // When
        key.isValid(capturingJwt, mock(HttpServletRequest.class));

        // Then
        assertThat(key.isValid(realJwt, mock(HttpServletRequest.class))).isTrue();
        ArgumentCaptor<SigningHandler> handlerCaptor = ArgumentCaptor.forClass(SigningHandler.class);
        verify(capturingJwt).verify(handlerCaptor.capture());
        assertThat(realJwt.verify(handlerCaptor.getValue())).isTrue();
    }

    private byte[] authorizedKeysLineFor(KeyPair keyPair, String comment) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keyPair.writePublicKey(baos, comment);
        return baos.toByteArray();
    }

    private byte[] makeAuthorizedKeys() throws Exception {
        return concatenate(concatenate(
                        authorizedKeysLineFor(rsaKeyPair.getValue().getSecond(), "RSA"),
                        authorizedKeysLineFor(ec256KeyPair.getValue().getSecond(), "ECDSA"),
                        authorizedKeysLineFor(ec384KeyPair.getValue().getSecond(), "ECDSA"),
                        authorizedKeysLineFor(ec521KeyPair.getValue().getSecond(), "ECDSA")),
                authorizedKeysLineFor(dsaKeyPair.getValue().getSecond(), "DSA"));
    }


}
