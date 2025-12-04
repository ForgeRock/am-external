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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.Endpoint;
import org.junit.jupiter.api.Test;

import com.sun.identity.saml2.jaxb.metadata.EndpointType;

/**
 * Unit test for {@link EndpointMapper}.
 *
 * @since 7.0.0
 */
public final class EndpointMapperTest {

    private EndpointMapper mapper = new EndpointMapper();

    @Test
    void whenEndpointTypePassedItIsMapperAccordingly() {
        // Given
        EndpointType endpointType = new EndpointType();
        endpointType.setLocation("some-location");
        endpointType.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        endpointType.setResponseLocation("some-response-location");

        // When
        List<Endpoint> endpoints = mapper.map(singletonList(endpointType), ROOT);

        // Then
        assertThat(endpoints).isNotEmpty();
        assertThat(endpoints).hasSize(1);

        Endpoint endpoint = endpoints.get(0);
        assertThat(endpoint.getLocation()).isEqualTo("some-location");
        assertThat(endpoint.getResponseLocation()).isEqualTo("some-response-location");
        assertThat(endpoint.getBinding()).isEqualTo("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
    }

    @Test
    void whenEndpointPassedItIsMappedBackAccordingly() {
        // Given
        Endpoint endpoint = new Endpoint();
        endpoint.setLocation("some-location");
        endpoint.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        endpoint.setResponseLocation("some-response-location");

        // When
        List<EndpointType> endpointTypes = mapper.inverse(singletonList(endpoint), ROOT);

        // Then
        assertThat(endpointTypes).isNotEmpty();
        assertThat(endpointTypes).hasSize(1);

        EndpointType endpointType = endpointTypes.get(0);
        assertThat(endpointType.getLocation()).isEqualTo("some-location");
        assertThat(endpointType.getResponseLocation()).isEqualTo("some-response-location");
        assertThat(endpointType.getBinding()).isEqualTo("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
    }

}
