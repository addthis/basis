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

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;

/**
 * A utility class for accumulating counters against a key set.
 */
@SuppressWarnings("serial")
public class CountMap extends LinkedHashMap<String, Long> implements Comparator<String> {

    private long sum;
    private long max;
    private long min;

    public CountMap add(String str, long value) {
        Long val = get(str);
        Long nval = val != null ? val + value : value;
        max = Math.max(nval, max);
        min = (min == 0 ? value : Math.min(min, value));
        put(str, nval);
        sum += value;
        return this;
    }

    public void add(CountMap map) {
        for (String key : map.keySet()) {
            add(key, map.get(key));
        }
    }

    @Override
    public void clear() {
        sum = 0;
        max = 0;
        min = 0;
        super.clear();
    }

    public long getSum() {
        return sum;
    }

    public long getMax() {
        return max;
    }

    public long getMin() {
        return min;
    }

    public long getAverage() {
        return size() > 0 ? sum / size() : 0;
    }

    public String[] getSortedKeys() {
        String keys[] = keySet().toArray(new String[size()]);
        Arrays.sort(keys, this);
        return keys;
    }

    public int compare(String s1, String s2) {
        return (int) (this.get(s2) - this.get(s1));
    }

    public boolean equals(Object s1, Object s2) {
        return s1.equals(s2);
    }
}
