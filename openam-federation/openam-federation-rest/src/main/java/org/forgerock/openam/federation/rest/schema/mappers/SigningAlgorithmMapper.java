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

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBIntrospector;

import org.forgerock.openam.federation.rest.schema.shared.SigningAlgorithm;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;
import org.forgerock.openam.objectenricher.service.CurrentSourceContext;

import com.sun.identity.saml2.jaxb.metadata.ExtensionsType;
import com.sun.identity.saml2.jaxb.metadata.ObjectFactory;
import com.sun.identity.saml2.jaxb.metadata.algsupport.SigningMethodType;

/**
 * {@link ValueMapper} implementation that retrieves and updates the signing methods from the extensions list. The order
 * of the signing methods is maintained during retrieval. During update the signing methods order will reflect that of
 * the updated signing algorithms, any matching existing signing method will be retained, and any signing method not in
 * the updated signing algorithm list will be discarded. All other extensions and their order will be retained during
 * update.
 *
 * @since 7.0.0
 */
public final class SigningAlgorithmMapper extends ValueMapper<ExtensionsType, List<SigningAlgorithm>> {

    private final ObjectFactory metaDataObjectFactory;
    private final com.sun.identity.saml2.jaxb.metadata.algsupport.ObjectFactory algSupportObjectFactory;

    /**
     * Create a new signing algorithm mapper.
     */
    public SigningAlgorithmMapper() {
        metaDataObjectFactory = new ObjectFactory();
        algSupportObjectFactory = new com.sun.identity.saml2.jaxb.metadata.algsupport.ObjectFactory();
    }

    @Override
    public List<SigningAlgorithm> map(ExtensionsType extensions, EnricherContext context) {
        return extensions.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .filter(SigningMethodType.class::isInstance)
                .map(SigningMethodType.class::cast)
                .map(SigningMethodType::getAlgorithm)
                .map(SigningAlgorithm::fromValue)
                .distinct()
                .collect(toList());
    }

    @Override
    public ExtensionsType inverse(List<SigningAlgorithm> signingAlgorithms, EnricherContext context) {
        List<Object> existingExtensions = context.as(CurrentSourceContext.class)
                .getSource(this)
                .map(ExtensionsType::getAny)
                .orElseGet(Collections::emptyList);

        Map<Boolean, List<Object>> extensionsPartitionedBySigningMethod = existingExtensions.stream()
                .collect(partitioningBy(this::isSigningMethod, toList()));

        Stream<Object> existingSigningMethods = extensionsPartitionedBySigningMethod.get(true).stream();
        Stream<Object> allOtherExtensions = extensionsPartitionedBySigningMethod.get(false).stream();

        Map<SigningAlgorithm, SigningMethodType> signingMethodsIndexedByAlgorithm = existingSigningMethods
                .map(JAXBIntrospector::getValue)
                .map(SigningMethodType.class::cast)
                .collect(toMap(method -> SigningAlgorithm.fromValue(method.getAlgorithm()), Function.identity()));

        Stream<JAXBElement<SigningMethodType>> updatedSigningMethods = signingAlgorithms.stream()
                .map(algorithm -> getOrCreateSigningMethod(algorithm, signingMethodsIndexedByAlgorithm));

        ExtensionsType updatedExtensions = metaDataObjectFactory.createExtensionsType();
        updatedExtensions.getAny()
                .addAll(Stream.concat(updatedSigningMethods, allOtherExtensions)
                        .collect(toList()));

        return updatedExtensions;
    }

    private JAXBElement<SigningMethodType> getOrCreateSigningMethod(SigningAlgorithm algorithm,
            Map<SigningAlgorithm, SigningMethodType> indexedSigningMethods) {
        SigningMethodType signingMethod = algSupportObjectFactory.createSigningMethodType();
        signingMethod.setAlgorithm(algorithm.value());
        return algSupportObjectFactory.createSigningMethod(
                indexedSigningMethods.getOrDefault(algorithm, signingMethod));
    }

    private boolean isSigningMethod(Object extension) {
        return JAXBIntrospector.getValue(extension) instanceof SigningMethodType;
    }

}
