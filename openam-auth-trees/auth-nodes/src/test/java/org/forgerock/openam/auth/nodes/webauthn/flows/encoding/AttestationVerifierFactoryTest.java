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
 * Copyright 2024-2025 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.webauthn.flows.encoding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.forgerock.openam.auth.nodes.webauthn.WebAuthnRegistrationNode;
import org.forgerock.openam.auth.nodes.webauthn.flows.FlowUtilities;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AndroidKeyVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AndroidSafetyNetVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.AttestationVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.FidoU2fVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.NoneVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.PackedVerifier;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmManufacturer;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmManufacturerId;
import org.forgerock.openam.auth.nodes.webauthn.flows.formats.tpm.TpmVerifier;
import org.forgerock.openam.auth.nodes.webauthn.metadata.MetadataServiceFactory;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorUtilities;
import org.forgerock.openam.auth.nodes.webauthn.trustanchor.TrustAnchorValidator;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.secrets.cache.SecretReferenceCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class AttestationVerifierFactoryTest {
    @Mock
    TrustAnchorValidator.Factory factory;
    @Mock
    TrustAnchorUtilities trustUtilities;
    Realm realm;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    SecretReferenceCache secrets;
    @Mock
    FlowUtilities flowUtilities;
    @Mock
    MetadataServiceFactory metadataServiceFactory;
    @Mock
    WebAuthnRegistrationNode.Config config;

    private AttestationVerifierFactory attestationVerifierFactory;

    public static Stream<Arguments> formatVerifier() {
        return Stream.of(
                arguments("none", NoneVerifier.class),
                arguments("fido-u2f", FidoU2fVerifier.class),
                arguments("packed", PackedVerifier.class),
                arguments("tpm", TpmVerifier.class),
                arguments("android-safetynet", AndroidSafetyNetVerifier.class),
                arguments("android-key", AndroidKeyVerifier.class)
        );
    }

    @BeforeEach
    void theSetUp() {
        Set<TpmManufacturer> tpmManufacturers = new HashSet<>(Arrays.asList(TpmManufacturerId.values()));
        attestationVerifierFactory = new AttestationVerifierFactory(trustUtilities, factory, flowUtilities,
                tpmManufacturers, secrets, metadataServiceFactory);
    }

    @ParameterizedTest
    @MethodSource("formatVerifier")
    public void testCorrectVerifierReturnedForAttestationFormat(String format,
            Class<AttestationVerifier> verifier) throws Exception {
        //when
        AttestationVerifier result = attestationVerifierFactory.create(realm, config, format);

        //then
        assertThat(result).isOfAnyClassIn(verifier);
    }
}
