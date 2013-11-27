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
package com.addthis.basis.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A String-String Multimap wrapper, useful for url and kv primitive applications
 */

public class Multidict {
    private Multimap<String, String> map;

    /**
     * create a new Multidict backed by a new LinkedHashMultimap
     */
    public Multidict() {
        map = LinkedHashMultimap.create();
    }

    /**
     * create a new Multidict backed by the given Multimap
     *
     * @param map
     */
    public Multidict(Multimap<String, String> map) {
        assert (map != null);
        this.map = map;
    }

    public Multidict(Multidict copyme) {
        assert (copyme != null);
        this.map = LinkedHashMultimap.create(copyme.map);
    }

    /**
     * Create a new dict by parsing an url query string
     *
     * @param query
     */
    public Multidict(String query) {
        map = MultidictUtil.parse(query);
    }

    public Multidict(String key, String value) {
        this();
        map.put(key, value);
    }

    /**
     * add one or more mappings to the multidict
     *
     * @param key
     * @param values
     * @return
     */
    public boolean put(String key, String... values) {
        return map.putAll(key, Arrays.asList(values));
    }

    /**
     * add a mapping to the multidict
     *
     * @param key
     * @param value
     * @return
     */
    public boolean put(String key, int value) {
        return map.put(key, String.valueOf(value));
    }

    /**
     * add a mapping to the multidict
     *
     * @param key
     * @param value
     * @return
     */
    public boolean put(String key, long value) {
        return map.put(key, String.valueOf(value));
    }

    /**
     * add a mapping to the multidict
     *
     * @param key
     * @param value
     * @return
     */
    public boolean put(String key, double value) {
        return map.put(key, String.valueOf(value));
    }

    /**
     * add a mapping to the multidict
     *
     * @param key
     * @param value
     * @return
     */
    public boolean put(String key, float value) {
        return map.put(key, String.valueOf(value));
    }

    /**
     * add a mapping to the multidict
     *
     * @param key
     * @param value
     * @return
     */
    public boolean put(String key, boolean value) {
        return map.put(key, String.valueOf(value));
    }

    public <T extends Enum<T>> boolean put(String key, T value) {
        return map.put(key, String.valueOf(value));
    }

    public <T extends Enum<T>> boolean put(T value) {
        if (value == null) {
            return false;
        } else {
            return map.put(value.getClass().getSimpleName().toLowerCase(), String.valueOf(value));
        }
    }


    /**
     * copy all of the mappings from another multidict into this multidict
     *
     * @param multidict
     * @return
     */
    public boolean putAll(Multidict multidict) {
        return map.putAll(multidict.map);
    }

    /**
     * add a mapping to the multidict, removing any previous mappings
     *
     * @param key
     * @param value
     * @return
     */
    public boolean set(String key, String value) {
        boolean had = containsKey(key);
        if (containsKey(key)) {
            map.removeAll(key);
        }
        map.put(key, value);
        return had;
    }

    /**
     * add a mapping to the multidict, removing any previous mappings
     *
     * @param key
     * @param value
     * @return
     */
    public boolean set(String key, int value) {
        boolean had = containsKey(key);
        if (containsKey(key)) {
            map.removeAll(key);
        }
        map.put(key, String.valueOf(value));
        return had;
    }

    /**
     * add a mapping to the multidict, removing any previous mappings
     *
     * @param key
     * @param value
     * @return
     */
    public boolean set(String key, long value) {
        boolean had = containsKey(key);
        if (containsKey(key)) {
            map.removeAll(key);
        }
        map.put(key, String.valueOf(value));
        return had;
    }

    /**
     * add a mapping to the multidict, removing any previous mappings
     *
     * @param key
     * @param value
     * @return
     */
    public boolean set(String key, double value) {
        boolean had = containsKey(key);
        if (containsKey(key)) {
            map.removeAll(key);
        }
        map.put(key, String.valueOf(value));
        return had;
    }

    /**
     * add a mapping to the multidict, removing any previous mappings
     *
     * @param key
     * @param value
     * @return
     */
    public boolean set(String key, float value) {
        boolean had = containsKey(key);
        if (containsKey(key)) {
            map.removeAll(key);
        }
        map.put(key, String.valueOf(value));
        return had;
    }

    /**
     * add a mapping to the multidict, removing any previous mappings
     *
     * @param key
     * @param value
     * @return
     */
    public boolean set(String key, boolean value) {
        boolean had = containsKey(key);
        if (containsKey(key)) {
            map.removeAll(key);
        }
        map.put(key, String.valueOf(value));
        return had;
    }

    /**
     * test whether a key is present in the multimap
     *
     * @param key
     * @return
     */
    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    /**
     * remove all values associated with a given key
     *
     * @param key
     * @return
     */
    public Collection<String> removeAll(String key) {
        return map.removeAll(key);
    }

    /**
     * retrieve the first value mapped to the given key (if present). this is a
     * convenience method for casual use, getAll() is preferred for correctness.
     *
     * @param key
     * @return
     */
    public String get(String key) {
        return containsKey(key) ? map.get(key).iterator().next() : null;
    }

    /**
     * retrieve the first value mapped to the given key, or dflt if not present.
     * this is a convenience method for casual use, getAll() is preferred for
     * correctness.
     *
     * @param key
     * @return
     */
    public String get(String key, String dflt) {
        return containsKey(key) ? map.get(key).iterator().next() : dflt;
    }

    /**
     * retrieve all mappings for the given key.
     *
     * @param key
     * @return
     */
    public Collection<String> getAll(String key) {
        return map.get(key);
    }

    /**
     * retrieve all mappings for the given key, or dflt if not present
     *
     * @param key
     * @return
     */
    public Collection<String> getAll(String key, String dflt) {
        return containsKey(key) ? map.get(key) : Collections.singleton(dflt);
    }

    public int getInt(String key, int dflt) {
        try {
            return containsKey(key) ? Integer.parseInt(get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public long getLong(String key, long dflt) {
        try {
            return containsKey(key) ? Long.parseLong(get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public float getFloat(String key, float dflt) {
        try {
            return containsKey(key) ? Float.parseFloat(get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public double getDouble(String key, double dflt) {
        try {
            return containsKey(key) ? Double.parseDouble(get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public boolean getBoolean(String key, boolean dflt) {
        try {
            return containsKey(key) ? Boolean.parseBoolean(get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    /**
     * serialize to url query format key1=value1&key2=value2
     */
    public String toString() {
        return MultidictUtil.toString(map);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Multidict)) {
            return false;
        }
        Multidict that = (Multidict) obj;
        return map.equals(that.map);
    }

    public int hashCode() {
        return map.hashCode();
    }

    /**
     * @return number of keys stored in this dict
     */
    public int size() {
        return map.size();
    }

    /**
     * @return a collection of all entries stored in the underlying map
     */
    public Collection<Entry<String, String>> entries() {
        return map.entries();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    /**
     * Flatten this Multidict into a dict.  Arbitrarily chooses values for keys that have more than one value.
     *
     * @return
     */
    public Dict asDict() {
        return MultidictUtil.asDict(this);
    }
}
