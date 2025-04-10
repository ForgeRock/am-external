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
 * Copyright 2021-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.BitSet;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.utils.BigIntegerUtils;
import org.forgerock.json.jose.utils.DerUtils;
import org.forgerock.openam.auth.nodes.webauthn.UserVerificationRequirement;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationFlags;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestedCredentialData;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.UserDeviceSettingsDao;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.shared.security.crypto.Fingerprints;
import org.forgerock.util.encode.Base64url;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Bytes;
import com.sun.identity.shared.encode.Base64;

@ExtendWith(MockitoExtension.class)
public class AuthenticationFlowTest {

    private static KeyPair ecdsaKeyPair;
    @Mock
    private FlowUtilities flowUtilities;
    @Mock
    private Realm realm;
    @Mock
    private Logger logger;
    @Mock
    private UserDeviceSettingsDao<WebAuthnDeviceSettings> dao;
    private AuthenticationFlow flow;
    private byte[] challengeBytes;
    private String rpId, clientData;
    private Set<String> origins;
    private UserVerificationRequirement requirement;
    private BitSet flags;
    private AuthData authData;
    private UserWebAuthnDeviceProfileManager deviceProfileManager;
    private WebAuthnDeviceSettings deviceSettings;

    @BeforeAll
    static void createKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        ecdsaKeyPair = keyPairGenerator.generateKeyPair();
    }

    public static Stream<Arguments> invalidEcdsaSignatures() {
        BigInteger order = ((ECPublicKey) ecdsaKeyPair.getPublic()).getParams().getOrder();
        byte[] tooBig = BigIntegerUtils.toBytesUnsigned(order);
        byte[] tooSmall = new byte[32];
        byte[] justRight = BigIntegerUtils.toBytesUnsigned(order.subtract(BigInteger.ONE));

        return Stream.of(
                Arguments.of(Bytes.concat(tooSmall, tooSmall)),
                Arguments.of(Bytes.concat(tooSmall, justRight)),
                Arguments.of(Bytes.concat(justRight, tooSmall)),
                Arguments.of(Bytes.concat(tooBig, tooBig)),
                Arguments.of(Bytes.concat(tooBig, justRight)),
                Arguments.of(Bytes.concat(justRight, tooBig))
        );
    }

    @BeforeEach
    void setup() throws Exception {
        try (MockedStatic<LoggerFactory> mockMessageFactory = org.mockito.Mockito.mockStatic(LoggerFactory.class)) {
            mockMessageFactory.when(() -> LoggerFactory.getLogger(AuthenticationFlow.class)).thenReturn(logger);

            flow = new AuthenticationFlow(flowUtilities);
        }

        challengeBytes = "testChallenge".getBytes(UTF_8);
        rpId = "https://example.com:80";
        clientData = "{\"type\":\"webauthn.get\",\"challenge\":\"" + Base64url.encode(challengeBytes)
                + "\",\"origin\":\"" + rpId + "\"}";
        origins = Set.of(rpId);
        requirement = UserVerificationRequirement.PREFERRED;
        flags = new BitSet();
        flags.set(0);
        authData = new AuthData(Base64.decode(Fingerprints.generate(rpId)), new AttestationFlags(flags), 0,
                null, new byte[20]);
        deviceProfileManager = new UserWebAuthnDeviceProfileManager(dao);
        deviceSettings = deviceProfileManager.createDeviceProfile("test",
                EcJWK.builder((ECPublicKey) ecdsaKeyPair.getPublic()).build(), "SHA256WithECDSA", "test", 1);

        given(flowUtilities.isOriginValid(realm, origins, rpId)).willReturn(true);

    }

    @ParameterizedTest
    @MethodSource("invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatureValues(byte[] invalidSignature) {
        // Given
        given(flowUtilities.getPublicKeyFromJWK(any())).willReturn(ecdsaKeyPair.getPublic());

        // When
        boolean valid = flow.accept(realm, clientData, authData, DerUtils.encodeEcdsaSignature(invalidSignature),
                challengeBytes, rpId, deviceSettings, origins, requirement);

        // Then
        assertThat(valid).isFalse();
        verify(logger).error(eq("error in verifying webauthn signature"), any(Throwable.class));
    }


    @Test
    void shouldRejectWhenAttestedDataIncludedFlagIsSet() {
        // Given
        BitSet flags = new BitSet();
        flags.set(0);
        flags.set(6);
        AuthData authData = new AuthData(Base64.decode(Fingerprints.generate(rpId)), new AttestationFlags(flags), 0,
                null, new byte[20]);

        // When
        boolean valid = flow.accept(realm, clientData, authData, DerUtils.encodeEcdsaSignature(new byte[64]),
                challengeBytes, rpId, deviceSettings, origins, requirement);

        // Then
        assertThat(valid).isFalse();
        verify(logger).error("attested data bit was set");
    }

    @Test
    void shouldRejectWhenAttestedCredentialDataIsSet() {
        // Given
        AuthData authData = new AuthData(Base64.decode(Fingerprints.generate(rpId)), new AttestationFlags(flags), 0,
                new AttestedCredentialData(null, 0, null, null, null), new byte[20]);

        // When
        boolean valid = flow.accept(realm, clientData, authData, DerUtils.encodeEcdsaSignature(new byte[64]),
                challengeBytes, rpId, deviceSettings, origins, requirement);

        // Then
        assertThat(valid).isFalse();
        verify(logger).error("attested credential data was present");
    }
}
