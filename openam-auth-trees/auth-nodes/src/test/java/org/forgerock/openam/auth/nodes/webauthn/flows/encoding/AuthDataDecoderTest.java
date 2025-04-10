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
 * Copyright 2024-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.BitSet;
import java.util.stream.Stream;

import org.forgerock.openam.auth.nodes.webauthn.data.AttestationFlags;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.util.encode.Base64url;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class AuthDataDecoderTest {

    /**
     * Note: The following values were lifted from the FIDO Conformance Tool test cases, and so should reflect
     * representative real-world examples.
     */
    //@Checkstyle:off LineLength
    private static final String BASE64URL_ENCODED_AUTH_DATA_BASIC =
            "iDBKfeB2ElU453aXWtJGCit9A2HoybDhnnPkR6J_2pABAAAAMw";
    private static final String BASE64URL_ENCODED_AUTH_DATA_ATTESTATION_DATA_ONLY =
            "iDBKfeB2ElU453aXWtJGCit9A2HoybDhnnPkR6J_2pBBAAAAQjJq3PAM70bQk5KY1sSoSnIAIFk5qF4_KqVGEYyIpEfSV1agLWc4jPNgWVu7gIR8gs3OpQECAyYgASFYILQmCw0JpPJvkxhfwQgLT72fPnacU_fRILbPTpeJbL5bIlggcQBzuRonP38zmodkp2WKwSORchueh1-mHjpRWM1crxM";
    private static final String BASE64URL_ENCODED_AUTH_DATA_EXTENSION_DATA_ONLY =
            "iDBKfeB2ElU453aXWtJGCit9A2HoybDhnnPkR6J_2pCBAAAAUKFxZXhhbXBsZS5leHRlbnNpb254dlRoaXMgaXMgYW4gZXhhbXBsZSBleHRlbnNpb24hIElmIHlvdSByZWFkIHRoaXMgbWVzc2FnZSwgeW91IHByb2JhYmx5IHN1Y2Nlc3NmdWxseSBwYXNzaW5nIGNvbmZvcm1hbmNlIHRlc3RzLiBHb29kIGpvYiE";
    private static final String BASE64URL_ENCODED_AUTH_DATA_FULL_ATTESTATION_AND_EXTENSION_DATA =
            "iDBKfeB2ElU453aXWtJGCit9A2HoybDhnnPkR6J_2pDFAAAAAgAAAAAAAAAAAAAAAAAAAAAAQCFG1PHaBtUo9HibvfAQ3snK3GqszqJg3wfBlKDhFdwJKdMOPADAey4Qt5ywsl_DdNRB0hCb1WwNhOdZe0j2_d2lAQIDJiABIVggapo9smcllnWlFjUuqOBERNxPJd7jDpdj7QXsDveSjjMiWCDm-zDJ2cT0QtRijdTfzI8GFMzqHq9CfjlcWJqX1g6fBqFraG1hYy1zZWNyZXT1";
    //@Checkstyle:on LineLength

    private AuthDataDecoder authDataDecoder;

    @BeforeEach
    void setUp() {
        authDataDecoder = new AuthDataDecoder();
    }

    /**
     * Provides a stream of arguments including valid AuthData values (represented as base64url encoded strings)
     * and the expected values once decoded.
     *
     * @return a stream of valid AuthData values and the expected values
     */
    private static Stream<Arguments> validEncodedAuthData() {
        return Stream.of(
                Arguments.of(BASE64URL_ENCODED_AUTH_DATA_BASIC,
                        setExpectedFlags(true, false, false, false)),
                Arguments.of(BASE64URL_ENCODED_AUTH_DATA_ATTESTATION_DATA_ONLY,
                        setExpectedFlags(true, false, true, false)),
                Arguments.of(BASE64URL_ENCODED_AUTH_DATA_EXTENSION_DATA_ONLY,
                        setExpectedFlags(true, false, false, true)),
                Arguments.of(BASE64URL_ENCODED_AUTH_DATA_FULL_ATTESTATION_AND_EXTENSION_DATA,
                        setExpectedFlags(true, true, true, true))
        );
    }

    @ParameterizedTest
    @MethodSource("validEncodedAuthData")
    public void testDecodeValidAuthData(String encodedAuthData,
                                         AttestationFlags expectedFlags) throws Exception {
        byte[] authDataAsBytes = Base64url.decode(encodedAuthData);
        AuthData authData = authDataDecoder.decode(authDataAsBytes);
        assertEquals(expectedFlags.isUserPresent(),
                authData.attestationFlags.isUserPresent(), "UP flag did not match expected");
        assertEquals(expectedFlags.isUserVerified(),
                authData.attestationFlags.isUserVerified(), "UV flag did not match expected");
        assertEquals(expectedFlags.isAttestedDataIncluded(),
                authData.attestationFlags.isAttestedDataIncluded(), "AT flag did not match expected");
        assertEquals(expectedFlags.isExtensionDataIncluded(),
                authData.attestationFlags.isExtensionDataIncluded(), "ED flag did not match expected");
        if (expectedFlags.isAttestedDataIncluded()) {
            assertNotNull(authData.attestedCredentialData);
        }
    }

    private static AttestationFlags setExpectedFlags(boolean upFlag, boolean uvFlag, boolean atFlag, boolean edFlag) {
        BitSet flags = new BitSet(8);
        if (upFlag) {
            flags.set(0); // Set the UP (User Presence) flag
        }
        if (uvFlag) {
            flags.set(2); // Set the UV (User Verified) flag
        }
        if (atFlag) {
            flags.set(6); // Set the AT (Attested Credential Data) flag
        }
        if (edFlag) {
            flags.set(7); // Set the ED (Extension Data) flag
        }
        return new AttestationFlags(flags);
    }
}
