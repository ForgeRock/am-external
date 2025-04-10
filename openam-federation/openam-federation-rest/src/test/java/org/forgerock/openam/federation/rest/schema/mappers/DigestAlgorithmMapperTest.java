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

import java.util.List;

import javax.xml.bind.JAXBIntrospector;

import org.forgerock.openam.federation.rest.schema.shared.DigestAlgorithm;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.service.CurrentSourceContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.sun.identity.saml2.jaxb.metadata.ExtensionsType;
import com.sun.identity.saml2.jaxb.metadata.algsupport.DigestMethodType;
import com.sun.identity.saml2.jaxb.metadata.algsupport.SigningMethodType;

/**
 * Unit test for {@link DigestAlgorithmMapper}.
 *
 * @since 7.0.0
 */
public final class DigestAlgorithmMapperTest {

    private static DigestAlgorithmMapper mapper;

    @BeforeAll
    static void setUp() {
        mapper = new DigestAlgorithmMapper();
    }

    @Test
    void whenMapDigestMethodsAreConvertedToTheirCorrespondingAlgorithms() {
        // Given
        DigestMethodType digestMethod1 = new DigestMethodType();
        digestMethod1.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha1");
        DigestMethodType digestMethod2 = new DigestMethodType();
        digestMethod2.setAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
        ExtensionsType extensions = new ExtensionsType();
        extensions.getAny().addAll(ImmutableList.of(digestMethod1, digestMethod2));

        // When
        List<DigestAlgorithm> digestAlgorithms = mapper.map(extensions, ROOT);

        // Then
        assertThat(digestAlgorithms).containsExactly(DigestAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_SHA_1,
                DigestAlgorithm.HTTP_WWW_W_3_ORG_2001_04_XMLENC_SHA_256);
    }

    @Test
    void whenMapDuplicateDigestMethodsAreDiscarded() {
        // Given
        DigestMethodType digestMethod1 = new DigestMethodType();
        digestMethod1.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha1");
        DigestMethodType digestMethod2 = new DigestMethodType();
        digestMethod2.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha1");
        ExtensionsType extensions = new ExtensionsType();
        extensions.getAny().addAll(ImmutableList.of(digestMethod1, digestMethod2));

        // When
        List<DigestAlgorithm> digestAlgorithms = mapper.map(extensions, ROOT);

        // Then
        assertThat(digestAlgorithms).hasSize(1);
        assertThat(digestAlgorithms).containsExactly(DigestAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_SHA_1);
    }

    @Test
    void whenMapOtherExtensionTypesAreIgnored() {
        // Given
        DigestMethodType digestMethod = new DigestMethodType();
        digestMethod.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha1");
        SigningMethodType signingMethod = new SigningMethodType();
        signingMethod.setAlgorithm("http://www.w3.org/2000/09/xmldsig#hmac-sha1");
        ExtensionsType extensions = new ExtensionsType();
        extensions.getAny().addAll(ImmutableList.of(digestMethod, signingMethod));

        // When
        List<DigestAlgorithm> digestAlgorithms = mapper.map(extensions, ROOT);

        // Then
        assertThat(digestAlgorithms).containsExactly(DigestAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_SHA_1);
    }

