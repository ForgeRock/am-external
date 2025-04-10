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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.exceptions.JwsSigningException;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthorizedKeyTest {

    @Mock
    private SignedJwt jwt;
    @Mock
    private HttpServletRequest request;
    @Mock
    private SigningHandler signingHandler;

    private static Stream<Arguments> matchingFrom() {
        return Stream.of(
                Arguments.of("127.0.0.1,fred.co.uk", "127.0.0.1", "localhost"),
                Arguments.of("127.0.0.?,!127.0.0.1,fred.co.uk", "127.0.0.2", "localhost"),
                Arguments.of("127.0.0.1,::1,fred.co.uk", "::1", "localhost"),
                Arguments.of("127.0.0.1,::?,fred.co.uk", "::?", "localhost"),
                Arguments.of("127.0.0.0/24,fred.co.uk", "127.0.0.1", "localhost"),
                Arguments.of("127.0.0.1,::1/128,fred.co.uk", "::1", "localhost")
        );
    }

    private static Stream<Arguments> fallingFrom() {
        return Stream.of(
                Arguments.of("127.0.0.?,::1,*.co.uk", "::2", "localhost"),
                Arguments.of("127.0.0.?,::1,*.co.uk", "192.168.0.1", "localhost"),
                Arguments.of("192.168.0.0/24,!192.168.0.1,::1,*.co.uk", "192.168.0.1", "localhost"),
                Arguments.of("127.0.0.?,::aaaa:0/128,*.co.uk", "::1", "localhost"),
                Arguments.of("127.0.0.0/24,::aaaa:0/128,*.co.uk", "192.168.0.1", "localhost"),
                Arguments.of("127.0.0.1,fred.co.uk", "127.0.0.2", "fred.co.uk"),
                Arguments.of("127.0.0.1,*.co.uk", "127.0.0.2", "fred.co.uk")
        );
    }

    private void addJwsHeaderStubbing() {
        JwsHeader value = new JwsHeader();
        value.setKeyId("fred");
        when(jwt.getHeader()).thenReturn(value);
    }

    @Test
    void shouldRequirePublicKey() {
        assertThatThrownBy(() -> new AuthorizedKey(null, signingHandler, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Public key is required");
    }

    @Test
    void shouldRequireSigningHandler() {
        assertThatThrownBy(() -> new AuthorizedKey("fred", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Signing handler is required");
    }

    @Test
    void shouldVerifyJwtIsValidWithWildcardSubjectOption() throws Exception {
        // Given
        addJwsHeaderStubbing();
        when(jwt.getClaimsSet()).thenReturn(new JwtClaimsSetBuilder().sub("amadmin").build());
        Key key = new AuthorizedKey("fred", signingHandler, "subject=\"*\"");
        given(jwt.verify(any(SigningHandler.class))).willThrow(new IllegalArgumentException());

        // When
        boolean result = key.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyJwtIsValidWithSubjectOption() throws Exception {
        // Given
        when(jwt.getClaimsSet()).thenReturn(new JwtClaimsSetBuilder().sub("amadmin").build());
        addJwsHeaderStubbing();
        Key key = new AuthorizedKey("fred", signingHandler, "subject=\"alice,amadmin,fred\"");
        given(jwt.verify(any(SigningHandler.class))).willThrow(new IllegalArgumentException());

        // When
        boolean result = key.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyJwtIsValidWithSubjectContainingComma() throws Exception {
        // Given
        when(jwt.getClaimsSet()).thenReturn(new JwtClaimsSetBuilder().sub("amadmin").build());
        Key key = new AuthorizedKey("fred", signingHandler, "subject=\"alice,ama\\\\,dmin,fred\"");
        jwt.getClaimsSet().setSubject("ama,dmin");

        // When
        boolean result = key.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyJwtIsValidWithPublicKey() throws Exception {
        // Given
        addJwsHeaderStubbing();
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, null);
        given(jwt.verify(any(SigningHandler.class))).willReturn(true);

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldVerifyJwtIsInvalidWithMismatchingPublicKey() throws Exception {
        // Given
        addJwsHeaderStubbing();
        AuthorizedKey authorizedKey = new AuthorizedKey("freddy", signingHandler, null);

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyJwtIsInvalid() throws Exception {
        // Given
        addJwsHeaderStubbing();
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, null);
        given(jwt.verify(any(SigningHandler.class))).willReturn(false);

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyJwtIsInvalidFromIllegalArgument() throws Exception {
        // Given
        addJwsHeaderStubbing();
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, null);
        given(jwt.verify(any(SigningHandler.class))).willThrow(new IllegalArgumentException());

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyJwtIsInvalidFromSubjectOption() throws Exception {
        // Given
        when(jwt.getClaimsSet()).thenReturn(new JwtClaimsSetBuilder().sub("amadmin").build());
        Key key = new AuthorizedKey("fred", signingHandler, "subject=\"alice,fred\"");

        // When
        boolean result = key.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyJwtIsInvalidFromSigningException() throws Exception {
        // Given
        addJwsHeaderStubbing();
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, null);
        given(jwt.verify(any(SigningHandler.class))).willThrow(new JwsSigningException(""));

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldApproveRequestsWhenNoFromRestriction() throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler,
                "agent-forwarding,environment=\"fred=\\\"a string\\\"\",nopty");

        // When
        boolean result = authorizedKey.isValidOrigin(request);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @MethodSource("matchingFrom")
    public void shouldApproveRequests(String from, String address, String host) throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, "from=\"" + from + "\"");
        setRequestOrigin(address);

        // When
        boolean result = authorizedKey.isValidOrigin(request);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @MethodSource("fallingFrom")
    public void shouldDenyRequests(String from, String address, String host) throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, "from=\"" + from + "\"");
        setRequestOrigin(address);

        // When
        boolean result = authorizedKey.isValidOrigin(request);

        // Then
        assertThat(result).isFalse();
    }

    private void setRequestOrigin(String address) {
        when(request.getRemoteAddr()).thenReturn(address);
    }

}
