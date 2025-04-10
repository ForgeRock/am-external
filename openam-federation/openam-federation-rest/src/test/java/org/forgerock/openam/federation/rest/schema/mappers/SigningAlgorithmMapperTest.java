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

package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBIntrospector;

import org.forgerock.openam.federation.rest.schema.shared.SigningAlgorithm;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.service.CurrentSourceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.sun.identity.saml2.jaxb.metadata.ExtensionsType;
import com.sun.identity.saml2.jaxb.metadata.algsupport.DigestMethodType;
import com.sun.identity.saml2.jaxb.metadata.algsupport.SigningMethodType;

/**
 * Unit test for {@link SigningAlgorithmMapper}.
 *
 * @since 7.0.0
 */
public final class SigningAlgorithmMapperTest {

    private SigningAlgorithmMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SigningAlgorithmMapper();
    }

    @Test
    void whenMapSigningMethodsAreConvertedToTheirCorrespondingAlgorithms() {
        // Given
        SigningMethodType signingMethod1 = new SigningMethodType();
        signingMethod1.setAlgorithm("http://www.w3.org/2000/09/xmldsig#rsa-sha1");
        SigningMethodType signingMethod2 = new SigningMethodType();
        signingMethod2.setAlgorithm("http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512");
        ExtensionsType extensions = new ExtensionsType();
        extensions.getAny().addAll(ImmutableList.of(signingMethod1, signingMethod2));

        // When
        List<SigningAlgorithm> signingAlgorithms = mapper.map(extensions, ROOT);

        // Then
        assertThat(signingAlgorithms).containsExactly(SigningAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_RSA_SHA_1,
                SigningAlgorithm.HTTP_WWW_W_3_ORG_2001_04_XMLDSIG_MORE_ECDSA_SHA_512);
    }

    @Test
    void whenMapDuplicateSigningMethodsAreDiscarded() {
        // Given
        SigningMethodType signingMethod1 = new SigningMethodType();
        signingMethod1.setAlgorithm("http://www.w3.org/2000/09/xmldsig#rsa-sha1");
        SigningMethodType signingMethod2 = new SigningMethodType();
        signingMethod2.setAlgorithm("http://www.w3.org/2000/09/xmldsig#rsa-sha1");
        ExtensionsType extensions = new ExtensionsType();
        extensions.getAny().addAll(ImmutableList.of(signingMethod1, signingMethod2));

        // When
        List<SigningAlgorithm> signingAlgorithms = mapper.map(extensions, ROOT);

        // Then
        assertThat(signingAlgorithms).containsExactly(SigningAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_RSA_SHA_1);
    }

    @Test
    void whenMapOtherExtensionTypesAreIgnored() {
        // Given
        SigningMethodType signingMethod = new SigningMethodType();
        signingMethod.setAlgorithm("http://www.w3.org/2000/09/xmldsig#rsa-sha1");
        DigestMethodType digestMethod = new DigestMethodType();
        digestMethod.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha1");
        ExtensionsType extensions = new ExtensionsType();
        extensions.getAny().addAll(ImmutableList.of(signingMethod, signingMethod));

        // When
        List<SigningAlgorithm> signingAlgorithms = mapper.map(extensions, ROOT);

        // Then
        assertThat(signingAlgorithms).containsExactly(SigningAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_RSA_SHA_1);
    }

    @Test
    void whenInverseSigningAlgorithmsAreConvertedToTheirCorrespondingSigningMethods() {
        // Given
        EnricherContext context = new CurrentSourceContext(ROOT, null, TypeToken.of(ExtensionsType.class));
        List<SigningAlgorithm> signingAlgorithms = ImmutableList
                .of(SigningAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_RSA_SHA_1,
                        SigningAlgorithm.HTTP_WWW_W_3_ORG_2001_04_XMLDSIG_MORE_ECDSA_SHA_512);

        // When
        ExtensionsType extensionObj = mapper.inverse(signingAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(2);
        assertThat(extensions).hasOnlyElementsOfType(SigningMethodType.class);
        assertThat(extensions).extracting("algorithm")
                .containsExactly("http://www.w3.org/2000/09/xmldsig#rsa-sha1",
                        "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512");
    }

    @Test
    void whenInverseExistingNonSigningMethodExtensionsAreRetained() {
        // Given
        DigestMethodType digestMethod = new DigestMethodType();
        digestMethod.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha1");
        ExtensionsType existingExtensions = new ExtensionsType();
        existingExtensions.getAny().addAll(ImmutableList.of(digestMethod));

        EnricherContext context = new CurrentSourceContext(ROOT,
                existingExtensions, TypeToken.of(ExtensionsType.class));
        List<SigningAlgorithm> signingAlgorithms = ImmutableList
                .of(SigningAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_RSA_SHA_1);

        // When
        ExtensionsType extensionObj = mapper.inverse(signingAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(2);
        assertThat(extensions).hasAtLeastOneElementOfType(SigningMethodType.class);
        assertThat(extensions).hasAtLeastOneElementOfType(DigestMethodType.class);
        assertThat(extensions).filteredOn(DigestMethodType.class::isInstance)
                .containsExactly(digestMethod);
    }

    @Test
    void whenInverseExistingMatchingSigningMethodsAreRetained() {
        // Given
        SigningMethodType signingMethod = new SigningMethodType();
        signingMethod.setAlgorithm("http://www.w3.org/2000/09/xmldsig#rsa-sha1");
        signingMethod.setMinKeySize(BigInteger.valueOf(1L));
        signingMethod.setMaxKeySize(BigInteger.valueOf(100L));
        ExtensionsType existingExtensions = new ExtensionsType();
        existingExtensions.getAny().addAll(ImmutableList.of(signingMethod));

        EnricherContext context = new CurrentSourceContext(ROOT,
                existingExtensions, TypeToken.of(ExtensionsType.class));
        List<SigningAlgorithm> signingAlgorithms = ImmutableList
                .of(SigningAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_RSA_SHA_1);

        // When
        ExtensionsType extensionObj = mapper.inverse(signingAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(1);
        assertThat(extensions).hasOnlyElementsOfType(SigningMethodType.class);
        assertThat(extensions).containsExactly(signingMethod);
        assertThat(extensions).extracting("algorithm")
                .containsExactly("http://www.w3.org/2000/09/xmldsig#rsa-sha1");
        assertThat(extensions).extracting("minKeySize")
                .containsExactly(BigInteger.valueOf(1L));
        assertThat(extensions).extracting("maxKeySize")
                .containsExactly(BigInteger.valueOf(100L));
    }

    @Test
    void whenInverseExistingNonSigningMethodExtensionsOrderIsMaintained() {
        // Given
        DigestMethodType digestMethod1 = new DigestMethodType();
        DigestMethodType digestMethod2 = new DigestMethodType();
        DigestMethodType digestMethod3 = new DigestMethodType();
        ExtensionsType existingExtensions = new ExtensionsType();
        existingExtensions.getAny().addAll(ImmutableList.of(digestMethod1, digestMethod2, digestMethod3));

        EnricherContext context = new CurrentSourceContext(ROOT,
                existingExtensions, TypeToken.of(ExtensionsType.class));
        List<SigningAlgorithm> signingAlgorithms = ImmutableList
                .of(SigningAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_RSA_SHA_1);

        // When
        ExtensionsType extensionObj = mapper.inverse(signingAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(4);
        assertThat(extensions).hasAtLeastOneElementOfType(SigningMethodType.class);
        assertThat(extensions).filteredOn(DigestMethodType.class::isInstance)
                .containsSequence(digestMethod1, digestMethod2, digestMethod3);
    }

    @Test
    void whenInverseSigningAlgorithmOrderIsMaintained() {
        // Given
        EnricherContext context = new CurrentSourceContext(ROOT, null, TypeToken.of(ExtensionsType.class));
        List<SigningAlgorithm> signingAlgorithms = ImmutableList
                .of(SigningAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_RSA_SHA_1,
                        SigningAlgorithm.HTTP_WWW_W_3_ORG_2001_04_XMLDSIG_MORE_ECDSA_SHA_512,
                        SigningAlgorithm.HTTP_WWW_W_3_ORG_2009_XMLDSIG_11_DSA_SHA_256);

        // When
        ExtensionsType extensionObj = mapper.inverse(signingAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(3);
        assertThat(extensions).hasOnlyElementsOfType(SigningMethodType.class);
        assertThat(extensions).extracting("algorithm")
                .containsSequence("http://www.w3.org/2000/09/xmldsig#rsa-sha1",
                        "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha512",
                        "http://www.w3.org/2009/xmldsig11#dsa-sha256");
    }

}
