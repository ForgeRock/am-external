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
 * Copyright 2023 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.oidc;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.openam.auth.node.api.SharedStateConstants.REALM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Provider;
import javax.script.Bindings;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.testsupport.JwtClaimsSetFactory;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.scripting.application.ScriptEvaluator;
import org.forgerock.openam.scripting.application.ScriptEvaluatorFactory;
import org.forgerock.openam.scripting.domain.EvaluatorVersion;
import org.forgerock.openam.scripting.domain.Script;
import org.forgerock.openam.scripting.domain.ScriptingLanguage;
import org.forgerock.openam.secrets.Secrets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mozilla.javascript.NativeJavaObject;

import com.iplanet.dpro.session.service.SessionService;

public class OidcNodeTest {
    private static final String JWT_TOKEN = "jwtToken";
    @Mock
    private OidcNode.Config config;
    @Mock
    private OidcIdTokenJwtHandlerFactory oidcIdTokenJwtHandlerFactory;
    @Mock
    private Realm realm;
    @Mock
    private Secrets secrets;
    @Mock
    private OidcIdTokenJwtHandler jwtHandler;
    @Mock
    private Script script;
    @Mock
    private ScriptEvaluator scriptEvaluator;
    @Mock
    private ScriptEvaluatorFactory scriptEvaluatorFactory;
    @Mock
    private Provider<SessionService> sessionServiceProvider;

    private OidcNode oidcNode;

    @Before
    public void setup() throws Exception {
        openMocks(this);
        given(config.oidcValidationType()).willReturn(OidcNode.OpenIdValidationType.WELL_KNOWN_URL);
        given(config.headerName()).willReturn("oidc_id_token");
        given(config.idTokenIssuer()).willReturn(JwtClaimsSetFactory.ISSUER_CONFIG);
        given(config.audienceName()).willReturn(JwtClaimsSetFactory.AUDIENCE_CONFIG);
        given(config.authorisedParties()).willReturn(Set.of(JwtClaimsSetFactory.AUTHORIZED_PARTIES_CONFIG));
        given(config.script()).willReturn(script);
        given(script.getEvaluatorVersion()).willReturn(EvaluatorVersion.defaultVersion());
        given(config.inputs()).willReturn(List.of("*"));
        given(config.unreasonableLifetimeLimit()).willReturn(60);
        given(oidcIdTokenJwtHandlerFactory.createOidcIdTokenJwtHandler(any(OidcNode.Config.class),
                any(Optional.class))).willReturn(jwtHandler);
        given(scriptEvaluatorFactory.create(any())).willReturn(scriptEvaluator);
        oidcNode = new OidcNode(config, realm, oidcIdTokenJwtHandlerFactory, secrets, scriptEvaluatorFactory,
                sessionServiceProvider);
    }

    @Test
    public void shouldProcessGivenIdTokenValueIsMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        // Given
        given(request.getHeader(config.headerName())).willReturn(null);

        JsonValue sharedState = JsonValue.json(field(REALM, "/realm"));
        JsonValue transientState = JsonValue.json(object());

        // When
        Action result = oidcNode.process(getContext(sharedState, transientState, request));

        // Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void shouldProcessGivenInvalidJwt() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        SignedJwt signedJwt = mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();

        // Given
        JsonValue sharedState = JsonValue.json(object());
        JsonValue transientState = JsonValue.json(object());
        TreeContext context = getContext(sharedState, transientState, request);
        context.getStateFor(oidcNode).putShared(REALM, "/realm");
        given(request.getHeader(config.headerName())).willReturn(JWT_TOKEN);
        given(jwtHandler.createJwtFromString(JWT_TOKEN)).willReturn(signedJwt);
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(jwtHandler.isJwtValid(signedJwt)).willReturn(false);

