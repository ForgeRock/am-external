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
 * Copyright 2019-2020 ForgeRock AS.
 */

package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.ExtendedIndexedEndpoint;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

import com.sun.identity.saml2.jaxb.metadata.IndexedEndpointType;
import com.sun.identity.saml2.jaxb.metadata.ObjectFactory;

/**
 * Mapper used to map between {@link IndexedEndpointType} and {@link ExtendedIndexedEndpoint} types.
 *
 * @since 7.0.0
 */
public final class ExtendedIndexedEndpointMapper
        extends ValueMapper<List<IndexedEndpointType>, List<ExtendedIndexedEndpoint>> {

    private final ObjectFactory objectFactory;

    /**
     * Create a new mapper instance.
     */
    public ExtendedIndexedEndpointMapper() {
        objectFactory = new ObjectFactory();
    }

    @Override
    public List<ExtendedIndexedEndpoint> map(
            List<IndexedEndpointType> indexedEndpointTypes, EnricherContext context) {
        return indexedEndpointTypes.stream()
                .map(this::jaxbToPojo)
                .collect(toList());
    }

    @Override
    public List<IndexedEndpointType> inverse(
            List<ExtendedIndexedEndpoint> assertionConsumers, EnricherContext context) {
        return assertionConsumers.stream()
                .map(this::pojoToJaxb)
                .collect(toList());
    }

    private ExtendedIndexedEndpoint jaxbToPojo(IndexedEndpointType indexedEndpointType) {
        ExtendedIndexedEndpoint extendedIndexedEndpoint = new ExtendedIndexedEndpoint();
        extendedIndexedEndpoint.setIsDefault(indexedEndpointType.isIsDefault());
        extendedIndexedEndpoint.setBinding(indexedEndpointType.getBinding());
        extendedIndexedEndpoint.setLocation(indexedEndpointType.getLocation());
        extendedIndexedEndpoint.setIndex(indexedEndpointType.getIndex());
        return extendedIndexedEndpoint;
    }

    private IndexedEndpointType pojoToJaxb(ExtendedIndexedEndpoint extendedIndexedEndpoint) {
        IndexedEndpointType indexedEndpointType = objectFactory.createIndexedEndpointType();
        indexedEndpointType.setIsDefault(extendedIndexedEndpoint.getIsDefault());
        indexedEndpointType.setBinding(extendedIndexedEndpoint.getBinding().toString());
        indexedEndpointType.setLocation(extendedIndexedEndpoint.getLocation());
        indexedEndpointType.setIndex(extendedIndexedEndpoint.getIndex());
        return indexedEndpointType;
    }
}
