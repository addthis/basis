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

import java.util.Map;

public class MapHelper<K, V> {
    private Map<K, V> map;

    public MapHelper(Map<K, V> map) {
        this.map = map;

    }

    public MapHelper<K, V> add(Map<K, V> map) {
        this.map.putAll(map);
        return this;
    }

    public MapHelper<K, V> add(K k, V v) {
        if (v != null) {
            map.put(k, v);
        }
        return this;
    }

    public Map<K, V> map() {
        return map;
    }
}
