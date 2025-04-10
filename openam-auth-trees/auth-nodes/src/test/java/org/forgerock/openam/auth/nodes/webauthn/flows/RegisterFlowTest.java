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
package org.forgerock.openam.auth.nodes.webauthn.flows;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.BitSet;
import java.util.Set;

import org.forgerock.openam.auth.nodes.webauthn.WebAuthnRegistrationNode;
import org.forgerock.openam.auth.nodes.webauthn.cose.CoseAlgorithm;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationFlags;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationObject;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AttestationDecoder;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.AttestationVerifierFactory;
import org.forgerock.openam.auth.nodes.webauthn.flows.encoding.EncodingUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.exceptions.InvalidDataException;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.NoneVerifier;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.util.encode.Base64url;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RegisterFlowTest {
    private static final int SECRET_BYTE_LENGTH = 32;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String rpId = "https://example.com";
    private final String format = "testFormat";
    @Mock
    Realm realm;
    @Mock
    WebAuthnRegistrationNode.Config config;
    @Mock
    AttestationDecoder attestationDecoder;
    @Mock
    FlowUtilities flowUtilities;
    @Mock
    AttestationVerifierFactory attestationVerifierFactory;

    private RegisterFlow registerFlow;
    private AttestationObject attestationObject;
    private AuthData authData;
    private String clientData;
    private byte[] challengeBytes;
    private Set<String> origins;

    @BeforeEach
    void setup() throws Exception {
        registerFlow = new RegisterFlow(attestationDecoder, flowUtilities, attestationVerifierFactory, realm, config);

        challengeBytes = createRandomBytes();
        origins = Set.of(rpId);

        // client data as a JSON string
        clientData = "{\"challenge\":\"" + Base64url.encode(challengeBytes) + "\",\"origin\":\"" + rpId
                + "\",\"type\":\"webauthn.create\"}";

        BitSet flags = new BitSet(7);
        flags.set(0);
        flags.set(6);
        authData = new AuthData(
                EncodingUtilities.getHash(rpId),
                new AttestationFlags(flags),
                1,
                new AttestedCredentialData(null, 0, null, null, null),
                "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8)
        );
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        attestationObject = new AttestationObject(format, authData, statement);
    }

    @Test
    void shouldPassWithValidAttestationData() throws Exception {
        // Given
        given(attestationVerifierFactory.create(any(), any(), eq(format))).willReturn(new NoneVerifier());
        given(attestationDecoder.decode(any())).willReturn(attestationObject);
        given(flowUtilities.isOriginValid(eq(realm), eq(origins), eq(rpId))).willReturn(true);

        // When
        registerFlow.accept(clientData, new byte[32], challengeBytes, rpId, origins);

        // Then
        // No exception is thrown
    }

    @Test
    void shouldFailWithNullAttestationFlags() throws Exception {
        // Given
        authData = new AuthData(
                EncodingUtilities.getHash(rpId),
                null,
                1,
                new AttestedCredentialData(null, 0, null, null, null),
                "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8)
        );
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        attestationObject = new AttestationObject(format, authData, statement);

        given(attestationDecoder.decode(any())).willReturn(attestationObject);
        given(flowUtilities.isOriginValid(eq(realm), eq(origins), eq(rpId))).willReturn(true);

        // When
        assertThatThrownBy(() ->
                registerFlow.accept(clientData, new byte[32], challengeBytes, rpId, origins))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldFailWithMissingIsAttestedDataIncludedFlag() throws Exception {
        // Given
        BitSet flags = new BitSet();
        flags.set(0);
        authData = new AuthData(
                EncodingUtilities.getHash(rpId),
                new AttestationFlags(flags),
                1,
                new AttestedCredentialData(null, 0, null, null, null),
                "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8)
        );
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        attestationObject = new AttestationObject(format, authData, statement);

        given(attestationDecoder.decode(any())).willReturn(attestationObject);
        given(flowUtilities.isOriginValid(eq(realm), eq(origins), eq(rpId))).willReturn(true);

        // When
        assertThatThrownBy(() ->
                registerFlow.accept(clientData, new byte[32], challengeBytes, rpId,
                        origins))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("attested data bit not set");

    }

    @Test
    void shouldFailWithMissingAttestedCredentialData() throws Exception {
        // Given
        BitSet flags = new BitSet();
        flags.set(0);
        flags.set(6);
        authData = new AuthData(
                EncodingUtilities.getHash(rpId),
                new AttestationFlags(flags),
                1,
                null,
                "rawAuthenticatorData".getBytes(StandardCharsets.UTF_8)
        );
        AttestationStatement statement = new AttestationStatement();
        statement.setAlg(CoseAlgorithm.EDDSA);
        attestationObject = new AttestationObject(format, authData, statement);

        given(attestationDecoder.decode(any())).willReturn(attestationObject);
        given(flowUtilities.isOriginValid(eq(realm), eq(origins), eq(rpId))).willReturn(true);

        // When
        assertThatThrownBy(() ->
                registerFlow.accept(clientData, new byte[32], challengeBytes, rpId, origins))
                .isInstanceOf(InvalidDataException.class)
                .hasMessage("attested credential data not present");
    }

    private byte[] createRandomBytes() {
        byte[] secretBytes = new byte[SECRET_BYTE_LENGTH];
        secureRandom.nextBytes(secretBytes);
        return secretBytes;
    }
}
