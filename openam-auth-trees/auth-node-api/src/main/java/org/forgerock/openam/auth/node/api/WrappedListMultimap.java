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
 * Copyright 2018 ForgeRock AS.
 */

package org.forgerock.openam.auth.node.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.forgerock.guava.common.collect.ListMultimap;
import org.forgerock.guava.common.collect.Multimap;
import org.forgerock.guava.common.collect.Multiset;

import com.google.common.collect.Multimaps;

class WrappedListMultimap<K, V> implements ListMultimap<K, V> {
    private final com.google.common.collect.ListMultimap<K, V> delegate;

    WrappedListMultimap(com.google.common.collect.ListMultimap<K, V> multimap) {
        this.delegate = multimap;
    }

    @Override
    public List<V> get(@Nullable K key) {
        return delegate.get(key);
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Multiset<K> keys() {
        return new WrappedMultiset(delegate.keys());
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public Collection<Map.Entry<K, V>> entries() {
        return delegate.entries();
    }

    @Override
    public List<V> removeAll(@Nullable Object key) {
        return delegate.removeAll(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
        return delegate.containsEntry(key, value);
    }

    @Override
    public boolean put(@Nullable K key, @Nullable V value) {
        return delegate.put(key, value);
    }

    @Override
    public boolean remove(@Nullable Object key, @Nullable Object value) {
        return delegate.remove(key, value);
    }

    @Override
    public boolean putAll(@Nullable K key, Iterable<? extends V> values) {
        return delegate.putAll(key, values);
    }

    @Override
    public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
        boolean changed = false;

        for (Map.Entry<? extends K, ? extends V> entry : multimap.entries()) {
            changed |= this.put(entry.getKey(), entry.getValue());
        }

        return changed;
    }

    @Override
    public List<V> replaceValues(K key, Iterable<? extends V> values) {
        return delegate.replaceValues(key, values);
    }

    @Override
    public Map<K, Collection<V>> asMap() {
        return delegate.asMap();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Multimap) {
            return delegate.equals(Multimaps.newListMultimap(((Multimap) obj).asMap(), ArrayList::new));
        }
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
