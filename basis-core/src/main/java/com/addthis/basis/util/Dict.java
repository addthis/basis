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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * String-String map wrapper, useful for url and kv primitive applications
 * <p/>
 * TODO finish url class cutover
 * TODO multi-value params, put(key, val ...) method
 */

public class Dict {

    private Map<String, String> map;

    /**
     * create a new Dict backed by a LinkedHashMap
     */
    public Dict() {
        map = new LinkedHashMap<>();
    }

    /**
     * create a new Dict backed by the specified map
     *
     * @param map
     */
    public Dict(Map<String, String> map) {
        assert (map != null);
        this.map = map;
    }

    /**
     * create a new Dict and read parameters from the given url query string
     *
     * @param query
     */
    public Dict(String query) {
        map = DictUtil.parse(query);
    }

    /**
     * create a new Dict, initialized with the given key and value
     *
     * @param key
     * @param value
     */
    public Dict(String key, String value) {
        this();
        put(key, value);
    }

    public String put(String key, String value) {
        return map.put(key, value);
    }

    public String put(String key, int value) {
        return map.put(key, String.valueOf(value));
    }

    public String put(String key, long value) {
        return map.put(key, String.valueOf(value));
    }

    public String put(String key, double value) {
        return map.put(key, String.valueOf(value));
    }

    public String put(String key, float value) {
        return map.put(key, String.valueOf(value));
    }

    public String put(String key, boolean value) {
        return map.put(key, String.valueOf(value));
    }

    public <T extends Enum<T>> String put(String key, T value) {
        return map.put(key, String.valueOf(value));
    }

    public <T extends Enum<T>> String put(T value) {
        return value == null ? null : map.put(value.getClass().getSimpleName().toLowerCase(), String.valueOf(value));
    }

    public boolean has(String key) {
        return map.containsKey(key);
    }

    public String get(String key) {
        return map.get(key);
    }

    public String get(String key, String dflt) {
        return has(key) ? map.get(key) : dflt;
    }

    public int getInt(String key, int dflt) {
        try {
            return has(key) ? Integer.parseInt(map.get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public long getLong(String key, long dflt) {
        try {
            return has(key) ? Long.parseLong(map.get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public float getFloat(String key, float dflt) {
        try {
            return has(key) ? Float.parseFloat(map.get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public double getDouble(String key, double dflt) {
        try {
            return has(key) ? Double.parseDouble(map.get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    public boolean getBoolean(String key, boolean dflt) {
        try {
            return has(key) ? Boolean.parseBoolean(map.get(key)) : dflt;
        } catch (NumberFormatException e) {
            return dflt;
        }
    }

    /**
     * serialize to url query format key1=value1&key2=value2
     */
    public String toString() {
        return DictUtil.toString(map);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Dict)) {
            return false;
        }
        Dict that = (Dict) obj;
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
     * return the set of keys stored in this collection
     *
     * @return
     */
    public Set<String> keySet() {
        return map.keySet();
    }

    /**
     * Return this Multidict as a Dict.
     *
     * @return
     */
    public Multidict asMultidict() {
        return DictUtil.asMultidict(this);
    }
}
