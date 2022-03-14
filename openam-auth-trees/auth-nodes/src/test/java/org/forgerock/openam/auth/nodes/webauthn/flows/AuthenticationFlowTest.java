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
 * Copyright 2021 ForgeRock AS.
 */

package org.forgerock.openam.auth.nodes.webauthn.flows;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.BitSet;
import java.util.Set;

import org.forgerock.json.jose.jwk.EcJWK;
import org.forgerock.json.jose.utils.BigIntegerUtils;
import org.forgerock.json.jose.utils.DerUtils;
import org.forgerock.openam.auth.nodes.webauthn.UserVerificationRequirement;
import org.forgerock.openam.auth.nodes.webauthn.data.AttestationFlags;
import org.forgerock.openam.auth.nodes.webauthn.data.AuthData;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.rest.devices.UserDeviceSettingsDao;
import org.forgerock.openam.core.rest.devices.webauthn.UserWebAuthnDeviceProfileManager;
import org.forgerock.openam.core.rest.devices.webauthn.WebAuthnDeviceSettings;
import org.forgerock.openam.shared.security.crypto.Fingerprints;
import org.forgerock.util.encode.Base64url;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.primitives.Bytes;
import com.sun.identity.shared.encode.Base64;

public class AuthenticationFlowTest {

    @Mock
    private FlowUtilities flowUtilities;

    @Mock
    private Realm realm;

    @Mock
    private UserDeviceSettingsDao<WebAuthnDeviceSettings> dao;

    private AuthenticationFlow flow;

    private KeyPair ecdsaKeyPair;

    @BeforeClass
    public void createKeys() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
        ecdsaKeyPair = keyPairGenerator.generateKeyPair();
    }

    @BeforeMethod
    public void setup() throws Exception {
        openMocks(this).close();

        flow = new AuthenticationFlow(flowUtilities);
    }

    @DataProvider
    public Object[][] invalidEcdsaSignatures() {
        BigInteger order = ((ECPublicKey) ecdsaKeyPair.getPublic()).getParams().getOrder();
        byte[] tooBig = BigIntegerUtils.toBytesUnsigned(order);
        byte[] tooSmall = new byte[32];
        byte[] justRight = BigIntegerUtils.toBytesUnsigned(order.subtract(BigInteger.ONE));
        return new Object[][] {
                { Bytes.concat(tooSmall, tooSmall) },
                { Bytes.concat(tooSmall, justRight) },
                { Bytes.concat(justRight, tooSmall) },
                { Bytes.concat(tooBig, tooBig) },
                { Bytes.concat(tooBig, justRight) },
                { Bytes.concat(justRight, tooBig) },
        };
    }

    @Test(dataProvider = "invalidEcdsaSignatures")
    public void shouldRejectInvalidEcdsaSignatureValues(byte[] invalidSignature) {
        // Given
        byte[] challengeBytes = "testChallenge".getBytes(UTF_8);
        String rpId = "https://example.com:80";
        String clientData = "{\"type\":\"webauthn.get\",\"challenge\":\"" + Base64url.encode(challengeBytes) + "\""
                + ",\"origin\":\"" + rpId + "\"}";
        Set<String> origins = Set.of(rpId);
        UserVerificationRequirement requirement = UserVerificationRequirement.PREFERRED;
        BitSet flags = new BitSet();
        flags.set(0);
        AuthData authData = new AuthData(Base64.decode(Fingerprints.generate(rpId)), new AttestationFlags(flags), 0,
                null, new byte[20]);
        UserWebAuthnDeviceProfileManager deviceProfileManager = new UserWebAuthnDeviceProfileManager(dao);
        WebAuthnDeviceSettings deviceSettings = deviceProfileManager.createDeviceProfile("test",
                EcJWK.builder((ECPublicKey) ecdsaKeyPair.getPublic()).build(), "SHA256WithECDSA", "test");

        given(flowUtilities.isOriginValid(realm, origins, rpId)).willReturn(true);
        given(flowUtilities.getPublicKeyFromJWK(any())).willReturn(ecdsaKeyPair.getPublic());
        byte[] ecdsaSig = DerUtils.encodeEcdsaSignature(invalidSignature);

        // When
        boolean valid = flow.accept(realm, clientData, authData, ecdsaSig, challengeBytes, rpId, deviceSettings,
                origins, requirement);

        // Then
        assertThat(valid).isFalse();
    }
}