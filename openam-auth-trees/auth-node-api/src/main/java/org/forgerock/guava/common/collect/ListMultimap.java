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
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Portions Copyright 2018-2025 Ping Identity Corporation.
 */

package org.forgerock.guava.common.collect;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * In order to maintain binary compatibility between product versions less than 6.5, which used forgerock-guava (a
 * repackaged distribution of Guava 18.0) and version 6.5, this interface is copied from the guava project.
 *
 * <p>A {@code Multimap} that can hold duplicate key-value pairs and that maintains
 * the insertion ordering of values for a given key. See the {@link Multimap}
 * documentation for information common to all multimaps.
 *
 * <p>The {@link #get}, {@link #removeAll}, and {@link #replaceValues} methods
 * each return a {@link List} of values. Though the method signature doesn't say
 * so explicitly, the map returned by {@link #asMap} has {@code List} values.
 *
 * <p>See the Guava User Guide article on <a href=
 * "http://code.google.com/p/guava-libraries/wiki/NewCollectionTypesExplained#Multimap">
 * {@code Multimap}</a>.
 *
 * @author Jared Levy
 * @since 2.0 (imported from Google Collections Library)
 * @deprecated Use {@link com.google.common.collect.ListMultimap} instead.
 */
@Deprecated
@GwtCompatible
public interface ListMultimap<K, V> extends Multimap<K, V> {
    /**
     * {@inheritDoc}
     *
     * <p>Because the values for a given key may have duplicates and follow the
     * insertion ordering, this method returns a {@link List}, instead of the
     * {@link java.util.Collection} specified in the {@link Multimap} interface.
     */
    @Override
    List<V> get(@Nullable K key);

    /**
     * {@inheritDoc}
     *
     * <p>Because the values for a given key may have duplicates and follow the
     * insertion ordering, this method returns a {@link List}, instead of the
     * {@link java.util.Collection} specified in the {@link Multimap} interface.
     */
    @Override
    List<V> removeAll(@Nullable Object key);

    /**
     * {@inheritDoc}
     *
     * <p>Because the values for a given key may have duplicates and follow the
     * insertion ordering, this method returns a {@link List}, instead of the
     * {@link java.util.Collection} specified in the {@link Multimap} interface.
     */
    @Override
    List<V> replaceValues(K key, Iterable<? extends V> values);

    /**
     * {@inheritDoc}
     *
     * <p><b>Note:</b> The returned map's values are guaranteed to be of type
     * {@link List}. To obtain this map with the more specific generic type
     * {@code Map<K, List<V>>}, call
     * {@link com.google.common.collect.Multimaps#asMap(com.google.common.collect.ListMultimap)} instead.
     */
    @Override
    Map<K, Collection<V>> asMap();

    /**
     * Compares the specified object to this multimap for equality.
     *
     * <p>Two {@code ListMultimap} instances are equal if, for each key, they
     * contain the same values in the same order. If the value orderings disagree,
     * the multimaps will not be considered equal.
     *
     * <p>An empty {@code ListMultimap} is equal to any other empty {@code
     * Multimap}, including an empty {@code SetMultimap}.
     */
    @Override
    boolean equals(@Nullable Object obj);
}
