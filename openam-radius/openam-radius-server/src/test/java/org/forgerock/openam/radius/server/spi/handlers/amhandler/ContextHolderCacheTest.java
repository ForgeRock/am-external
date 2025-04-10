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
 * Copyright 2015-2025 Ping Identity Corporation. All Rights Reserved
 *
 * This code is to be used exclusively in connection with Ping Identity
 * Corporation software or services. Ping Identity Corporation only offers
 * such software or services to legal entities who have entered into a
 * binding license agreement with Ping Identity Corporation.
 */

package org.forgerock.openam.radius.server.spi.handlers.amhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/**
 * Test methods for the <code>ContextHolderCacheTest</code> class.
 *
 * @see org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolderCache
 */
public class ContextHolderCacheTest {

    /**
     * Test for the following method;.
     *
     * @see org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolderCache#createCachedContextHolder
     */
    @Test
    void createCachedContextHolder() {
        // Given
        final ContextCacheSize cacheSize = mock(ContextCacheSize.class);
        when(cacheSize.getDesiredCacheSize()).thenReturn(5);
        final ContextHolderCache cache = new ContextHolderCache(cacheSize);
        // when
        final ContextHolder cachedContext = cache.createCachedContextHolder();
        // then
        assertThat(cachedContext).isNotNull();
        assertThat(cachedContext.getCacheKey()).isNotNull();
    }

    /**
     * Test for the following method;.
     *
     * @see org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolderCache#get
     */
    @Test
    void getReturnsCreatedValue() {
        // Given
        final ContextCacheSize cacheSize = mock(ContextCacheSize.class);
        when(cacheSize.getDesiredCacheSize()).thenReturn(5);
        final ContextHolderCache cache = new ContextHolderCache(cacheSize);
        final ContextHolder cachedContext = cache.createCachedContextHolder();

        // when
        final ContextHolder cachedEntry = cache.get(cachedContext.getCacheKey());
        // then
        assertThat(cachedEntry).isSameAs(cachedContext);
    }

    /**
     * Test for the following method;.
     *
     * @see org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolderCache#getCacheKey
     */
    @Test
    void removeReducesSizeByOne() {
        // Given
        final ContextCacheSize cacheSize = mock(ContextCacheSize.class);
        when(cacheSize.getDesiredCacheSize()).thenReturn(5);
        final ContextHolderCache cache = new ContextHolderCache(cacheSize);
        final ContextHolder cachedContext = cache.createCachedContextHolder();
        final int afterInsertSize = cache.size();

        // When
        cache.remove(cachedContext.getCacheKey());
        // Then
        assertThat(cache.size()).isEqualTo(afterInsertSize - 1);
    }

    /**
     * Test the cache resizing;.
     *
     * @see org.forgerock.openam.radius.server.spi.handlers.amhandler.ContextHolderCache#get
     */
    @Test
    void updateCacheSize() {
        // Given
        final int initialCacheSize = 5;
        final int newCacheSize = 3;
        final ContextCacheSize cacheSize = mock(ContextCacheSize.class);
        when(cacheSize.getDesiredCacheSize()).thenReturn(initialCacheSize, initialCacheSize, initialCacheSize,
                initialCacheSize, initialCacheSize, initialCacheSize, initialCacheSize, initialCacheSize,
                initialCacheSize, initialCacheSize, initialCacheSize, initialCacheSize, initialCacheSize,
                initialCacheSize, initialCacheSize, initialCacheSize, initialCacheSize, initialCacheSize,
                initialCacheSize, initialCacheSize, initialCacheSize, newCacheSize);
        final ContextHolderCache cache = new ContextHolderCache(cacheSize);
        for (Integer i = 0; i < initialCacheSize * 4; ++i) {
            final String idx = i.toString();
            cache.put(idx, new ContextHolder(idx));
        }
        assertThat(cache.size()).isEqualTo(initialCacheSize);
        // So we created 20 cached entries, but max size was 5, so we should have entries 14->19
        for (Integer i = 0; i < 15; ++i) {
            assertThat(cache.get(i.toString())).isNull();
        }
        for (Integer i = 15; i < initialCacheSize * 4; ++i) {
            assertThat(cache.get(i.toString())).isNotNull();
        }

        // Next put will cause resize due to cacheSize returning 3. At this point we should evict oldest entries and
        // max size of the cache should be reported as 3.
        cache.put("20", new ContextHolder("20"));

        // Then
        assertThat(cache.size()).isEqualTo(newCacheSize);
        for (Integer i = 18; i <= 20; ++i) {
            final ContextHolder hldr = cache.get(i.toString());
            assertThat(cache.get(i.toString())).isNotNull();
        }
    }
}
