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
 * Copyright 2019-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.stream.Stream;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBIntrospector;

import org.forgerock.openam.federation.rest.schema.shared.DigestAlgorithm;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;
import org.forgerock.openam.objectenricher.service.CurrentSourceContext;

import com.sun.identity.saml2.jaxb.metadata.ExtensionsType;
import com.sun.identity.saml2.jaxb.metadata.ObjectFactory;
import com.sun.identity.saml2.jaxb.metadata.algsupport.DigestMethodType;

/**
 * {@link ValueMapper} implementation that retrieves and updates the digest methods from the extensions list. The order
 * of the digest methods is maintained during retrieval. During update the digest methods order will reflect that of the
 * updated digest algorithms and any digest method not in the updated digest algorithm list will be discarded. All other
 * extensions and their order will be retained during update.
 *
 * @since 7.0.0
 */
public final class DigestAlgorithmMapper extends ValueMapper<ExtensionsType, List<DigestAlgorithm>> {

    private final ObjectFactory metaDataObjectFactory;
    private final com.sun.identity.saml2.jaxb.metadata.algsupport.ObjectFactory algSupportObjectFactory;

    /**
     * Create a new digest algorithm mapper.
     */
    public DigestAlgorithmMapper() {
        metaDataObjectFactory = new ObjectFactory();
        algSupportObjectFactory = new com.sun.identity.saml2.jaxb.metadata.algsupport.ObjectFactory();
    }

    @Override
    public List<DigestAlgorithm> map(ExtensionsType extensions, EnricherContext context) {
        return extensions.getAny()
                .stream()
                .map(JAXBIntrospector::getValue)
                .filter(DigestMethodType.class::isInstance)
                .map(DigestMethodType.class::cast)
                .map(DigestMethodType::getAlgorithm)
                .map(DigestAlgorithm::fromValue)
                .distinct()
                .collect(toList());
    }

    @Override
    public ExtensionsType inverse(List<DigestAlgorithm> digestAlgorithms, EnricherContext context) {
        Stream<Object> extensionsOtherThanDigestMethod = context.as(CurrentSourceContext.class)
                .getSource(this)
                .map(ExtensionsType::getAny)
                .map(List::stream)
                .orElseGet(Stream::empty)
                .filter(this::isNotDigestMethod);

        Stream<JAXBElement<DigestMethodType>> newDigestMethods = digestAlgorithms.stream()
                .map(this::createDigestMethod);

        ExtensionsType updatedExtensions = metaDataObjectFactory.createExtensionsType();
        updatedExtensions.getAny()
                .addAll(Stream.concat(newDigestMethods, extensionsOtherThanDigestMethod)
                        .collect(toList()));

        return updatedExtensions;
    }

    private JAXBElement<DigestMethodType> createDigestMethod(DigestAlgorithm algorithm) {
        DigestMethodType digestMethod = algSupportObjectFactory.createDigestMethodType();
        digestMethod.setAlgorithm(algorithm.value());
        return algSupportObjectFactory.createDigestMethod(digestMethod);
    }

    private boolean isNotDigestMethod(Object extension) {
        return !(JAXBIntrospector.getValue(extension) instanceof DigestMethodType);
    }

}
