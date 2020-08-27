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
 * Copyright 2020 ForgeRock AS.
 */

package org.forgerock.openam.federation.rest.schema.mappers;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.Endpoint;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

import com.sun.identity.saml2.jaxb.metadata.IndexedEndpointType;
import com.sun.identity.saml2.jaxb.metadata.ObjectFactory;

/**
 * Mapper used to map between {@link IndexedEndpointType} and {@link Endpoint} types.
 *
 * @since 7.0.0
 */
public final class IndexedEndpointMapper extends ValueMapper<List<IndexedEndpointType>, List<Endpoint>> {

    private final ObjectFactory objectFactory;

    /**
     * Create a new mapper instance.
     */
    public IndexedEndpointMapper() {
        objectFactory = new ObjectFactory();
    }

    @Override
    public List<Endpoint> map(List<IndexedEndpointType> indexedEndpointTypes, EnricherContext context) {
        return indexedEndpointTypes.stream()
                .map(this::jaxbToPojo)
                .collect(toList());
    }

    @Override
    public List<IndexedEndpointType> inverse(List<Endpoint> indexedEndpoints, EnricherContext context) {
        return indexedEndpoints.stream()
                .map(this::pojoToJaxb)
                .collect(toList());
    }

    private Endpoint jaxbToPojo(IndexedEndpointType endpointType) {
        Endpoint indexedEndpoint = new Endpoint();
        indexedEndpoint.setBinding(endpointType.getBinding());
        indexedEndpoint.setLocation(endpointType.getLocation());
        indexedEndpoint.setResponseLocation(endpointType.getResponseLocation());
        return indexedEndpoint;
    }

    private IndexedEndpointType pojoToJaxb(Endpoint endpoint) {
        IndexedEndpointType indexedEndpointType = objectFactory.createIndexedEndpointType();
        indexedEndpointType.setBinding(endpoint.getBinding().toString());
        indexedEndpointType.setLocation(endpoint.getLocation());
        indexedEndpointType.setResponseLocation(endpoint.getResponseLocation());
        return indexedEndpointType;
    }

}
