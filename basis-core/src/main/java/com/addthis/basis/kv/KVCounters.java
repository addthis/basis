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
package com.addthis.basis.kv;


/**
 * Simple class that extends KVPairs to add explicit counter functionality.
 * Provides a get/increment interface to counter for a set of keys
 * Behavior is undefined if KVPairs accessors are used to add non-integer key values
 */
public class KVCounters extends KVPairs {
    /**
     * increment the counter associated with key.  creates a new counter if key doesn't exist.
     *
     * @param key
     */
    public void increment(String key) {
        putValue(key, getIntValue(key, 0) + 1);
    }

    /**
     * returns the value of the counter associated with key (returns 0 if noexist)
     *
     * @param key
     * @return
     */
    public int get(String key) {
        return getIntValue(key, 0);
    }

    /**
     * Sets the value of the counter associated with key
     *
     * @param key
     * @param val
     */
    public void set(String key, int val) {
        putValue(key, val);
    }

    /**
     * returns true if a counter for key exists
     *
     * @param key
     * @return
     */
    public boolean has(String key) {
        // using getValue() instead of hasKey() because hasKey()
        // can return true if key exists but has no counter
        return (getValue(key) != null);
    }

    /**
     * clears the counter associated with key
     *
     * @param key
     * @return key value or zero if noexist
     */
    public int reset(String key) {
        return takeValue(key, 0);
    }
}

