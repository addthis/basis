/*
*  Licensed to the Apache Software Foundation (ASF) under one or more
*  contributor license agreements. See the NOTICE file distributed with
*  this work for additional information regarding copyright ownership.
*  The ASF licenses this file to You under the Apache License, Version 2.0
*  (the "License"); you may not use this file except in compliance with
*  the License. You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package com.addthis.basis.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.base.Objects;

/**
 * HashMap is an implementation of {@link Map}. All optional operations are supported.
 * <p/>
 * <p>All elements are permitted as keys or values, including null.
 * <p/>
 * <p>Note that the iteration order for HashMap is non-deterministic. If you want
 * deterministic iteration, use {@link java.util.LinkedHashMap}.
 * <p/>
 * <p>Note: the implementation of {@code HashMap} is not synchronized.
 * If one thread of several threads accessing an instance modifies the map
 * structurally, access to the map needs to be synchronized. A structural
 * modification is an operation that adds or removes an entry. Changes in
 * the value of an entry are not structural changes.
 * <p/>
 * <p>The {@code Iterator} created by calling the {@code iterator} method
 * may throw a {@code ConcurrentModificationException} if the map is structurally
 * changed while an iterator is used to iterate over the elements. Only the
 * {@code remove} method that is provided by the iterator allows for removal of
 * elements during iteration. It is not possible to guarantee that this
 * mechanism works in all cases of unsynchronized concurrent modification. It
 * should only be used for debugging purposes.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class AndroidHashMap<K, V> extends AbstractMap<K, V> implements Cloneable, Serializable {

    /**
     * Min capacity (other than zero) for a HashMap. Must be a power of two
     * greater than 1 (and less than 1 << 30).
     */
    private static final int MINIMUM_CAPACITY = 4;

    /**
     * Max capacity for a HashMap. Must be a power of two >= MINIMUM_CAPACITY.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * An empty table shared by all zero-capacity maps (typically from default
     * constructor). It is never written to, and replaced on first put. Its size
     * is set to half the minimum, so that the first resize will create a
     * minimum-sized table.
     */
    private static final Entry[] EMPTY_TABLE
            = new AndroidHashMapEntry[MINIMUM_CAPACITY >>> 1];

    /**
     * The default load factor. Note that this implementation ignores the
     * load factor, but cannot do away with it entirely because it's
     * mentioned in the API.
     * <p/>
     * <p>Note that this constant has no impact on the behavior of the program,
     * but it is emitted as part of the serialized form. The load factor of
     * .75 is hardwired into the program, which uses cheap shifts in place of
     * expensive division.
     */
    static final float DEFAULT_LOAD_FACTOR = .75F;

    /**
     * The hash table. If this hash map contains a mapping for null, it is
     * not represented this hash table.
     */
    transient AndroidHashMapEntry<K, V>[] table;

    /**
     * The entry representing the null key, or null if there's no such mapping.
     */
    transient AndroidHashMapEntry<K, V> entryForNullKey;

    /**
     * The number of mappings in this hash map.
     */
    transient int size;

    /**
     * Incremented by "structural modifications" to allow (best effort)
     * detection of concurrent modification.
     */
    transient int modCount;

    /**
     * The table is rehashed when its size exceeds this threshold.
     * The value of this field is generally .75 * capacity, except when
     * the capacity is zero, as described in the EMPTY_TABLE declaration
     * above.
     */
    private transient int threshold;

    // Views - lazily initialized
    private transient Set<K> keySet;
    private transient Set<Entry<K, V>> entrySet;
    private transient Collection<V> values;

    /**
     * Constructs a new empty {@code HashMap} instance.
     */
    @SuppressWarnings("unchecked")
    public AndroidHashMap() {
        table = (AndroidHashMapEntry<K, V>[]) EMPTY_TABLE;
        threshold = -1; // Forces first put invocation to replace EMPTY_TABLE
    }

    /**
     * Constructs a new {@code HashMap} instance with the specified capacity.
     *
     * @param capacity the initial capacity of this hash map.
     * @throws IllegalArgumentException when the capacity is less than zero.
     */
    public AndroidHashMap(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity: " + capacity);
        }

        if (capacity == 0) {
            @SuppressWarnings("unchecked")
            AndroidHashMapEntry<K, V>[] tab = (AndroidHashMapEntry<K, V>[]) EMPTY_TABLE;
            table = tab;
            threshold = -1; // Forces first put() to replace EMPTY_TABLE
            return;
        }

        if (capacity < MINIMUM_CAPACITY) {
            capacity = MINIMUM_CAPACITY;
        } else if (capacity > MAXIMUM_CAPACITY) {
            capacity = MAXIMUM_CAPACITY;
        } else {
            capacity = roundUpToPowerOfTwo(capacity);
        }
        makeTable(capacity);
    }

    /**
     * Constructs a new {@code HashMap} instance with the specified capacity and
     * load factor.
     *
     * @param capacity   the initial capacity of this hash map.
     * @param loadFactor the initial load factor.
     * @throws IllegalArgumentException when the capacity is less than zero or the load factor is
     *                                  less or equal to zero or NaN.
     */
    public AndroidHashMap(int capacity, float loadFactor) {
        this(capacity);

        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor: " + loadFactor);
        }

        /*
         * Note that this implementation ignores loadFactor; it always uses
         * a load factor of 3/4. This simplifies the code and generally
         * improves performance.
         */
    }

    /**
     * Constructs a new {@code HashMap} instance containing the mappings from
     * the specified map.
     *
     * @param map the mappings to add.
     */
    public AndroidHashMap(Map<? extends K, ? extends V> map) {
        this(capacityForInitSize(map.size()));
        constructorPutAll(map);
    }

    /**
     * Inserts all of the elements of map into this HashMap in a manner
     * suitable for use by constructors and pseudo-constructors (i.e., clone,
     * readObject). Also used by LinkedHashMap.
     */
    final void constructorPutAll(Map<? extends K, ? extends V> map) {
        if (table == EMPTY_TABLE) {
            doubleCapacity(); // Don't do unchecked puts to a shared table.
        }
        for (Entry<? extends K, ? extends V> e : map.entrySet()) {
            constructorPut(e.getKey(), e.getValue());
        }
    }

    /**
     * Returns an appropriate capacity for the specified initial size. Does
     * not round the result up to a power of two; the caller must do this!
     * The returned value will be between 0 and MAXIMUM_CAPACITY (inclusive).
     */
    static int capacityForInitSize(int size) {
        int result = (size >> 1) + size; // Multiply by 3/2 to allow for growth

        // boolean expr is equivalent to result >= 0 && result<MAXIMUM_CAPACITY
        return (result & ~(MAXIMUM_CAPACITY - 1)) == 0 ? result : MAXIMUM_CAPACITY;
    }

    /**
     * Returns a shallow copy of this map.
     *
     * @return a shallow copy of this map.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        /*
         * This could be made more efficient. It unnecessarily hashes all of
         * the elements in the map.
         */
        AndroidHashMap<K, V> result;
        try {
            result = (AndroidHashMap<K, V>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }

        // Restore clone to empty state, retaining our capacity and threshold
        result.makeTable(table.length);
        result.entryForNullKey = null;
        result.size = 0;
        result.keySet = null;
        result.entrySet = null;
        result.values = null;

        result.init(); // Give subclass a chance to initialize itself
        result.constructorPutAll(this); // Calls method overridden in subclass!!
        return result;
    }

    /**
     * This method is called from the pseudo-constructors (clone and readObject)
     * prior to invoking constructorPut/constructorPutAll, which invoke the
     * overridden constructorNewEntry method. Normally it is a VERY bad idea to
     * invoke an overridden method from a pseudo-constructor (Effective Java
     * Item 17). In this case it is unavoidable, and the init method provides a
     * workaround.
     */
    void init() {
    }

    /**
     * Returns whether this map is empty.
     *
     * @return {@code true} if this map has no elements, {@code false}
     * otherwise.
     * @see #size()
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements in this map.
     *
     * @return the number of elements in this map.
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns the value of the mapping with the specified key.
     *
     * @param key the key.
     * @return the value of the mapping with the specified key, or {@code null}
     * if no mapping for the specified key is found.
     */
    public V get(Object key) {
        if (key == null) {
            AndroidHashMapEntry<K, V> e = entryForNullKey;
            return e == null ? null : e.value;
        }

        int hash = secondaryHash(key);
        AndroidHashMapEntry<K, V>[] tab = table;
        for (AndroidHashMapEntry<K, V> e = tab[hash & (tab.length - 1)];
             e != null; e = e.next) {
            K eKey = e.key;
            if (eKey == key || (e.hash == hash && key.equals(eKey))) {
                return e.value;
            }
        }
        return null;
    }

    /**
     * Returns whether this map contains the specified key.
     *
     * @param key the key to search for.
     * @return {@code true} if this map contains the specified key,
     * {@code false} otherwise.
     */
    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return entryForNullKey != null;
        }

        int hash = secondaryHash(key);
        AndroidHashMapEntry<K, V>[] tab = table;
        for (AndroidHashMapEntry<K, V> e = tab[hash & (tab.length - 1)];
             e != null; e = e.next) {
            K eKey = e.key;
            if (eKey == key || (e.hash == hash && key.equals(eKey))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether this map contains the specified value.
     *
     * @param value the value to search for.
     * @return {@code true} if this map contains the specified value,
     * {@code false} otherwise.
     */
    @Override
    public boolean containsValue(Object value) {
        AndroidHashMapEntry[] tab = table;
        int len = tab.length;
        if (value == null) {
            for (int i = 0; i < len; i++) {
                for (AndroidHashMapEntry e = tab[i]; e != null; e = e.next) {
                    if (e.value == null) {
                        return true;
                    }
                }
            }
            return entryForNullKey != null && entryForNullKey.value == null;
        }

        // value is non-null
        for (int i = 0; i < len; i++) {
            for (AndroidHashMapEntry e = tab[i]; e != null; e = e.next) {
                if (value.equals(e.value)) {
                    return true;
                }
            }
        }
        return entryForNullKey != null && value.equals(entryForNullKey.value);
    }

    /**
     * Maps the specified key to the specified value.
     *
     * @param key   the key.
     * @param value the value.
     * @return the value of any previous mapping with the specified key or
     * {@code null} if there was no such mapping.
     */
    @Override
    public V put(K key, V value) {
        if (key == null) {
            return putValueForNullKey(value);
        }

        int hash = secondaryHash(key);
        AndroidHashMapEntry<K, V>[] tab = table;
        int index = hash & (tab.length - 1);
        for (AndroidHashMapEntry<K, V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && key.equals(e.key)) {
                preModify(e);
                V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }

        // No entry for (non-null) key is present; create one
        modCount++;
        if (size++ > threshold) {
            tab = doubleCapacity();
            index = hash & (tab.length - 1);
        }
        addNewEntry(key, value, hash, index);
        return null;
    }

    private V putValueForNullKey(V value) {
        AndroidHashMapEntry<K, V> entry = entryForNullKey;
        if (entry == null) {
            addNewEntryForNullKey(value);
            size++;
            modCount++;
            return null;
        } else {
            preModify(entry);
            V oldValue = entry.value;
            entry.value = value;
            return oldValue;
        }
    }

    /**
     * Give LinkedHashMap a chance to take action when we modify an existing
     * entry.
     *
     * @param e the entry we're about to modify.
     */
    void preModify(AndroidHashMapEntry<K, V> e) {
    }

    /**
     * This method is just like put, except that it doesn't do things that
     * are inappropriate or unnecessary for constructors and pseudo-constructors
     * (i.e., clone, readObject). In particular, this method does not check to
     * ensure that capacity is sufficient, and does not increment modCount.
     */
    private void constructorPut(K key, V value) {
        if (key == null) {
            AndroidHashMapEntry<K, V> entry = entryForNullKey;
            if (entry == null) {
                entryForNullKey = constructorNewEntry(null, value, 0, null);
                size++;
            } else {
                entry.value = value;
            }
            return;
        }

        int hash = secondaryHash(key);
        AndroidHashMapEntry<K, V>[] tab = table;
        int index = hash & (tab.length - 1);
        AndroidHashMapEntry<K, V> first = tab[index];
        for (AndroidHashMapEntry<K, V> e = first; e != null; e = e.next) {
            if (e.hash == hash && key.equals(e.key)) {
                e.value = value;
                return;
            }
        }

        // No entry for (non-null) key is present; create one
        tab[index] = constructorNewEntry(key, value, hash, first);
        size++;
    }

    /**
     * Creates a new entry for the given key, value, hash, and index and
     * inserts it into the hash table. This method is called by put
     * (and indirectly, putAll), and overridden by LinkedHashMap. The hash
     * must incorporate the secondary hash function.
     */
    void addNewEntry(K key, V value, int hash, int index) {
        table[index] = new AndroidHashMapEntry<K, V>(key, value, hash, table[index]);
    }

    /**
     * Creates a new entry for the null key, and the given value and
     * inserts it into the hash table. This method is called by put
     * (and indirectly, putAll), and overridden by LinkedHashMap.
     */
    void addNewEntryForNullKey(V value) {
        entryForNullKey = new AndroidHashMapEntry<K, V>(null, value, 0, null);
    }

    /**
     * Like newEntry, but does not perform any activity that would be
     * unnecessary or inappropriate for constructors. In this class, the
     * two methods behave identically; in LinkedHashMap, they differ.
     */
    AndroidHashMapEntry<K, V> constructorNewEntry(
            K key, V value, int hash, AndroidHashMapEntry<K, V> first) {
        return new AndroidHashMapEntry<K, V>(key, value, hash, first);
    }

    /**
     * Copies all the mappings in the specified map to this map. These mappings
     * will replace all mappings that this map had for any of the keys currently
     * in the given map.
     *
     * @param map the map to copy mappings from.
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        ensureCapacity(map.size());
        super.putAll(map);
    }

    /**
     * Ensures that the hash table has sufficient capacity to store the
     * specified number of mappings, with room to grow. If not, it increases the
     * capacity as appropriate. Like doubleCapacity, this method moves existing
     * entries to new buckets as appropriate. Unlike doubleCapacity, this method
     * can grow the table by factors of 2^n for n > 1. Hopefully, a single call
     * to this method will be faster than multiple calls to doubleCapacity.
     * <p/>
     * <p>This method is called only by putAll.
     */
    private void ensureCapacity(int numMappings) {
        int newCapacity = roundUpToPowerOfTwo(capacityForInitSize(numMappings));
        AndroidHashMapEntry<K, V>[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (newCapacity <= oldCapacity) {
            return;
        }
        if (newCapacity == oldCapacity * 2) {
            doubleCapacity();
            return;
        }

        // We're growing by at least 4x, rehash in the obvious way
        AndroidHashMapEntry<K, V>[] newTable = makeTable(newCapacity);
        if (size != 0) {
            int newMask = newCapacity - 1;
            for (int i = 0; i < oldCapacity; i++) {
                for (AndroidHashMapEntry<K, V> e = oldTable[i]; e != null; ) {
                    AndroidHashMapEntry<K, V> oldNext = e.next;
                    int newIndex = e.hash & newMask;
                    AndroidHashMapEntry<K, V> newNext = newTable[newIndex];
                    newTable[newIndex] = e;
                    e.next = newNext;
                    e = oldNext;
                }
            }
        }
    }

    /**
     * Allocate a table of the given capacity and set the threshold accordingly.
     *
     * @param newCapacity must be a power of two
     */
    private AndroidHashMapEntry<K, V>[] makeTable(int newCapacity) {
        @SuppressWarnings("unchecked") AndroidHashMapEntry<K, V>[] newTable
                = (AndroidHashMapEntry<K, V>[]) new AndroidHashMapEntry[newCapacity];
        table = newTable;
        threshold = (newCapacity >> 1) + (newCapacity >> 2); // 3/4 capacity
        return newTable;
    }

    /**
     * Doubles the capacity of the hash table. Existing entries are placed in
     * the correct bucket on the enlarged table. If the current capacity is,
     * MAXIMUM_CAPACITY, this method is a no-op. Returns the table, which
     * will be new unless we were already at MAXIMUM_CAPACITY.
     */
    private AndroidHashMapEntry<K, V>[] doubleCapacity() {
        AndroidHashMapEntry<K, V>[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            return oldTable;
        }
        int newCapacity = oldCapacity * 2;
        AndroidHashMapEntry<K, V>[] newTable = makeTable(newCapacity);
        if (size == 0) {
            return newTable;
        }

        for (int j = 0; j < oldCapacity; j++) {
            /*
             * Rehash the bucket using the minimum number of field writes.
             * This is the most subtle and delicate code in the class.
             */
            AndroidHashMapEntry<K, V> e = oldTable[j];
            if (e == null) {
                continue;
            }
            int highBit = e.hash & oldCapacity;
            AndroidHashMapEntry<K, V> broken = null;
            newTable[j | highBit] = e;
            for (AndroidHashMapEntry<K, V> n = e.next; n != null; e = n, n = n.next) {
                int nextHighBit = n.hash & oldCapacity;
                if (nextHighBit != highBit) {
                    if (broken == null)
                        newTable[j | nextHighBit] = n;
                    else
                        broken.next = n;
                    broken = e;
                    highBit = nextHighBit;
                }
            }
            if (broken != null)
                broken.next = null;
        }
        return newTable;
    }

    /**
     * Removes the mapping with the specified key from this map.
     *
     * @param key the key of the mapping to remove.
     * @return the value of the removed mapping or {@code null} if no mapping
     * for the specified key was found.
     */
    @Override
    public V remove(Object key) {
        if (key == null) {
            return removeNullKey();
        }
        int hash = secondaryHash(key);
        AndroidHashMapEntry<K, V>[] tab = table;
        int index = hash & (tab.length - 1);
        for (AndroidHashMapEntry<K, V> e = tab[index], prev = null;
             e != null; prev = e, e = e.next) {
            if (e.hash == hash && key.equals(e.key)) {
                if (prev == null) {
                    tab[index] = e.next;
                } else {
                    prev.next = e.next;
                }
                modCount++;
                size--;
                postRemove(e);
                return e.value;
            }
        }
        return null;
    }

    private V removeNullKey() {
        AndroidHashMapEntry<K, V> e = entryForNullKey;
        if (e == null) {
            return null;
        }
        entryForNullKey = null;
        modCount++;
        size--;
        postRemove(e);
        return e.value;
    }

    /**
     * Subclass overrides this method to unlink entry.
     */
    void postRemove(AndroidHashMapEntry<K, V> e) {
    }

    /**
     * Removes all mappings from this hash map, leaving it empty.
     *
     * @see #isEmpty
     * @see #size
     */
    @Override
    public void clear() {
        if (size != 0) {
            Arrays.fill(table, null);
            entryForNullKey = null;
            modCount++;
            size = 0;
        }
    }

    /**
     * Returns a set of the keys contained in this map. The set is backed by
     * this map so changes to one are reflected by the other. The set does not
     * support adding.
     *
     * @return a set of the keys.
     */
    @Override
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet());
    }

    /**
     * Returns a collection of the values contained in this map. The collection
     * is backed by this map so changes to one are reflected by the other. The
     * collection supports remove, removeAll, retainAll and clear operations,
     * and it does not support add or addAll operations.
     * <p>
     * This method returns a collection which is the subclass of
     * AbstractCollection. The iterator method of this subclass returns a
     * "wrapper object" over the iterator of map's entrySet(). The {@code size}
     * method wraps the map's size method and the {@code contains} method wraps
     * the map's containsValue method.
     * </p>
     * <p>
     * The collection is created when this method is called for the first time
     * and returned in response to all subsequent calls. This method may return
     * different collections when multiple concurrent calls occur, since no
     * synchronization is performed.
     * </p>
     *
     * @return a collection of the values contained in this map.
     */
    @Override
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null) ? vs : (values = new Values());
    }

    /**
     * Returns a set containing all of the mappings in this map. Each mapping is
     * an instance of {@link Map.Entry}. As the set is backed by this map,
     * changes in one will be reflected in the other.
     *
     * @return a set of the mappings.
     */
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    static class AndroidHashMapEntry<K, V> implements Entry<K, V> {

        final K key;
        V value;
        final int hash;
        AndroidHashMapEntry<K, V> next;

        AndroidHashMapEntry(K key, V value, int hash, AndroidHashMapEntry<K, V> next) {
            this.key = key;
            this.value = value;
            this.hash = hash;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry<?, ?>) o;
            return Objects.equal(e.getKey(), key)
                   && Objects.equal(e.getValue(), value);
        }

        @Override
        public final int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^
                   (value == null ? 0 : value.hashCode());
        }

        @Override
        public final String toString() {
            return key + "=" + value;
        }
    }

    private abstract class HashIterator {

        int nextIndex;
        AndroidHashMapEntry<K, V> nextEntry = entryForNullKey;
        AndroidHashMapEntry<K, V> lastEntryReturned;
        int expectedModCount = modCount;

        HashIterator() {
            if (nextEntry == null) {
                AndroidHashMapEntry<K, V>[] tab = table;
                AndroidHashMapEntry<K, V> next = null;
                while (next == null && nextIndex < tab.length) {
                    next = tab[nextIndex++];
                }
                nextEntry = next;
            }
        }

        public boolean hasNext() {
            return nextEntry != null;
        }

        AndroidHashMapEntry<K, V> nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextEntry == null)
                throw new NoSuchElementException();

            AndroidHashMapEntry<K, V> entryToReturn = nextEntry;
            AndroidHashMapEntry<K, V>[] tab = table;
            AndroidHashMapEntry<K, V> next = entryToReturn.next;
            while (next == null && nextIndex < tab.length) {
                next = tab[nextIndex++];
            }
            nextEntry = next;
            return lastEntryReturned = entryToReturn;
        }

        public void remove() {
            if (lastEntryReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            AndroidHashMap.this.remove(lastEntryReturned.key);
            lastEntryReturned = null;
            expectedModCount = modCount;
        }
    }

    private final class KeyIterator extends HashIterator
            implements Iterator<K> {

        public K next() {
            return nextEntry().key;
        }
    }

    private final class ValueIterator extends HashIterator
            implements Iterator<V> {

        public V next() {
            return nextEntry().value;
        }
    }

    private final class EntryIterator extends HashIterator
            implements Iterator<Entry<K, V>> {

        public Entry<K, V> next() {
            return nextEntry();
        }
    }

    /**
     * Returns true if this map contains the specified mapping.
     */
    private boolean containsMapping(Object key, Object value) {
        if (key == null) {
            AndroidHashMapEntry<K, V> e = entryForNullKey;
            return e != null && Objects.equal(value, e.value);
        }

        int hash = secondaryHash(key);
        AndroidHashMapEntry<K, V>[] tab = table;
        int index = hash & (tab.length - 1);
        for (AndroidHashMapEntry<K, V> e = tab[index]; e != null; e = e.next) {
            if (e.hash == hash && key.equals(e.key)) {
                return Objects.equal(value, e.value);
            }
        }
        return false; // No entry for key
    }

    /**
     * Removes the mapping from key to value and returns true if this mapping
     * exists; otherwise, returns does nothing and returns false.
     */
    private boolean removeMapping(Object key, Object value) {
        if (key == null) {
            AndroidHashMapEntry<K, V> e = entryForNullKey;
            if (e == null || !Objects.equal(value, e.value)) {
                return false;
            }
            entryForNullKey = null;
            modCount++;
            size--;
            postRemove(e);
            return true;
        }

        int hash = secondaryHash(key);
        AndroidHashMapEntry<K, V>[] tab = table;
        int index = hash & (tab.length - 1);
        for (AndroidHashMapEntry<K, V> e = tab[index], prev = null;
             e != null; prev = e, e = e.next) {
            if (e.hash == hash && key.equals(e.key)) {
                if (!Objects.equal(value, e.value)) {
                    return false;  // Map has wrong value for key
                }
                if (prev == null) {
                    tab[index] = e.next;
                } else {
                    prev.next = e.next;
                }
                modCount++;
                size--;
                postRemove(e);
                return true;
            }
        }
        return false; // No entry for key
    }

    // Subclass (LinkedHashMap) overrides these for correct iteration order
    Iterator<K> newKeyIterator() {
        return new KeyIterator();
    }

    Iterator<V> newValueIterator() {
        return new ValueIterator();
    }

    Iterator<Entry<K, V>> newEntryIterator() {
        return new EntryIterator();
    }

    private final class KeySet extends AbstractSet<K> {

        public Iterator<K> iterator() {
            return newKeyIterator();
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object o) {
            int oldSize = size;
            AndroidHashMap.this.remove(o);
            return size != oldSize;
        }

        public void clear() {
            AndroidHashMap.this.clear();
        }
    }

    private final class Values extends AbstractCollection<V> {

        public Iterator<V> iterator() {
            return newValueIterator();
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public boolean contains(Object o) {
            return containsValue(o);
        }

        public void clear() {
            AndroidHashMap.this.clear();
        }
    }

    private final class EntrySet extends AbstractSet<Entry<K, V>> {

        public Iterator<Entry<K, V>> iterator() {
            return newEntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<?, ?> e = (Entry<?, ?>) o;
            return containsMapping(e.getKey(), e.getValue());
        }

        public boolean remove(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<?, ?> e = (Entry<?, ?>) o;
            return removeMapping(e.getKey(), e.getValue());
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public void clear() {
            AndroidHashMap.this.clear();
        }
    }

    private static final long serialVersionUID = 362498820763181265L;

    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("loadFactor", float.class)
    };

    private void writeObject(ObjectOutputStream stream) throws IOException {
        // Emulate loadFactor field for other implementations to read
        ObjectOutputStream.PutField fields = stream.putFields();
        fields.put("loadFactor", DEFAULT_LOAD_FACTOR);
        stream.writeFields();

        stream.writeInt(table.length); // Capacity
        stream.writeInt(size);
        for (Entry<K, V> e : entrySet()) {
            stream.writeObject(e.getKey());
            stream.writeObject(e.getValue());
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException,
                                                             ClassNotFoundException {
        stream.defaultReadObject();
        int capacity = stream.readInt();
        if (capacity < 0) {
            throw new InvalidObjectException("Capacity: " + capacity);
        }
        if (capacity < MINIMUM_CAPACITY) {
            capacity = MINIMUM_CAPACITY;
        } else if (capacity > MAXIMUM_CAPACITY) {
            capacity = MAXIMUM_CAPACITY;
        } else {
            capacity = roundUpToPowerOfTwo(capacity);
        }
        makeTable(capacity);

        int size = stream.readInt();
        if (size < 0) {
            throw new InvalidObjectException("Size: " + size);
        }

        init(); // Give subclass (LinkedHashMap) a chance to initialize itself
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked") K key = (K) stream.readObject();
            @SuppressWarnings("unchecked") V val = (V) stream.readObject();
            constructorPut(key, val);
        }
    }

    /**
     * Computes a hash code and applies a supplemental hash function to defend
     * against poor quality hash functions. This is critical because HashMap
     * uses power-of-two length hash tables, that otherwise encounter collisions
     * for hash codes that do not differ in lower or upper bits.
     * Routine taken from java.util.concurrent.ConcurrentHashMap.hash(int).
     *
     * @hide
     */
    public static int secondaryHash(Object key) {
        return secondaryHash(key.hashCode());
    }

    /**
     * Computes an identity hash code and applies a supplemental hash function to defend
     * against poor quality hash functions. This is critical because identity hash codes
     * are currently implemented as object addresses, which will have been aligned by the
     * underlying memory allocator causing all hash codes to have the same bottom bits.
     *
     * @hide
     */
    public static int secondaryIdentityHash(Object key) {
        return secondaryHash(System.identityHashCode(key));
    }

    private static int secondaryHash(int h) {
        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += (h << 15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h << 3);
        h ^= (h >>> 6);
        h += (h << 2) + (h << 14);
        return h ^ (h >>> 16);
    }

    /**
     * Returns the smallest power of two >= its argument, with several caveats:
     * If the argument is negative but not Integer.MIN_VALUE, the method returns
     * zero. If the argument is > 2^30 or equal to Integer.MIN_VALUE, the method
     * returns Integer.MIN_VALUE. If the argument is zero, the method returns
     * zero.
     *
     * @hide
     */
    public static int roundUpToPowerOfTwo(int i) {
        i--; // If input is a power of two, shift its high-order bit right.

        // "Smear" the high-order bit all the way to the right.
        i |= i >>> 1;
        i |= i >>> 2;
        i |= i >>> 4;
        i |= i >>> 8;
        i |= i >>> 16;

        return i + 1;
    }

}