        // When
        Action result = oidcNode.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("false");
        assertThat(sharedState.isDefined(OidcNode.LOOKUP_ATTRIBUTES)).isFalse();
    }

    @Test
    public void shouldProcessGivenObjectDataThrowsException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);

        // Given
        JsonValue sharedState = JsonValue.json(object());
        JsonValue transientState = JsonValue.json(object());
        TreeContext context = getContext(sharedState, transientState, request);
        given(scriptEvaluator.evaluateScript(any(), any(), any()))
                .willThrow(new ScriptException("test exception"));

        // When
        Action result = oidcNode.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("false");
    }

    @Test
    public void shouldProcessGivenValidJwtAndJavascriptScript() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        SignedJwt signedJwt = mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();
        NativeJavaObject nativeJavaObject = mock(NativeJavaObject.class);

        // Given
        JsonValue sharedState = JsonValue.json(object());
        JsonValue transientState = JsonValue.json(object());
        TreeContext context = getContext(sharedState, transientState, request);
        context.getStateFor(oidcNode).putShared(REALM, "/realm");
        given(script.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(request.getHeader(config.headerName())).willReturn(JWT_TOKEN);
        given(jwtHandler.createJwtFromString(JWT_TOKEN)).willReturn(signedJwt);
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(jwtHandler.isJwtValid(signedJwt)).willReturn(true);
        given(nativeJavaObject.unwrap()).willReturn(JsonValue.json("a"));
        given(scriptEvaluator.evaluateScript(any(Script.class), any(Bindings.class), eq(realm)))
                .willReturn(nativeJavaObject);

        // When
        Action result = oidcNode.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(sharedState.isDefined(OidcNode.LOOKUP_ATTRIBUTES)).isTrue();
        assertThat(sharedState.get(OidcNode.LOOKUP_ATTRIBUTES).asString()).isEqualTo("a");
    }

    @Test
    public void shouldProcessGivenValidJwtAndGroovyScript() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        SignedJwt signedJwt = mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();

        // Given
        JsonValue sharedState = JsonValue.json(object());
        JsonValue transientState = JsonValue.json(object());
        TreeContext context = getContext(sharedState, transientState, request);
        context.getStateFor(oidcNode).putShared(REALM, "/realm");
        given(script.getLanguage()).willReturn(ScriptingLanguage.GROOVY);
        given(request.getHeader(config.headerName())).willReturn(JWT_TOKEN);
        given(jwtHandler.createJwtFromString(JWT_TOKEN)).willReturn(signedJwt);
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(jwtHandler.isJwtValid(signedJwt)).willReturn(true);
        given(scriptEvaluator.evaluateScript(any(Script.class), any(Bindings.class), eq(realm)))
                .willReturn(JsonValue.json("a"));

        // When
        Action result = oidcNode.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(sharedState.isDefined(OidcNode.LOOKUP_ATTRIBUTES)).isTrue();
        assertThat(sharedState.get(OidcNode.LOOKUP_ATTRIBUTES).asString()).isEqualTo("a");
    }

    @Test
    public void shouldProcessGivenValidJwtAndUndefinedProofOfPossession() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        SignedJwt signedJwt = mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet().build();
        NativeJavaObject nativeJavaObject = mock(NativeJavaObject.class);

        // Given
        JsonValue sharedState = JsonValue.json(object());
        JsonValue transientState = JsonValue.json(object());
        TreeContext context = getContext(sharedState, transientState, request);
        context.getStateFor(oidcNode).putShared(REALM, "/realm");
        given(script.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(request.getHeader(config.headerName())).willReturn(JWT_TOKEN);
        given(jwtHandler.createJwtFromString(JWT_TOKEN)).willReturn(signedJwt);
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(jwtHandler.isJwtValid(signedJwt)).willReturn(true);
        given(nativeJavaObject.unwrap()).willReturn(JsonValue.json("a"));
        given(scriptEvaluator.evaluateScript(any(Script.class), any(Bindings.class), eq(realm)))
                .willReturn(nativeJavaObject);

        // When
        Action result = oidcNode.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(sharedState.isDefined(OidcNode.LOOKUP_ATTRIBUTES)).isTrue();
        assertThat(sharedState.get(OidcNode.LOOKUP_ATTRIBUTES).asString()).isEqualTo("a");
        assertThat(sharedState.isDefined(OAuth2Constants.ProofOfPossession.CNF)).isFalse();
    }

    @Test
    public void shouldProcessGivenValidJwtAndDefinedProofOfPossession() throws Exception {
        String validCnf = "valid_cnf";
        HttpServletRequest request = mock(HttpServletRequest.class);
        SignedJwt signedJwt = mock(SignedJwt.class);
        JwtClaimsSet jwtClaimsSet = JwtClaimsSetFactory.aJwtClaimsSet()
                .withCustomAttribute(OAuth2Constants.ProofOfPossession.CNF, validCnf)
                .build();
        NativeJavaObject nativeJavaObject = mock(NativeJavaObject.class);

        // Given
        JsonValue sharedState = JsonValue.json(object());
        JsonValue transientState = JsonValue.json(object());
        TreeContext context = getContext(sharedState, transientState, request);
        context.getStateFor(oidcNode).putShared(REALM, "/realm");
        given(script.getLanguage()).willReturn(ScriptingLanguage.JAVASCRIPT);
        given(request.getHeader(config.headerName())).willReturn(JWT_TOKEN);
        given(jwtHandler.createJwtFromString(JWT_TOKEN)).willReturn(signedJwt);
        given(signedJwt.getClaimsSet()).willReturn(jwtClaimsSet);
        given(jwtHandler.isJwtValid(signedJwt)).willReturn(true);
        given(nativeJavaObject.unwrap()).willReturn(JsonValue.json("a"));
        given(scriptEvaluator.evaluateScript(any(Script.class), any(Bindings.class), eq(realm)))
                .willReturn(nativeJavaObject);

        // When
        Action result = oidcNode.process(context);

        // Then
        assertThat(result.outcome).isEqualTo("true");
        assertThat(sharedState.isDefined(OidcNode.LOOKUP_ATTRIBUTES)).isTrue();
        assertThat(sharedState.get(OidcNode.LOOKUP_ATTRIBUTES).asString()).isEqualTo("a");
        assertThat(sharedState.isDefined("org.forgerock.openam.authentication.modules.jwtpop.cnf")).isTrue();
        assertThat(sharedState.get("org.forgerock.openam.authentication.modules.jwtpop.cnf").asString())
                .isEqualTo(validCnf);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, HttpServletRequest request) {
        return new TreeContext(sharedState, transientState,
                new ExternalRequestContext.Builder()
                        .servletRequest(request).build(), emptyList(), Optional.empty());
    }
}
