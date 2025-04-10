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
 * Copyright 2016-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.amster;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.callback.Callback;
import jakarta.servlet.http.HttpServletRequest;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.builders.SignedJwtBuilderImpl;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.IdentifiedIdentity;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.util.SignatureUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sun.identity.authentication.callbacks.HiddenValueCallback;
import com.sun.identity.idm.IdType;

@ExtendWith(MockitoExtension.class)
public class AmsterJwtDecisionNodeTest {

    public static final Set<String> TRUE = asSet("true");
    private static final Provider BC_PROVIDER = new BouncyCastleProvider();
    @Mock
    private HttpServletRequest request;
    @Mock
    private AuthorizedKeys authorizedKeys;
    @Mock
    private AmsterJwtDecisionNode.Config config;
    private AmsterJwtDecisionNode node;
    private Path authorizedKeysFile;
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
        authorizedKeysFile = Files.createFile(Path.of(Optional.ofNullable(System.getenv("TMPDIR"))
                .orElse(System.getProperty("java.io.tmpdir")), randomUUID().toString()));
        node = new AmsterJwtDecisionNode(config, authorizedKeys);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        PrivateKey privateKey = keyGen.generateKeyPair().getPrivate();
        rsaSigningHandler = new RSASigningHandler(privateKey, SignatureUtil.getInstance());
    }

    @Test
    void shouldReturnNonceOnFirstExecution() throws Exception {
        //Given

        //When
        Action action = node.process(getContext(json(object()), List.of()));

        //Then
        assertThat(action.callbacks).hasSize(1).hasExactlyElementsOfTypes(HiddenValueCallback.class);
        assertThat(((HiddenValueCallback) action.callbacks.get(0)).getValue()).isNotEmpty();
    }

    @Test
    void shouldReturnFalseIfNoncePresentButNoCallbacks() throws Exception {
        //Given

        //When
        Action action = node.process(getContext(json(object(field("amster.nonce", "NONCE-Y"))), List.of()));

        //Then
        assertThat(action.outcome).isEqualTo("false");
    }

    @Test
    void shouldReturnFalseIfNonceAndCallbackPresentButInvalidJwt() throws Exception {
        //Given

        //When
        Action action = node.process(getContext(json(object(field("amster.nonce", "NONCE-Y"))),
                List.of(new HiddenValueCallback("jwt", "jwty-not-jwt"))));

        //Then
        assertThat(action.outcome).isEqualTo("false");
    }

    @Test
    void shouldReturnTrueIfNonceAndCallbackPresentWithValidJwt() throws Exception {
        //Given
        Key failKey = mock(Key.class);
        given(failKey.isValid(isA(SignedJwt.class), isA(HttpServletRequest.class))).willReturn(false);
        Key passKey = mock(Key.class);
        given(passKey.isValid(isA(SignedJwt.class), isA(HttpServletRequest.class))).willReturn(true);
        given(config.authorizedKeys()).willReturn(authorizedKeysFile.toString());
        given(authorizedKeys.read(isA(InputStream.class))).willReturn(newLinkedHashSet(List.of(failKey, passKey)));

        JwtClaimsSet claims = new JwtClaimsSetBuilder().sub("amadmin").build();
        claims.setClaim("nonce", "NONCE-Y");
        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(claims)
                .build();

        //When
        Action action = node.process(getContext(json(object(field("amster.nonce", "NONCE-Y"))),
                List.of(new HiddenValueCallback("jwt", jws))));

        //Then
        assertThat(action.outcome).isEqualTo("true");
        assertThat(action.identifiedIdentity).isPresent().contains(new IdentifiedIdentity("amadmin", IdType.USER));
    }

    @Test
    void shouldReturnFalseIfNonceAndCallbackPresentWithJwtMissingNonce() throws Exception {
        //Given
        JwtClaimsSet claims = new JwtClaimsSetBuilder().sub("amadmin").build();
        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(claims)
                .build();

        //When
        Action action = node.process(getContext(json(object(field("amster.nonce", "NONCE-Y"))),
                List.of(new HiddenValueCallback("jwt", jws))));

        //Then
        assertThat(action.outcome).isEqualTo("false");
    }

    @Test
    void shouldReturnFalseIfNonceAndCallbackPresentWithJwtIncorrectNonce() throws Exception {
        //Given
        JwtClaimsSet claims = new JwtClaimsSetBuilder().sub("amadmin").build();
        claims.setClaim("nonce", "NONCE-Z");
        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(claims)
                .build();

        //When
        Action action = node.process(getContext(json(object(field("amster.nonce", "NONCE-Y"))),
                List.of(new HiddenValueCallback("jwt", jws))));

        //Then
        assertThat(action.outcome).isEqualTo("false");
    }

    @Test
    void shouldReturnFalseIfAuthorisedKeysFileNotFound() throws Exception {
        //Given
        given(config.authorizedKeys()).willReturn(authorizedKeysFile.resolve("not-here").toString());

        JwtClaimsSet claims = new JwtClaimsSetBuilder().sub("amadmin").build();
        claims.setClaim("nonce", "NONCE-Y");
        String jws = new SignedJwtBuilderImpl(rsaSigningHandler)
                .headers().alg(JwsAlgorithm.RS256).done()
                .claims(claims)
                .build();

        //When
        Action action = node.process(getContext(json(object(field("amster.nonce", "NONCE-Y"))),
                List.of(new HiddenValueCallback("jwt", jws))));

        //Then
        assertThat(action.outcome).isEqualTo("false");
    }

    private TreeContext getContext(JsonValue transientState, List<? extends Callback> callbacks) {
        return new TreeContext(json(object()), transientState, json(object()),
                new ExternalRequestContext.Builder().servletRequest(request).build(), callbacks, false,
                Optional.of("bob"));
    }
}
