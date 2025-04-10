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
 * Copyright 2019-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */
package org.forgerock.openam.saml2.crypto.signing;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.xml.security.algorithms.MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA1;
import static org.apache.xml.security.algorithms.MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256;
import static org.apache.xml.security.algorithms.MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA384;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_DSA_SHA256;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA256;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA384;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA512;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA384;
import static org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.forgerock.openam.saml2.Saml2EntityRole.SP;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.withSettings;

import java.math.BigInteger;
import java.security.Key;
import java.security.interfaces.DSAKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.security.spec.ECField;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.util.List;

import org.forgerock.openam.federation.util.XmlSecurity;
import org.forgerock.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.ExtensionsType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.jaxb.metadata.algsupport.DigestMethodType;
import com.sun.identity.saml2.jaxb.metadata.algsupport.SigningMethodType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class AlgorithmSelectorTest {

    private AlgorithmSelector selector;
    private EntityDescriptorElement entityDescriptor;
    @Mock
    private Key key;

    @BeforeAll
    static void setupClass() {
        XmlSecurity.init();
    }

    @BeforeEach
    void setup() {
        selector = new AlgorithmSelector();
    }

    public static Object[][] algorithms() {
        return new Object[][]{
                {rsaKey(2048), ALGO_ID_SIGNATURE_RSA_SHA1, true},
                {rsaKey(2048), ALGO_ID_SIGNATURE_RSA_SHA256, true},
                {rsaKey(2048), ALGO_ID_SIGNATURE_RSA_SHA384, true},
                {rsaKey(2048), ALGO_ID_SIGNATURE_RSA_SHA512, true},
                {dsaKey(1024), ALGO_ID_SIGNATURE_DSA_SHA256, true},
                {ecKey(128), ALGO_ID_SIGNATURE_ECDSA_SHA256, true},
                {ecKey(128), ALGO_ID_SIGNATURE_ECDSA_SHA384, true},
                {ecKey(128), ALGO_ID_SIGNATURE_ECDSA_SHA512, true},
                //Invalid algorithms
                {rsaKey(2048), ALGO_ID_SIGNATURE_DSA_SHA256, false},
                {rsaKey(2048), ALGO_ID_SIGNATURE_ECDSA_SHA256, false},
                {rsaKey(2048), ALGO_ID_SIGNATURE_ECDSA_SHA384, false},
                {rsaKey(2048), ALGO_ID_SIGNATURE_ECDSA_SHA512, false},
                {dsaKey(2048), ALGO_ID_SIGNATURE_RSA_SHA1, false},
                {dsaKey(1024), ALGO_ID_SIGNATURE_RSA_SHA256, false},
                {dsaKey(1024), ALGO_ID_SIGNATURE_RSA_SHA384, false},
                {dsaKey(1024), ALGO_ID_SIGNATURE_RSA_SHA512, false},
                {dsaKey(1024), ALGO_ID_SIGNATURE_ECDSA_SHA256, false},
                {dsaKey(1024), ALGO_ID_SIGNATURE_ECDSA_SHA384, false},
                {dsaKey(1024), ALGO_ID_SIGNATURE_ECDSA_SHA512, false},
                {ecKey(128), ALGO_ID_SIGNATURE_RSA_SHA1, false},
                {ecKey(128), ALGO_ID_SIGNATURE_RSA_SHA256, false},
                {ecKey(128), ALGO_ID_SIGNATURE_RSA_SHA384, false},
                {ecKey(128), ALGO_ID_SIGNATURE_RSA_SHA512, false},
                {ecKey(128), ALGO_ID_SIGNATURE_DSA_SHA256, false},
        };
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    public void shouldValidateKeyAlgorithm(Key key, String signingMethod, boolean expectedOutcome) {
        // When
        boolean result = selector.checkKeyMatchesSigningAlgorithm(key, signingMethod(signingMethod, null, null));

        assertThat(result).isEqualTo(expectedOutcome);
    }

    public static Object[][] keySizes() {
        return new Object[][]{
                {rsaKey(1), null, null, true},
                {rsaKey(1024), 1023, null, true},
                {rsaKey(1024), null, 1025, true},
                {rsaKey(1024), 1023, 1025, true},
                {rsaKey(1024), 1024, 1024, true},
                {dsaKey(1), null, null, true},
                {dsaKey(1024), 1023, null, true},
                {dsaKey(1024), null, 1025, true},
                {dsaKey(1024), 1023, 1025, true},
                {dsaKey(1024), 1024, 1024, true},
                {ecKey(1), null, null, true},
                {ecKey(1024), 1023, null, true},
                {ecKey(1024), null, 1025, true},
                {ecKey(1024), 1023, 1025, true},
                {ecKey(1024), 1024, 1024, true},
                //Invalid key sizes
                {rsaKey(-1), null, null, false},
                {rsaKey(1024), 2048, null, false},
                {rsaKey(1024), null, 1023, false},
                {rsaKey(1024), 1025, 1023, false},
                {rsaKey(1024), 1024, 1023, false},
                {rsaKey(1024), 1025, 1024, false},
                {dsaKey(-1), null, null, false},
                {dsaKey(1024), 2048, null, false},
                {dsaKey(1024), null, 1023, false},
                {dsaKey(1024), 1025, 1023, false},
                {dsaKey(1024), 1024, 1023, false},
                {dsaKey(1024), 1025, 1024, false},
                {ecKey(-1), null, null, false},
                {ecKey(1024), 2048, null, false},
                {ecKey(1024), null, 1023, false},
                {ecKey(1024), 1025, 1023, false},
                {ecKey(1024), 1024, 1023, false},
                {ecKey(1024), 1025, 1024, false},
        };
    }

    @ParameterizedTest
    @MethodSource("keySizes")
    public void shouldValidateKeySize(Key key, Integer minSize, Integer maxSize, boolean expectedOutcome) {
        // When
        String signingMethod;
        switch (key.getAlgorithm()) {
            case "RSA":
                signingMethod = ALGO_ID_SIGNATURE_RSA_SHA256;
                break;
            case "DSA":
                signingMethod = ALGO_ID_SIGNATURE_DSA_SHA256;
                break;
            case "EC":
                signingMethod = ALGO_ID_SIGNATURE_ECDSA_SHA256;
                break;
            default:
                throw new IllegalArgumentException("Unrecognised key algorithm");
        }
        boolean result = selector.checkKeyMatchesSigningAlgorithm(key, signingMethod(signingMethod, minSize, maxSize));

        assertThat(result).isEqualTo(expectedOutcome);
    }

    @Test
    void shouldPickDefaultRsaSigningAlgorithmForRsaKey() {
        // Given
        given(key.getAlgorithm()).willReturn("RSA");

        // When
        String signingAlg = selector.getDefaultXmlSigningAlgorithmForKey(key);

        // Then
        assertThat(signingAlg).isEqualTo(ALGO_ID_SIGNATURE_RSA_SHA256);
    }

    @Test
    void shouldPickDefaultDsaSigningAlgorithmForDsaKey() {
        // Given
        given(key.getAlgorithm()).willReturn("DSA");

        // When
        String signingAlg = selector.getDefaultXmlSigningAlgorithmForKey(key);

        // Then
        assertThat(signingAlg).isEqualTo(ALGO_ID_SIGNATURE_DSA_SHA256);
    }

    @Test
    void shouldPickDefaultEcdsaSigningAlgorithmForEcKey() {
        // Given
        given(key.getAlgorithm()).willReturn("EC");

        // When
        String signingAlg = selector.getDefaultXmlSigningAlgorithmForKey(key);

        // Then
        assertThat(signingAlg).isEqualTo(ALGO_ID_SIGNATURE_ECDSA_SHA512);
    }

    @Test
    void shouldPickRoleDescriptorAlgorithmsFirst() throws Exception {
        // Given
        setupEntityDescriptor(asList(signingMethod(ALGO_ID_SIGNATURE_RSA_SHA384, null, null),
                digestMethod(ALGO_ID_DIGEST_SHA384)),
                asList(signingMethod(ALGO_ID_SIGNATURE_RSA, null, null), digestMethod(ALGO_ID_DIGEST_SHA1)));

        // When
        Pair<String, String> algorithms = selector.selectSigningAlgorithms(rsaKey(2048), entityDescriptor, SP,
                key -> null);

        // Then
        assertThat(algorithms.getFirst()).isEqualTo(ALGO_ID_SIGNATURE_RSA_SHA384);
        assertThat(algorithms.getSecond()).isEqualTo(ALGO_ID_DIGEST_SHA384);
    }

    @Test
    void shouldFallbackToProviderAlgorithms() throws Exception {
        // Given
        setupEntityDescriptor(emptyList(),
                asList(signingMethod(ALGO_ID_SIGNATURE_RSA_SHA384, null, null), digestMethod(ALGO_ID_DIGEST_SHA384)));

        // When
        Pair<String, String> algorithms = selector.selectSigningAlgorithms(rsaKey(2048), entityDescriptor, SP,
                key -> null);

        // Then
        assertThat(algorithms.getFirst()).isEqualTo(ALGO_ID_SIGNATURE_RSA_SHA384);
        assertThat(algorithms.getSecond()).isEqualTo(ALGO_ID_DIGEST_SHA384);
    }

    @Test
    void shouldUseDefaultsWhenThereAreNoExtensions() throws Exception {
        // Given
        setupEntityDescriptor(emptyList(), emptyList());

        // When
        Pair<String, String> algorithms = selector.selectSigningAlgorithms(rsaKey(2048), entityDescriptor, SP,
                key -> "badger");

        // Then
        assertThat(algorithms.getFirst()).isEqualTo("badger");
        assertThat(algorithms.getSecond()).isEqualTo(ALGO_ID_DIGEST_SHA256);
    }

    @Test
    void shouldThrowExceptionWhenKeyDoesNotMatchRoleLevelAlgorithms() throws Exception {

        assertThatThrownBy(() -> {
            // Given
            setupEntityDescriptor(asList(signingMethod(ALGO_ID_SIGNATURE_DSA_SHA256, null, null),
                            signingMethod(ALGO_ID_SIGNATURE_ECDSA_SHA256, null, null)),
                    emptyList());

            // When
            selector.selectSigningAlgorithms(rsaKey(2048), entityDescriptor, SP, key -> null);
        }).isInstanceOf(SAML2Exception.class);
    }

    private static Key rsaKey(int size) {
        RSAKey rsaKey = mock(RSAKey.class, withSettings().extraInterfaces(Key.class));
        given(((Key) rsaKey).getAlgorithm()).willReturn("RSA");
        BigInteger modulus = mock(BigInteger.class);
        given(rsaKey.getModulus()).willReturn(modulus);
        given(modulus.bitLength()).willReturn(size);
        return (Key) rsaKey;
    }

    private static Key dsaKey(int size) {
        DSAKey dsaKey = mock(DSAKey.class, withSettings().extraInterfaces(Key.class));
        given(((Key) dsaKey).getAlgorithm()).willReturn("DSA");
        BigInteger p = mock(BigInteger.class);
        DSAParams dsaParams = mock(DSAParams.class);
        given(dsaKey.getParams()).willReturn(dsaParams);
        given(dsaParams.getP()).willReturn(p);
        given(p.bitLength()).willReturn(size);
        return (Key) dsaKey;
    }

    private static Key ecKey(int size) {
        ECKey ecKey = mock(ECKey.class, withSettings().extraInterfaces(Key.class));
        given(((Key) ecKey).getAlgorithm()).willReturn("EC");
        ECParameterSpec ecParams = mock(ECParameterSpec.class);
        EllipticCurve curve = mock(EllipticCurve.class);
        ECField field = mock(ECField.class);
        given(ecKey.getParams()).willReturn(ecParams);
        given(ecParams.getCurve()).willReturn(curve);
        given(curve.getField()).willReturn(field);
        given(field.getFieldSize()).willReturn(size);
        return (Key) ecKey;
    }

    private SigningMethodType signingMethod(String algorithm, Integer minSize, Integer maxSize) {
        SigningMethodType signingMethodType = new SigningMethodType();
        signingMethodType.setAlgorithm(algorithm);
        if (minSize != null) {
            signingMethodType.setMinKeySize(BigInteger.valueOf(minSize));
        }
        if (maxSize != null) {
            signingMethodType.setMaxKeySize(BigInteger.valueOf(maxSize));
        }
        return signingMethodType;
    }

    private DigestMethodType digestMethod(String algorithm) {
        DigestMethodType digestMethodType = new DigestMethodType();
        digestMethodType.setAlgorithm(algorithm);
        return digestMethodType;
    }

    private void setupEntityDescriptor(List<Object> roleAlgorithms, List<Object> providerAlgorithms) {
        entityDescriptor = new EntityDescriptorElement(new EntityDescriptorType());
        SPSSODescriptorType sp = new SPSSODescriptorType();
        ExtensionsType extensions = new ExtensionsType();
        sp.setExtensions(extensions);
        extensions.getAny().addAll(roleAlgorithms);

        extensions = new ExtensionsType();
        entityDescriptor.getValue().setExtensions(extensions);
        extensions.getAny().addAll(providerAlgorithms);

        entityDescriptor.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor().add(sp);
    }
}
