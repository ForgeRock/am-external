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
 * Copyright 2026 Ping Identity Corporation.
 */
package org.forgerock.openam.auth.nodes.framework;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.forgerock.am.trees.api.Tree;
import org.forgerock.openam.auth.node.api.InputState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Ticker;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * A singleton cache manager for tree input states.
 *
 * <p>This cache is used to store the aggregated {@link InputState} requirements of authentication trees.
 * Traversing large or nested trees to determine required inputs is a resource-intensive operation;
 * this class significantly improves performance by buffering the results for a short period.</p>
 *
 * <p>The cache employs an expiration policy of 5 seconds to ensure that any configuration changes
 * made to the authentication trees are propagated relatively quickly while still protecting the
 * system from high-frequency processing overhead during peak login periods.</p>
 */
@Singleton
public class InputStatesCache {

    private static final Logger logger = LoggerFactory.getLogger(InputStatesCache.class);
    private final LoadingCache<TreeCacheKey, List<InputState>> cache;

    @Inject
    InputStatesCache(Ticker ticker) {
        cache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .maximumSize(1000)
                .ticker(ticker)
                .build(new CacheLoader<>() {
                    @Override
                    public List<InputState> load(TreeCacheKey key) throws Exception {
                        logger.debug("Cache miss for tree {}. Visiting nodes...", key.tree().getName());
                        return key.tree.visitNodes(new InputCollector());
                    }
                });
    }

    /**
     * Retrieves the cached input states for a specific tree, or computes them using the provided loader.
     *
     * <p>If the input states for the given {@code treeId} are not present in the cache, the
     * {@code loader} is executed, and its result is stored in the cache before being returned.</p>
     *
     * @param tree The authentication tree.
     * @return A list of {@link InputState} objects required by the specified tree.
     * @throws ExecutionException If an error occurs during the computation of the input states.
     */
    public List<InputState> get(Tree tree) throws ExecutionException {
        return cache.get(new TreeCacheKey(tree));
    }

    private record TreeCacheKey(Tree tree) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TreeCacheKey other = (TreeCacheKey) o;
            return other.tree.getName().equals(this.tree.getName())
                    && other.tree.getRealm().equals(this.tree.getRealm());
        }

        @Override
        public int hashCode() {
            return Objects.hash(tree.getName(), tree.getRealm());
        }
    }
}