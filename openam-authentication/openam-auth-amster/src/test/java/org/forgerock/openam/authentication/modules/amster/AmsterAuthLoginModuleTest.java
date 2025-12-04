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

import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.sun.identity.authentication.util.ISAuthConstants.LOGIN_START;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import jakarta.servlet.http.HttpServletRequest;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.authentication.modules.common.AMLoginModuleBinder;
import org.forgerock.util.SignatureUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.util.ISAuthConstants;

@ExtendWith(MockitoExtension.class)
public class AmsterAuthLoginModuleTest {

    public static final Set<String> TRUE = asSet("true");
    private static final Subject SUBJECT = null;
    private static final Map SHARED_STATE = new HashMap();
    private static final Provider BC_PROVIDER = new BouncyCastleProvider();
    private static final String AUTHORIZED_KEYS_SETTING = "forgerock-am-auth-amster-authorized-keys";
    private static final String ENABLED_SETTING = "forgerock-am-auth-amster-enabled";
    @Mock
    private Logger debug;
    @Mock
    private AMLoginModuleBinder binder;
    @Mock
    private HttpServletRequest request;
    @Mock
    private AuthorizedKeys authorizedKeys;
    private AmsterAuthLoginModule module;
    private File authorizedKeysFile;
    private ImmutableMap<String, Set<String>> options;
    private SigningHandler rsaSigningHandler;

    @BeforeAll
    static void addProvider() {
        Security.addProvider(BC_PROVIDER);
    }

    @AfterAll
    static void removeProvider() {
        Security.removeProvider(BC_PROVIDER.getName());
    }

    @BeforeEach
    void setup() throws Exception {
        authorizedKeysFile = new File(System.getProperty("java.io.tmpdir"), randomUUID().toString());
        options = ImmutableMap.of(AUTHORIZED_KEYS_SETTING, asSet(authorizedKeysFile.getAbsolutePath()),
                ENABLED_SETTING, TRUE);
        this.module = new AmsterAuthLoginModule(debug, authorizedKeys);
        module.setAMLoginModule(binder);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        PrivateKey privateKey = keyGen.generateKeyPair().getPrivate();
        rsaSigningHandler = new RSASigningHandler(privateKey, SignatureUtil.getInstance());
    }

    @Test
    void initShouldNotInitKeysWhenDisabled() throws Exception {
        // Given
        Map options = new HashMap();

        // When
        module.init(SUBJECT, SHARED_STATE, options);


        // Then
        verifyNoMoreInteractions(debug);
    }

    @Test
    void initShouldReportErrorIfAuthorizedKeysSettingIsMissing() throws Exception {
        // Given
        Map options = singletonMap(ENABLED_SETTING, asSet("true"));

        // When
        module.init(SUBJECT, SHARED_STATE, options);

        // Then
        verify(debug).error(contains("No value for forgerock-am-auth-amster-authorized-keys"));
    }

    @Test
    void initShouldReportErrorIfAuthorizedKeysDoesNotExist() throws Exception {
        // Given
        Map options = ImmutableMap.of(AUTHORIZED_KEYS_SETTING, asSet("/does/not/exist"), ENABLED_SETTING, TRUE);

        // When
        module.init(SUBJECT, SHARED_STATE, options);

        // Then
        verify(debug).error(contains("Could not read authorized keys file"), isA(FileNotFoundException.class));
        verify(authorizedKeys).read(isA(ByteArrayInputStream.class));
    }

    @Test
    void initShouldSucceedIfAuthorizedKeysIsEmpty() throws Exception {
        // Given
        Files.write(authorizedKeysFile.toPath(), new byte[0]);

        // When
        module.init(SUBJECT, SHARED_STATE, options);

        // Then
        verifyNoMoreInteractions(debug);
    }

    @Test
    void initShouldReadAuthorizedKeys() throws Exception {
        // Given
        Files.write(authorizedKeysFile.toPath(), "".getBytes());
        authorizedKeysFile.deleteOnExit();

        // When
        module.init(SUBJECT, SHARED_STATE, options);

        // Then
        verifyNoMoreInteractions(debug);
    }

    @Test
    void shouldFailWhenDisabled() throws Exception {
        // Given
        Map options = new HashMap();
        module.init(SUBJECT, SHARED_STATE, options);

        Key passKey = mock(Key.class);
        JwtClaimsSet claims = new JwtClaimsSetBuilder().sub("amadmin").build();
        claims.setClaim("nonce", module.getNonce());
        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(claims)
                .build();

        // When
        assertThatThrownBy(() ->
                module.process(new Callback[]{new HiddenValueCallback("jwt", jws)}, LOGIN_START))
                // Then
                .isInstanceOf(LoginException.class)
                .hasMessage("Login disabled");
    }

    @Test
    void shouldThrowExceptionIfNoCallbacks() throws Exception {
        assertThatThrownBy(() -> module.process(new Callback[0], LOGIN_START))
                .isInstanceOf(LoginException.class)
                .hasMessage("Login disabled");
    }

    @Test
    void shouldProcessSignedJwt() throws Exception {
        // Given
        when(binder.getHttpServletRequest()).thenReturn(request);
        Key failKey = mock(Key.class);
        given(failKey.isValid(isA(SignedJwt.class), isA(HttpServletRequest.class))).willReturn(false);
        Key passKey = mock(Key.class);
        given(passKey.isValid(isA(SignedJwt.class), isA(HttpServletRequest.class))).willReturn(true);
        given(authorizedKeys.read(isA(InputStream.class))).willReturn(newLinkedHashSet(asList(failKey, passKey)));
        module.init(SUBJECT, SHARED_STATE, options);
        JwtClaimsSet claims = new JwtClaimsSetBuilder().sub("amadmin").build();
        claims.setClaim("nonce", module.getNonce());
        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(claims)
                .build();

        // When
        int result = module.process(new Callback[]{new HiddenValueCallback("jwt", jws)}, LOGIN_START);

        // Then
        assertThat(result).isEqualTo(ISAuthConstants.LOGIN_SUCCEED);
        assertThat(module.getPrincipal().getName()).isEqualTo("amadmin");
    }

    @Test
    void shouldFailWithoutNonce() throws Exception {
        // Given
        Key passKey = mock(Key.class);
        given(authorizedKeys.read(isA(InputStream.class))).willReturn(singleton(passKey));
        module.init(SUBJECT, SHARED_STATE, options);
        JwtClaimsSet claims = new JwtClaimsSetBuilder().sub("amadmin").build();
        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(claims)
                .build();

        // When
        assertThatThrownBy(()
                -> module.process(new Callback[]{new HiddenValueCallback("jwt", jws)}, LOGIN_START))
                // Then
                .isInstanceOf(LoginException.class)
                .hasMessage("Not authenticated");

    }

    @Test
    void shouldFailIfNoMatchingKeys() throws Exception {
        // Given
        Key failKey = mock(Key.class);
        given(authorizedKeys.read(isA(InputStream.class))).willReturn(singleton(failKey));
        module.init(SUBJECT, SHARED_STATE, options);

        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(new JwtClaimsSetBuilder().sub("amadmin").build())
                .build();

        // When
        assertThatThrownBy(()
                -> module.process(new Callback[]{new HiddenValueCallback("jwt", jws)}, LOGIN_START))
                // Then
                .isInstanceOf(LoginException.class)
                .hasMessage("Not authenticated");

    }

}
