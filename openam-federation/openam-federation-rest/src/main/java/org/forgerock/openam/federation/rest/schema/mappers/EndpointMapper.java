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

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.Endpoint;
import org.forgerock.openam.objectenricher.EnricherContext;
import org.forgerock.openam.objectenricher.mapper.ValueMapper;

import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.ObjectFactory;

/**
 * Mapper used to map between {@link EndpointType} and {@link Endpoint} types.
 *
 * @since 7.0.0
 */
public final class EndpointMapper extends ValueMapper<List<EndpointType>, List<Endpoint>> {

    private final ObjectFactory objectFactory;

    /**
     * Create a new mapper instance.
     */
    public EndpointMapper() {
        objectFactory = new ObjectFactory();
    }

    @Override
    public List<Endpoint> map(List<EndpointType> endpoints, EnricherContext context) {
        return endpoints.stream()
                .map(this::jaxbToPojo)
                .collect(toList());
    }

    @Override
    public List<EndpointType> inverse(List<Endpoint> endpoints, EnricherContext context) {
        return endpoints.stream()
                .map(this::pojoToJaxb)
                .collect(toList());
    }

    private Endpoint jaxbToPojo(EndpointType endpointType) {
        Endpoint endpoint = new Endpoint();
        endpoint.setBinding(endpointType.getBinding());
        endpoint.setLocation(endpointType.getLocation());
        endpoint.setResponseLocation(endpointType.getResponseLocation());
        return endpoint;
    }

    private EndpointType pojoToJaxb(Endpoint endpoint) {
        EndpointType endpointType = objectFactory.createEndpointType();
        endpointType.setBinding(endpoint.getBinding().toString());
        endpointType.setLocation(endpoint.getLocation());
        endpointType.setResponseLocation(endpoint.getResponseLocation());
        return endpointType;
    }

}
