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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.objectenricher.EnricherContext.ROOT;

import java.util.List;

import org.forgerock.openam.federation.rest.schema.shared.Endpoint;
import org.testng.annotations.Test;

import com.sun.identity.saml2.jaxb.metadata.IndexedEndpointType;

/**
 * Unit test for {@link IndexedEndpointMapper}.
 *
 * @since 7.0.0
 */
public final class IndexedEndpointMapperTest {

    private IndexedEndpointMapper mapper = new IndexedEndpointMapper();

    @Test
    public void whenIndexedEndpointTypePassedItIsMapperAccordingly() {
        // Given
        IndexedEndpointType indexedEndpointType = new IndexedEndpointType();
        indexedEndpointType.setIndex(3);
        indexedEndpointType.setIsDefault(true);
        indexedEndpointType.setLocation("some-location");
        indexedEndpointType.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        indexedEndpointType.setResponseLocation("some-response-location");

        // When
        List<Endpoint> endpoints = mapper.map(singletonList(indexedEndpointType), ROOT);

        // Then
        assertThat(endpoints).isNotEmpty();
        assertThat(endpoints).hasSize(1);

        Endpoint endpoint = endpoints.get(0);
        assertThat(endpoint.getLocation()).isEqualTo("some-location");
        assertThat(endpoint.getResponseLocation()).isEqualTo("some-response-location");
        assertThat(endpoint.getBinding()).isEqualTo("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
    }

    @Test
    public void whenIndexedEndpointPassedItIsMappedBackAccordingly() {
        // Given
        Endpoint endpoint = new Endpoint();
        endpoint.setLocation("some-location");
        endpoint.setBinding("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        endpoint.setResponseLocation("some-response-location");

        // When
        List<IndexedEndpointType> indexedEndpointTypes = mapper.inverse(singletonList(endpoint), ROOT);

        // Then
        assertThat(indexedEndpointTypes).isNotEmpty();
        assertThat(indexedEndpointTypes).hasSize(1);

        IndexedEndpointType indexedEndpointType = indexedEndpointTypes.get(0);
        assertThat(indexedEndpointType.getLocation()).isEqualTo("some-location");
        assertThat(indexedEndpointType.getResponseLocation()).isEqualTo("some-response-location");
        assertThat(indexedEndpointType.getBinding()).isEqualTo("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
    }

}
