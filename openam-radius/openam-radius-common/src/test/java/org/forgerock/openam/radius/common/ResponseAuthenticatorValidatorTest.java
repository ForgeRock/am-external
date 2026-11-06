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
 * Copyright 2026 ForgeRock AS.
 */
package org.forgerock.openam.radius.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

class ResponseAuthenticatorValidatorTest {

    private static final String SECRET = "sharedSecret";
    private static final short IDENTIFIER = 42;

    private final ResponseAuthenticatorValidator validator = new ResponseAuthenticatorValidator();

    @Test
    void shouldAcceptResponseGivenAuthenticatorComputedWithSharedSecret() throws Exception {
        // Given
        Authenticator requestAuthenticator = requestAuthenticator();
        Packet response = signedResponse(requestAuthenticator, SECRET);

        // When
        boolean valid = validator.isValid(response, requestAuthenticator, SECRET);

        // Then
        assertThat(valid).isTrue();
    }

    @Test
    void shouldRejectResponseGivenAuthenticatorComputedWithDifferentSecret() throws Exception {
        // Given
        Authenticator requestAuthenticator = requestAuthenticator();
        Packet response = signedResponse(requestAuthenticator, "notTheSharedSecret");

        // When
        boolean valid = validator.isValid(response, requestAuthenticator, SECRET);

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    void shouldRejectResponseGivenAllZeroAuthenticator() {
        // Given
        Authenticator requestAuthenticator = requestAuthenticator();
        Packet response = response(() -> new byte[16]);

        // When
        boolean valid = validator.isValid(response, requestAuthenticator, SECRET);

        // Then
        assertThat(valid).isFalse();
    }

    @Test
    void shouldAcceptResponseGivenAuthenticatorSurvivesSerialisationRoundTrip() throws Exception {
        // Given
        Authenticator requestAuthenticator = requestAuthenticator();
        Packet received = PacketFactory.toPacket(signedResponse(requestAuthenticator, SECRET).getOctets());

        // When
        boolean valid = validator.isValid(received, requestAuthenticator, SECRET);

        // Then
        assertThat(valid).isTrue();
    }

    @Test
    void shouldRejectResponseGivenAuthenticatorOfADifferentRequest() throws Exception {
        // Given
        Packet response = signedResponse(requestAuthenticator(), SECRET);

        // When
        boolean valid = validator.isValid(response, otherRequestAuthenticator(), SECRET);

        // Then
        assertThat(valid).isFalse();
    }

    private Authenticator requestAuthenticator() {
        byte[] octets = new byte[16];
        for (int i = 0; i < octets.length; i++) {
            octets[i] = (byte) i;
        }
        return () -> octets;
    }

    private Authenticator otherRequestAuthenticator() {
        byte[] octets = new byte[16];
        for (int i = 0; i < octets.length; i++) {
            octets[i] = (byte) (100 + i);
        }
        return () -> octets;
    }

    /**
     * Builds a response bearing the Response Authenticator that a server holding the given secret would produce, as
     * described in section 3 of RFC 2865.
     */
    private Packet signedResponse(Authenticator requestAuthenticator, String secret)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        Packet response = response(requestAuthenticator);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(response.getOctets());
        md5.update(secret.getBytes("UTF-8"));
        response.setAuthenticator(new ResponseAuthenticator(md5.digest()));
        return response;
    }

    private Packet response(Authenticator authenticator) {
        Packet response = new AccessAccept();
        response.setIdentifier(IDENTIFIER);
        response.setAuthenticator(authenticator);
        return response;
    }
}
