/*
 * Copyright 2016-2017 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.amster;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.exceptions.JwsSigningException;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sun.identity.shared.debug.Debug;

public class AuthorizedKeyTest {

    @Mock
    private SignedJwt jwt;
    @Mock
    private HttpServletRequest request;
    @Mock
    private SigningHandler signingHandler;
    @Mock
    private Debug debug;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        JwsHeader value = new JwsHeader();
        value.setKeyId("fred");
        when(jwt.getHeader()).thenReturn(value);
        when(jwt.getClaimsSet()).thenReturn(new JwtClaimsSetBuilder().sub("amadmin").build());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRequirePublicKey() throws Exception {
        AuthorizedKey authorizedKey = new AuthorizedKey(null, signingHandler, null, debug);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldRequireSigningHandler() throws Exception {
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", null, null, debug);
    }

    @Test
    public void shouldVerifyJwtIsValidWithWildcardSubjectOption() throws Exception {
        // Given
        Key key = new AuthorizedKey("fred", signingHandler, "subject=\"*\"", debug);
        given(jwt.verify(any(SigningHandler.class))).willThrow(new IllegalArgumentException());

        // When
        boolean result = key.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldVerifyJwtIsValidWithSubjectOption() throws Exception {
        // Given
        Key key = new AuthorizedKey("fred", signingHandler, "subject=\"alice,amadmin,fred\"", debug);
        given(jwt.verify(any(SigningHandler.class))).willThrow(new IllegalArgumentException());

        // When
        boolean result = key.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldVerifyJwtIsValidWithSubjectContainingComma() throws Exception {
        // Given
        Key key = new AuthorizedKey("fred", signingHandler, "subject=\"alice,ama\\\\,dmin,fred\"", debug);
        given(jwt.verify(any(SigningHandler.class))).willThrow(new IllegalArgumentException());
        jwt.getClaimsSet().setSubject("ama,dmin");

        // When
        boolean result = key.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldVerifyJwtIsValidWithPublicKey() throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, null, debug);
        given(jwt.verify(any(SigningHandler.class))).willReturn(true);

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void shouldVerifyJwtIsInvalidWithMismatchingPublicKey() throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("freddy", signingHandler, null, debug);
        given(jwt.verify(any(SigningHandler.class))).willReturn(true);

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldVerifyJwtIsInvalid() throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, null, debug);
        given(jwt.verify(any(SigningHandler.class))).willReturn(false);

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldVerifyJwtIsInvalidFromIllegalArgument() throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, null, debug);
        given(jwt.verify(any(SigningHandler.class))).willThrow(new IllegalArgumentException());

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldVerifyJwtIsInvalidFromSubjectOption() throws Exception {
        // Given
        Key key = new AuthorizedKey("fred", signingHandler, "subject=\"alice,fred\"", debug);
        given(jwt.verify(any(SigningHandler.class))).willReturn(true);

        // When
        boolean result = key.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldVerifyJwtIsInvalidFromSigningException() throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, null, debug);
        given(jwt.verify(any(SigningHandler.class))).willThrow(new JwsSigningException(""));

        // When
        boolean result = authorizedKey.isValid(jwt, request);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    public void shouldApproveRequestsWhenNoFromRestriction() throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler,
                "agent-forwarding,environment=\"fred=\\\"a string\\\"\",nopty", debug);

        // When
        boolean result = authorizedKey.isValidOrigin(request);

        // Then
        assertThat(result).isTrue();
    }

    @DataProvider
    public Object[][] matchingFrom() {
        return new Object[][] {
                { "127.0.0.1,fred.co.uk", "127.0.0.1", "localhost" },
                { "127.0.0.?,!127.0.0.1,fred.co.uk", "127.0.0.2", "localhost" },
                { "127.0.0.1,::1,fred.co.uk", "::1", "localhost" },
                { "127.0.0.1,::?,fred.co.uk", "::?", "localhost" },
                { "127.0.0.0/24,fred.co.uk", "127.0.0.1", "localhost" },
                { "127.0.0.1,::1/128,fred.co.uk", "::1", "localhost" },
        };
    }

    @Test(dataProvider = "matchingFrom")
    public void shouldApproveRequests(String from, String address, String host) throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, "from=\"" + from + "\"", debug);
        setRequestOrigin(address, host);

        // When
        boolean result = authorizedKey.isValidOrigin(request);

        // Then
        assertThat(result).isTrue();
    }

    @DataProvider
    public Object[][] failingFrom() {
        return new Object[][] {
                { "127.0.0.?,::1,*.co.uk", "::2", "localhost" },
                { "127.0.0.?,::1,*.co.uk", "192.168.0.1", "localhost" },
                { "192.168.0.0/24,!192.168.0.1,::1,*.co.uk", "192.168.0.1", "localhost" },
                { "127.0.0.?,::aaaa:0/128,*.co.uk", "::1", "localhost" },
                { "127.0.0.0/24,::aaaa:0/128,*.co.uk", "192.168.0.1", "localhost" },
                { "127.0.0.1,fred.co.uk", "127.0.0.2", "fred.co.uk" },
                { "127.0.0.1,*.co.uk", "127.0.0.2", "fred.co.uk" },
        };
    }

    @Test(dataProvider = "failingFrom")
    public void shouldDenyRequests(String from, String address, String host) throws Exception {
        // Given
        AuthorizedKey authorizedKey = new AuthorizedKey("fred", signingHandler, "from=\"" + from + "\"", debug);
        setRequestOrigin(address, host);

        // When
        boolean result = authorizedKey.isValidOrigin(request);

        // Then
        assertThat(result).isFalse();
    }

    private void setRequestOrigin(String address, String host) {
        when(request.getRemoteAddr()).thenReturn(address);
        when(request.getRemoteHost()).thenReturn(host);
    }

}
