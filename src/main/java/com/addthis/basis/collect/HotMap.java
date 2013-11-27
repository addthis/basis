/*
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
 */
package com.addthis.basis.collect;


import java.util.Iterator;
import java.util.Map;

/**
 * A wrapper for a Map  that keeps a linked list in order
 * of node access.  This allows aging out of elements with more
 * flexibility than LinkedHashMap which only updates node order
 * when a new node is inserted.
 * <p/>
 * This class is NOT thread safe.  Iterating over elements
 * while updating the map can result in unpredictable results
 * (such as infinite loops).
 */
public final class HotMap<K, V> implements Iterable<Map.Entry<K, V>> {

    @SuppressWarnings("unchecked")
    public HotMap(Map map) {
        this.map = map;
    }

    public String toString() {
        return map.toString();
    }

    protected MapEntry createMapEntry(K key, V val) {
        return new MapEntry(key, val);
    }

    protected final class MapEntry implements Map.Entry<K, V> {
        K key;
        V value;
        MapEntry prev;
        MapEntry next;

        MapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldval = this.value;
            this.value = value;
            return oldval;
        }

        public String toString() {
            return "[" + key + "," + value + "]";
        }
    }

    private String iteratorToString(Iterator<?> i) {
        StringBuilder sb = new StringBuilder("<");
        while (i.hasNext()) {
            sb.append(i.next().toString());
            if (i.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(">");
        return sb.toString();
    }

    /** */
    private final class MapIterator implements Iterator<Map.Entry<K, V>> {
        private MapIterator(Iterator<Map.Entry<K, MapEntry>> iterator) {
            this.iter = iterator;
        }

        Iterator<Map.Entry<K, MapEntry>> iter;
        MapEntry last;

        public String toString() {
            return iteratorToString(this);
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Map.Entry<K, V> next() {
            last = iter.next().getValue();
            return createMapEntry(last.key, last.value);
        }

        public void remove() {
            delink(last);
            iter.remove();
        }
    }

    /** */
    private final class UseMapIterator implements Iterator<Map.Entry<K, V>> {
        MapEntry current = firstEntry;
        MapEntry last = null;

        public String toString() {
            return /*firstEntry+":"+lastEntry+";"+*/iteratorToString(this);
        }

        public boolean hasNext() {
            return current != null;
        }

        public Map.Entry<K, V> next() {
            MapEntry ret = createMapEntry(current.key, current.value);
            last = current;
            current = current.next;
            if (current == last) {
                throw new RuntimeException("error. circular link in hot map use list");
            }
            return ret;
        }

        public void remove() {
            if (last != null) {
                delink(last);
            }
        }
    }

    private Map<K, MapEntry> map;
    private MapEntry firstEntry;
    private MapEntry lastEntry;

    public int size() {
        return map.size();
    }

    public void clear() {
        map.clear();
        firstEntry = null;
        lastEntry = null;
    }

    public Iterator<Map.Entry<K, V>> entriesFromMap() {
        return new MapIterator(map.entrySet().iterator());
    }

    public Iterator<Map.Entry<K, V>> entriesByUse() {
        return new UseMapIterator();
    }

    public Iterator<Map.Entry<K, V>> iterator() {
        return new UseMapIterator();
    }

    public V get(K key) {
        MapEntry e = map.get(key);
        if (e != null) {
            updateLinks(e);
            return e.value;
        } else {
            return null;
        }
    }

    public V peek(K key) {
        return map.get(key).value;
    }

    public Map.Entry<K, V> getEntry(K key) {
        MapEntry e = map.get(key);
        if (e != null) {
            updateLinks(e);
            return e;
        } else {
            return null;
        }
    }

    public V put(K key, V value) {
        MapEntry e = map.get(key);
        if (e != null) {
            updateLinks(e);
            return e.setValue(value);
        } else {
            e = createMapEntry(key, value);
            map.put(key, e);
            updateLinks(e);
            return null;
        }
    }

    public V remove(K key) {
        MapEntry e = map.remove(key);
        if (e != null) {
            delink(e);
            return e.value;
        }
        return null;
    }

    public V peekEldest() {
        return firstEntry != null ? firstEntry.value : null;
    }

    public Map.Entry<K, V> peekEldestEntry() {
        return firstEntry != null ? firstEntry : null;
    }

    public V removeEldest() {
        Map.Entry<K, V> e = removeEldestEntry();
        return e != null ? e.getValue() : null;
    }

    public Map.Entry<K, V> removeEldestEntry() {
        if (firstEntry != null) {
            MapEntry e = map.remove(firstEntry.key);
            delink(firstEntry);
            return e;
        }
        return null;
    }

    private void delink(MapEntry e) {
        if (e == firstEntry) {
            firstEntry = e.next;
        }
        if (e == lastEntry) {
            lastEntry = e.prev;
        }
        if (e.prev != null) {
            e.prev.next = e.next;
        }
        if (e.next != null) {
            e.next.prev = e.prev;
        }
        e.next = null;
        e.prev = null;
    }

    private void updateLinks(MapEntry e) {
        delink(e);
        if (firstEntry == null) {
            firstEntry = e;
        }
        if (lastEntry != null) {
            e.prev = lastEntry;
            lastEntry.next = e;
        }
        lastEntry = e;
    }

    public Map<K, MapEntry> getMap() {
        return this.map;
    }
}
