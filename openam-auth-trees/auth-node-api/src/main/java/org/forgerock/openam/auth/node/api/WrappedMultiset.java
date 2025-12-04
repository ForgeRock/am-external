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
 * Copyright 2018-2025 Ping Identity Corporation.
 */

package org.forgerock.openam.auth.node.api;

import static com.google.common.collect.Multisets.immutableEntry;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.forgerock.guava.common.collect.Multiset;

import com.google.common.collect.ImmutableMultiset;

/**
 * A wrapper around a Guava {@link com.google.common.collect.Multiset} to make it compatible with the
 * {@link Multiset} interface.
 *
 * @param <K> the type of elements in the multiset
 */
class WrappedMultiset<K> implements Multiset<K> {
    private final com.google.common.collect.Multiset<K> delegate;

    WrappedMultiset(com.google.common.collect.Multiset<K> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int count(@Nullable Object element) {
        return delegate.count(element);
    }

    @Override
    public int add(@Nullable K element, int occurrences) {
        return delegate.add(element, occurrences);
    }

    @Override
    public int remove(@Nullable Object element, int occurrences) {
        return delegate.remove(element, occurrences);
    }

    @Override
    public int setCount(K element, int count) {
        return delegate.setCount(element, count);
    }

    @Override
    public boolean setCount(K element, int oldCount, int newCount) {
        return delegate.setCount(element, oldCount, newCount);
    }

    @Override
    public Set<K> elementSet() {
        return delegate.elementSet();
    }

    @Override
    public Set<Entry<K>> entrySet() {
        return new WrappedEntrySet<>(delegate.entrySet());
    }

    @Override
    public Iterator<K> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
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
    public boolean contains(@Nullable Object element) {
        return delegate.contains(element);
    }

    @Override
    public boolean containsAll(Collection<?> elements) {
        return delegate.containsAll(elements);
    }

    @Override
    public boolean addAll(Collection<? extends K> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean add(K element) {
        return delegate.add(element);
    }

    @Override
    public boolean remove(@Nullable Object element) {
        return delegate.remove(element);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Multiset) {
            return delegate.equals(ImmutableMultiset.copyOf(((Multiset) obj).elementSet()));
        }
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    private static final class WrappedEntrySet<KK> implements Set<Entry<KK>> {
        private final Set<com.google.common.collect.Multiset.Entry<KK>> delegate;

        private WrappedEntrySet(Set<com.google.common.collect.Multiset.Entry<KK>> delegate) {
            this.delegate = delegate;
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
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<Entry<KK>> iterator() {
            return new WrappedIterator(delegate.iterator());
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(Entry<KK> kkEntry) {
            return delegate.add(immutableEntry(kkEntry.getElement(), kkEntry.getCount()));
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends Entry<KK>> c) {
            boolean changed = false;

            for (Entry<KK> entry : c) {
                changed |= this.add(entry);
            }

            return changed;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c.stream()
                    .map(e -> e instanceof Entry ? immutableEntry(((Entry) e).getElement(), ((Entry) e).getCount()) : e)
                    .collect(Collectors.toList()));
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c.stream()
                    .map(e -> e instanceof Entry ? immutableEntry(((Entry) e).getElement(), ((Entry) e).getCount()) : e)
                    .collect(Collectors.toList()));
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    private static final class WrappedIterator<KK> implements Iterator<Entry<KK>> {

        private final Iterator<com.google.common.collect.Multiset.Entry<KK>> delegate;

        WrappedIterator(Iterator<com.google.common.collect.Multiset.Entry<KK>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Entry<KK> next() {
            return new WrappedEntry<>(delegate.next());
        }

        @Override
        public void remove() {
            delegate.remove();
        }

        @Override
        public void forEachRemaining(Consumer<? super Entry<KK>> action) {
            delegate.forEachRemaining(entry -> action.accept(new WrappedEntry<>(entry)));
        }
    }

    private static final class WrappedEntry<KK> implements Entry<KK> {

        private final com.google.common.collect.Multiset.Entry<KK> delegate;

        WrappedEntry(com.google.common.collect.Multiset.Entry<KK> delegate) {
            this.delegate = delegate;
        }

        @Override
        public KK getElement() {
            return delegate.getElement();
        }

        @Override
        public int getCount() {
            return delegate.getCount();
        }
    }
}
