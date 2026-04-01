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
 * Copyright 2025 Ping Identity Corporation.
 */
package com.sun.identity.saml2.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.SAMLData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.sun.identity.saml2.profile.SPCache;

class SAMLDataAccessorTest {

    @BeforeEach
    void setUp() {
        // Clear the cache before each test
        SPCache.samlDataHash.clear();
    }

    @Test
    void shouldNotUseCacheWhenGetSAMLDataGivenKeyIsNotInCache() throws SAML2TokenRepositoryException {
        // Given
        String key = "uncachedKey";
        SAMLData samlData = mock(SAMLData.class);
        try (MockedStatic<SAML2FailoverUtils> mockedStatic = org.mockito.Mockito.mockStatic(SAML2FailoverUtils.class)) {
            mockedStatic.when(() -> SAML2FailoverUtils.retrieveSAML2Token(key)).thenReturn(samlData);

            // When
            var result = SAMLDataAccessor.getSAMLData(key);
            var secondRetrieval = SAMLDataAccessor.getSAMLData(key);

            // Then
            assertEquals(samlData, result, "Result should match the retrieved data");
            assertEquals(secondRetrieval, result, "Result should match the retrieved data");
            mockedStatic.verify(() -> SAML2FailoverUtils.retrieveSAML2Token(key), org.mockito.Mockito.times(1));
            mockedStatic.verify(() -> SAML2FailoverUtils.deleteSAML2Token(key), org.mockito.Mockito.times(0));
        }
    }

    @Test
    void shouldUseCacheWhenGetSAMLDataGivenKeyIsNotInCache() throws SAML2TokenRepositoryException {
        // Given
        String key = "cachedKey";
        SAMLData samlData = mock(SAMLData.class);
        SPCache.samlDataHash.put(key, samlData);

        try (MockedStatic<SAML2FailoverUtils> mockedStatic = org.mockito.Mockito.mockStatic(SAML2FailoverUtils.class)) {
            // When
            var result = SAMLDataAccessor.getSAMLData(key);

            // Then
            assertEquals(samlData, result, "Result should match the cached data");
            mockedStatic.verify(() -> SAML2FailoverUtils.retrieveSAML2Token(key), org.mockito.Mockito.times(0));
        }
    }
}