    @Test
    void whenInverseDigestAlgorithmsAreConvertedToTheirCorrespondingDigestMethods() {
        // Given
        EnricherContext context = new CurrentSourceContext(ROOT, null, TypeToken.of(ExtensionsType.class));
        List<DigestAlgorithm> digestAlgorithms = ImmutableList
                .of(DigestAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_SHA_1,
                        DigestAlgorithm.HTTP_WWW_W_3_ORG_2001_04_XMLENC_SHA_256);

        // When
        ExtensionsType extensionObj = mapper.inverse(digestAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(2);
        assertThat(extensions).hasOnlyElementsOfType(DigestMethodType.class);
        assertThat(extensions).extracting("algorithm")
                .containsExactly("http://www.w3.org/2000/09/xmldsig#sha1", "http://www.w3.org/2001/04/xmlenc#sha256");
    }

    @Test
    void whenInverseExistingNonDigestMethodExtensionsAreRetained() {
        // Given
        SigningMethodType signingMethod = new SigningMethodType();
        signingMethod.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha1");
        ExtensionsType existingExtensions = new ExtensionsType();
        existingExtensions.getAny().addAll(ImmutableList.of(signingMethod));

        EnricherContext context = new CurrentSourceContext(ROOT,
                existingExtensions, TypeToken.of(ExtensionsType.class));
        List<DigestAlgorithm> digestAlgorithms = ImmutableList
                .of(DigestAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_SHA_1);

        // When
        ExtensionsType extensionObj = mapper.inverse(digestAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(2);
        assertThat(extensions).hasAtLeastOneElementOfType(DigestMethodType.class);
        assertThat(extensions).hasAtLeastOneElementOfType(SigningMethodType.class);
        assertThat(extensions).filteredOn(SigningMethodType.class::isInstance)
                .containsExactly(signingMethod);
    }

    @Test
    void whenInverseExistingDigestMethodsAreReplaced() {
        // Given
        DigestMethodType digestMethod = new DigestMethodType();
        digestMethod.setAlgorithm("http://www.w3.org/2001/04/xmldsig-more#sha384");
        ExtensionsType existingExtensions = new ExtensionsType();
        existingExtensions.getAny().addAll(ImmutableList.of(digestMethod));

        EnricherContext context = new CurrentSourceContext(ROOT,
                existingExtensions, TypeToken.of(ExtensionsType.class));
        List<DigestAlgorithm> digestAlgorithms = ImmutableList
                .of(DigestAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_SHA_1);

        // When
        ExtensionsType extensionObj = mapper.inverse(digestAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(1);
        assertThat(extensions).hasOnlyElementsOfType(DigestMethodType.class);
        assertThat(extensions).extracting("algorithm")
                .containsExactly("http://www.w3.org/2000/09/xmldsig#sha1");
    }

    @Test
    void whenInverseExistingNonDigestMethodExtensionsOrderIsMaintained() {
        // Given
        SigningMethodType signingMethod1 = new SigningMethodType();
        SigningMethodType signingMethod2 = new SigningMethodType();
        SigningMethodType signingMethod3 = new SigningMethodType();
        ExtensionsType existingExtensions = new ExtensionsType();
        existingExtensions.getAny().addAll(ImmutableList.of(signingMethod1, signingMethod2, signingMethod3));

        EnricherContext context = new CurrentSourceContext(ROOT,
                existingExtensions, TypeToken.of(ExtensionsType.class));
        List<DigestAlgorithm> digestAlgorithms = ImmutableList
                .of(DigestAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_SHA_1);

        // When
        ExtensionsType extensionObj = mapper.inverse(digestAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(4);
        assertThat(extensions).hasAtLeastOneElementOfType(DigestMethodType.class);
        assertThat(extensions).filteredOn(SigningMethodType.class::isInstance)
                .containsSequence(signingMethod1, signingMethod2, signingMethod3);
    }

    @Test
    void whenInverseDigestAlgorithmOrderIsMaintained() {
        // Given
        EnricherContext context = new CurrentSourceContext(ROOT, null, TypeToken.of(ExtensionsType.class));
        List<DigestAlgorithm> digestAlgorithms = ImmutableList
                .of(DigestAlgorithm.HTTP_WWW_W_3_ORG_2000_09_XMLDSIG_SHA_1,
                        DigestAlgorithm.HTTP_WWW_W_3_ORG_2001_04_XMLENC_SHA_256,
                        DigestAlgorithm.HTTP_WWW_W_3_ORG_2001_04_XMLDSIG_MORE_SHA_384);

        // When
        ExtensionsType extensionObj = mapper.inverse(digestAlgorithms, context);

        // Then
        List<Object> extensions = extensionObj.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .collect(toList());

        assertThat(extensions).isNotNull();
        assertThat(extensions).hasSize(3);
        assertThat(extensions).hasOnlyElementsOfType(DigestMethodType.class);
        assertThat(extensions).extracting("algorithm")
                .containsSequence("http://www.w3.org/2000/09/xmldsig#sha1",
                        "http://www.w3.org/2001/04/xmlenc#sha256", "http://www.w3.org/2001/04/xmldsig-more#sha384");
    }

}
