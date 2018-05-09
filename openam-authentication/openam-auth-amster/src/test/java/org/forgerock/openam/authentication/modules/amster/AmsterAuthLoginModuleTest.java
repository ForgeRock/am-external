/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.amster;

import static com.sun.identity.authentication.util.ISAuthConstants.LOGIN_START;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.guava.common.collect.Sets.newLinkedHashSet;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.contains;
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
import javax.servlet.http.HttpServletRequest;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.guava.common.collect.ImmutableMap;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.authentication.modules.common.AMLoginModuleBinder;
import org.forgerock.util.SignatureUtil;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;

public class AmsterAuthLoginModuleTest {

    private static final Subject SUBJECT = null;
    private static final Map SHARED_STATE = new HashMap();
    private static final Provider BC_PROVIDER = new BouncyCastleProvider();
    private static final String AUTHORIZED_KEYS_SETTING = "forgerock-am-auth-amster-authorized-keys";
    private static final String ENABLED_SETTING = "forgerock-am-auth-amster-enabled";
    public static final Set<String> TRUE = asSet("true");

    @Mock
    private Debug debug;
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

    @BeforeClass
    public void addProvider() {
        Security.addProvider(BC_PROVIDER);
    }

    @AfterClass
    public void removeProvider() {
        Security.removeProvider(BC_PROVIDER.getName());
    }

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        authorizedKeysFile = new File(System.getProperty("java.io.tmpdir"), randomUUID().toString());
        options = ImmutableMap.of(AUTHORIZED_KEYS_SETTING, asSet(authorizedKeysFile.getAbsolutePath()),
                ENABLED_SETTING, TRUE);
        this.module = new AmsterAuthLoginModule(debug, authorizedKeys);
        module.setAMLoginModule(binder);
        when(binder.getHttpServletRequest()).thenReturn(request);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getRemoteHost()).thenReturn("localhost");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        PrivateKey privateKey = keyGen.generateKeyPair().getPrivate();
        rsaSigningHandler = new RSASigningHandler(privateKey, SignatureUtil.getInstance());
    }

    @Test
    public void initShouldNotInitKeysWhenDisabled() throws Exception {
        // Given
        Map options = new HashMap();

        // When
        module.init(SUBJECT, SHARED_STATE, options);


        // Then
        verifyNoMoreInteractions(debug);
    }

    @Test
    public void initShouldReportErrorIfAuthorizedKeysSettingIsMissing() throws Exception {
        // Given
        Map options = singletonMap(ENABLED_SETTING, asSet("true"));

        // When
        module.init(SUBJECT, SHARED_STATE, options);

        // Then
        verify(debug).error(contains("No value for forgerock-am-auth-amster-authorized-keys"));
    }

    @Test
    public void initShouldReportErrorIfAuthorizedKeysDoesNotExist() throws Exception {
        // Given
        Map options = ImmutableMap.of(AUTHORIZED_KEYS_SETTING, asSet("/does/not/exist"), ENABLED_SETTING, TRUE);

        // When
        module.init(SUBJECT, SHARED_STATE, options);

        // Then
        verify(debug).error(contains("Could not read authorized keys file"), isA(FileNotFoundException.class));
        verify(authorizedKeys).read(isA(ByteArrayInputStream.class));
    }

    @Test
    public void initShouldSucceedIfAuthorizedKeysIsEmpty() throws Exception {
        // Given
        Files.write(authorizedKeysFile.toPath(), new byte[0]);

        // When
        module.init(SUBJECT, SHARED_STATE, options);

        // Then
        verifyNoMoreInteractions(debug);
    }

    @Test
    public void initShouldReadAuthorizedKeys() throws Exception {
        // Given
        Files.write(authorizedKeysFile.toPath(), "".getBytes());
        authorizedKeysFile.deleteOnExit();

        // When
        module.init(SUBJECT, SHARED_STATE, options);

        // Then
        verifyNoMoreInteractions(debug);
    }

    @Test(expectedExceptions = LoginException.class)
    public void shouldFailWhenDisabled() throws Exception {
        // Given
        Map options = new HashMap();
        module.init(SUBJECT, SHARED_STATE, options);

        Key passKey = mock(Key.class);
        given(passKey.isValid(isA(SignedJwt.class), isA(HttpServletRequest.class))).willReturn(true);
        given(authorizedKeys.read(isA(InputStream.class))).willReturn(singleton(passKey));
        JwtClaimsSet claims = new JwtClaimsSetBuilder().sub("amadmin").build();
        claims.setClaim("nonce", module.getNonce());
        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(claims)
                .build();

        // When
        module.process(new Callback[]{new HiddenValueCallback("jwt", jws)}, LOGIN_START);

        // Then - exception
    }

    @Test(expectedExceptions = LoginException.class)
    public void shouldThrowExceptionIfNoCallbacks() throws Exception {
        module.process(new Callback[0], LOGIN_START);
    }

    @Test
    public void shouldProcessSignedJwt() throws Exception {
        // Given
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

    @Test(expectedExceptions = LoginException.class)
    public void shouldFailWithoutNonce() throws Exception {
        // Given
        Key passKey = mock(Key.class);
        given(passKey.isValid(isA(SignedJwt.class), isA(HttpServletRequest.class))).willReturn(true);
        given(authorizedKeys.read(isA(InputStream.class))).willReturn(singleton(passKey));
        module.init(SUBJECT, SHARED_STATE, options);
        JwtClaimsSet claims = new JwtClaimsSetBuilder().sub("amadmin").build();
        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(claims)
                .build();

        // When
        module.process(new Callback[]{new HiddenValueCallback("jwt", jws)}, LOGIN_START);

        // Then - exception
    }

    @Test(expectedExceptions = LoginException.class)
    public void shouldFailIfNoMatchingKeys() throws Exception {
        // Given
        Key failKey = mock(Key.class);
        given(failKey.isValid(isA(SignedJwt.class), isA(HttpServletRequest.class))).willReturn(false);
        given(authorizedKeys.read(isA(InputStream.class))).willReturn(singleton(failKey));
        module.init(SUBJECT, SHARED_STATE, options);

        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(new JwtClaimsSetBuilder().sub("amadmin").build())
                .build();

        // When
        module.process(new Callback[] { new HiddenValueCallback("jwt", jws) }, LOGIN_START);

        // Then - exception
    }

}
