package com.addthis.basis.util;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;


public class AppendLinkedHashMap<K, V>
        extends AndroidHashMap<K, V>
        implements Map<K, V>
{

    //Fake start/placeholder entry; never a real entry
    private AppendLinkedHashMap.Entry<K, V> head;
    //Last entry; updated to a real entry when size > 0
    private AppendLinkedHashMap.Entry<K, V> tail;

    /**
     * Set up list
     */
    @Override
    void init()
    {
        head = new Entry<>(-1, null, null, null);
        tail = head;
    }

    @Override
    void transferAndroidHashMap.Entry[] newTable, boolean rehash)
    {
        int newCapacity = newTable.length;
        for (Entry<K, V> e = head.ptr; e != null; e = e.ptr)
        {
            int index = indexFor(e.hash, newCapacity);
            e.next = newTable[index];
            newTable[index] = e;
        }
    }

    /**
     * Null just always returns false
     */
    @Override
    public boolean containsValue(Object value)
    {
        for (Entry e = head.ptr; e != null; e = e.ptr)
        {
            if (value.equals(e.value))
            {
                return true;
            }
        }
        return false;
    }

    /**
     */
    public V get(Object key)
    {
        if (key == null)
        {
            throw new UnsupportedOperationException("Querying for null keys is not supported");
        }
       AndroidHashMap.Entry<K, V> e = getEntry(key);
        if (e == null)
        {
            return null; //value not found
        }
        return e.value;
    }

    /**
     * We do NOT check to see if value already exists. I strongly suggest you do this yourself.
     *
     * If the key is null, we will add it to the list (for iterators) and increment the size
     * but it will not be available for lookup via the hashmap. We also allow an unlimited number
     * of null keys.
     */
    @Override
    public V put(K key, V value)
    {
        if (key == null)
        {
            AppendLinkedHashMap.Entry<K, V> e = new AppendLinkedHashMap.Entry<>(0, null, value, null);
            tail.ptr = e;
            tail = e;
            size++;
        }
        int hash = hash(key);
        int i = indexFor(hash, table.length);
        addEntry(hash, key, value, i);
        return null;
    }

    /**
     */
    void createEntry(int hash, K key, V value, int bucketIndex)
    {
       AndroidHashMap.HashMapEntry<K, V> old = table[bucketIndex];
        AppendLinkedHashMap.Entry<K, V> e = new AppendLinkedHashMap.Entry<>(hash, key, value, old);
        table[bucketIndex] = e;
        tail.ptr = e;
        tail = e;
        size++;
    }

    /**
     */
    public V remove(Object key)
    {
        throw new UnsupportedOperationException();
    }

    /**
     */
    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }

    /**
     */
    private static class Entry<K, V> extends AndroidHashMap.HashMapEntry<K, V>
    {
        AppendLinkedHashMap.Entry<K, V> ptr;

        Entry(int hash, K key, V value, AndroidHashMap.HashMapEntry<K, V> next)
        {
            super(key, value, hash, next);
        }
    }

    public Iterator<V> valuesIterator()
    {
        return new ValuesIterator();
    }

    private class ValuesIterator implements Iterator<V>
    {
        AppendLinkedHashMap.Entry<K, V> nextEntry = head.ptr;

        public boolean hasNext()
        {
            return nextEntry != null;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public V next()
        {
            if (nextEntry == null)
            {
                throw new NoSuchElementException();
            }
            AppendLinkedHashMap.Entry<K, V> thisEntry = nextEntry;
            nextEntry = thisEntry.ptr;
            return thisEntry.value;
        }
    }
}
