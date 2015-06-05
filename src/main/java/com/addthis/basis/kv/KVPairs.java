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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.addthis.basis.util.LessBytes;
import com.addthis.basis.util.LessNumbers;
import com.addthis.basis.util.LessStrings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that represents a collection of key/value pairs.
 *
 * What was going on when this class was implemented?
 * @deprecated Consider using the
 * <a href="http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/collect/Multimap.html">Multimap</a>
 * interface.
 */
@Deprecated
public class KVPairs implements Iterable<KVPair> {
    protected static final Logger log = LoggerFactory.getLogger(KVPairs.class);
    private static final boolean putupper = System.getProperty("kv.putupper", "1").equals("1");
    private static final boolean getupper = System.getProperty("kv.getupper", "1").equals("1");

    public static KVPairs mergeAndCopy(KVPairs base, KVPairs overrider) {
        if (overrider == null) {
            return base;
        } else if (base == null) {
            return overrider;
        }
        KVPairs ret = overrider.getCopy();
        for (KVPair kv : base) {
            if (!ret.hasKey(kv.getKey())) {
                ret.addValue(kv.getKey(), kv.getValue());
            }
        }
        return ret;
    }

    public static KVPairs fromFullURL(String url) {
        KVPairs kvp = new KVPairs();
        kvp.addValue("_URLROOT_", kvp.parseURLPath(url, true));
        return kvp;
    }


    /*
     * Remove pairs with empty/null values
     */
    public static KVPairs clearEmpties(KVPairs values) {

        for (Iterator<KVPair> iter = values.elements(); iter.hasNext();) {
            KVPair kv = iter.next();

            if (LessStrings.isEmpty(kv.getValue())) {
                iter.remove();
            }
        }

        return values;
    }


    private String fixGetCase(String key) {
        return getupper ? key.toUpperCase() : key;
    }

    private HashMap<String, KVPair> pairs;

    /**
     * Constructs an empty KVPairs object.
     */
    public KVPairs() {
        pairs = new LinkedHashMap<>();
    }

    protected KVPairs(KVPairs kv) {
        this.pairs = kv.pairs;
    }

    /**
     * Constructs a KVPairs object from an urlencoded string.
     *
     * @param urlpath
     */
    public KVPairs(String urlpath) {
        this();
        if (urlpath != null) {
            parseURLPath(urlpath, false);
        }
    }

    public KVPairs(byte kbin[]) {
        this();
        fromBinArray(kbin);
    }


    /**
     * Returns a shallow clone of this KVPairs.
     */
    @SuppressWarnings("unchecked")
    public KVPairs getCopy() {
        KVPairs kv = new KVPairs(this);
        try {
            kv.pairs = (HashMap<String, KVPair>) pairs.clone();
        } catch (Exception ex) {
            log.trace("getCopy", ex);
        }
        return kv;
    }

    /**
     * Returns a more human readable (but not always decodable copy) for debugging.
     */
    public String getPrintable() {
        return getPrintable(false);
    }

    public String getPrintable(boolean newline) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<KVPair> i = pairs.values().iterator(); i.hasNext();) {
            KVPair kv = i.next();
            sb.append(kv.getKey() + "='" + kv.getValue() + "'");
            if (i.hasNext()) {
                sb.append(newline ? '\n' : ' ');
            }
        }
        return sb.toString();
    }

    private void parsePair(String s) {
        KVPair kv = KVPair.parsePair(s);
        if (kv != null) {
            addPair(kv);
        }
    }

    /**
     * A faster tokenizer than StringTokenizer (about 2x)
     */
    private void parsePairs(String s, char del) {
        if (s.length() == 0) {
            return;
        }
        int i = 0;
        int j = s.indexOf(del);
        while (j >= 0) {
            if (j > 0) {
                parsePair(s.substring(i, j));
            }
            i = j + 1;
            j = s.indexOf(del, i);
        }
        parsePair(s.substring(i));
    }

    /**
     * Parses a urlencoded string of key/value pairs and adds the values
     * to this KVPairs object.
     *
     * @param urlPath
     * @param lookForQ
     * @return
     */
    public String parseURLPath(String urlPath, boolean lookForQ) {
        String filePath = urlPath;
        if (lookForQ) {
            int idx = urlPath.indexOf('?');
            if (idx < 0 || idx == urlPath.length()) {
                return urlPath;
            }
            filePath = urlPath.substring(0, idx);
            urlPath = urlPath.substring(idx + 1);
        }
        parsePairs(urlPath, '&');
        return filePath;
    }

    public KVPairs prefix(String prefix) {
        HashMap<String, KVPair> oldPairs = pairs;
        pairs = new LinkedHashMap<>();
        for (Map.Entry<String, KVPair> mapEntry : oldPairs.entrySet()) {
            KVPair kvpair = mapEntry.getValue();
            kvpair.setKey(prefix + kvpair.getKey());
            addPair(kvpair);
        }

        return this;
    }


    /**
     * Merges values from provided KVPairs overriding any values
     * with the same keys in this KVPairs.
     *
     * @param add
     */
    public synchronized KVPairs merge(KVPairs add) {
        if (add != null) {
            pairs.putAll(add.pairs);
        }
        return this;
    }

    /**
     * merge which is empty-value-aware- if there is an empty value in the target, override it with a
     * non-empty value form the source
     */
    public KVPairs mergeNotEmpty(KVPairs add) {
        if (add != null) {
            for (String key : add.pairs.keySet()) {
                KVPair newVal = add.getPair(key);
                KVPair thisVal = getPair(key);
                if ((newVal != null) && (!LessStrings.isEmpty(newVal.getValue()))) {
                    if ((thisVal == null) || (LessStrings.isEmpty(thisVal.getValue()))) {
                        addPair(newVal);
                    }
                }
            }
        }
        return this;
    }


    public KVPairs add(String key, String val) {
        addValue(key, val);
        return this;
    }

    public KVPairs add(String key, int val) {
        return add(key, Integer.toString(val));
    }

    private static KVPair createPair(String key, String value) {
        return new KVPair(key, value);
    }

    /**
     * Adds a key/value pair.
     *
     * @param key
     * @param val
     * @return
     */
    public KVPair addValue(String key, String val) {
        return addPair(createPair(key, val));
    }

    public KVPair addValue(String key, int value) {
        return addPair(createPair(key, Integer.toString(value)));
    }

    public KVPair addValue(String key, long value) {
        return addPair(createPair(key, Long.toString(value)));
    }

    public KVPair addValue(String key, float value) {
        return addPair(createPair(key, Float.toString(value)));
    }

    public KVPair addValue(String key, double value) {
        return addPair(createPair(key, Double.toString(value)));
    }

    public KAPair addValue(String key, String... values) {
        KAPair pair = new KAPair(key, values);
        addPair(pair);
        return pair;
    }

    /**
     * putValue() synonym for addValue()
     */
    public KVPair putValue(String key, String val) {
        return addValue(key, val);
    }

    public KVPair putValue(String key, int value) {
        return addValue(key, value);
    }

    public KVPair putValue(String key, long value) {
        return addValue(key, value);
    }

    public KVPair putValue(String key, float value) {
        return addValue(key, value);
    }

    public KVPair putValue(String key, double value) {
        return addValue(key, value);
    }

    public KAPair putValue(String key, String... values) {
        return addValue(key, values);
    }

    /**
     * Adds a key/value pair from a KVPair object.
     *
     * @param pair
     * @return
     */
    public final synchronized KVPair addPair(KVPair pair) {
        String key = pair.getKey();
        return pairs.put(putupper ? key.toUpperCase() : key, pair);
    }

    public KVPair putPair(KVPair pair) {
        return addPair(pair);
    }

    /**
     * Replaces the value for an existing key and adds a new key/value pair
     * for a missing key.  Preserves original key and it's case.
     */
    public final void replaceOrAdd(String key, String value) {
        KVPair pair = getPair(key);
        if (pair != null) {
            pair.setValue(value);
        } else {
            addValue(key, value);
        }
    }

    /**
     * Replaces the value for an existing key and adds a new key/value pair
     * for a missing key.  Preserves original key and it's case.
     */
    public final void replaceOrAdd(String key, long value) {
        KVPair pair = getPair(key);
        if (pair != null) {
            pair.setValue(Long.toString(value));
        } else {
            addValue(key, value);
        }
    }

    /**
     * Replaces the value for an existing key and adds a new key/value pair
     * for a missing key.  Preserves original key and it's case.
     */
    public final void replaceOrAdd(String key, int value) {
        KVPair pair = getPair(key);
        if (pair != null) {
            pair.setValue(Integer.toString(value));
        } else {
            addValue(key, value);
        }
    }

    /**
     * Changes the key associated with a value.  Does nothing if the
     * key does not exist.
     *
     * @param okey
     * @param nkey
     * @return true if the key exists, false otherwise
     */
    public final boolean renameValue(String okey, String nkey) {
        KVPair p = pairs.remove(fixGetCase(okey));
        if (p != null) {
            p.setKey(nkey);
            addPair(p);
            return true;
        }
        return false;
    }

    /**
     * Remove the key and value for a given key.
     */
    public final KVPair removePair(String key) {
        return pairs.remove(fixGetCase(key));
    }

    /**
     * Returns a string value or null of the key is absent.
     * Removes any matching key and data.
     */
    public String takeValue(String key) {
        KVPair p = removePair(key);
        return (p != null ? p.getValue() : null);
    }

    /**
     * Returns a string value or default of the key is absent.
     * Removes any matching key and data.
     */
    public String takeValue(String key, String def) {
        KVPair p = removePair(key);
        return (p != null ? p.getValue() : def);
    }

    /**
     * Integer takeValue() with base10.
     * Removes any matching key and data.
     */
    public int takeValue(String key, int def) {
        return takeValue(key, def, 10);
    }

    /**
     * Returns an integer value using the specified base or default
     * if the key is absent or value malformed.
     * Removes any matching key and data.
     */
    public int takeValue(String key, int def, int base) {
        String val = takeValue(key);
        return (val != null ? LessNumbers.parseInt(val, def, base) : def);
    }

    /**
     * Returns an integer value using the specified base or default
     * if the key is absent or value malformed.
     * Removes any matching key and data.
     */
    public float takeValue(String key, float def) {
        String val = takeValue(key);
        return (val != null ? LessNumbers.parseFloat(val, def) : def);
    }

    /**
     * Long takeValue() with base10.
     * Removes any matching key and data.
     */
    public long takeValue(String key, long def) {
        return takeValue(key, def, 10);
    }

    /**
     * Returns a long value using the specified base or default
     * if the key is absent or value malformed.
     * Removes any matching key and data.
     */
    public long takeValue(String key, long def, int base) {
        String val = takeValue(key);
        return (val != null ? LessNumbers.parseLong(val, def, base) : def);
    }

    /**
     * Return KVPair wrapper object for a given key.
     *
     * @param key
     * @return KVPair object or null if no key matches.
     */
    public final synchronized KVPair getPair(String key) {
        return pairs.get(fixGetCase(key));
    }

    /**
     * Returns true if a given key exists.
     *
     * @param key
     * @return true if key exists
     */
    public final boolean hasKey(String key) {
        if (key == null) {
            return false;
        }
        return pairs.containsKey(fixGetCase(key));
    }

    /**
     * @return true if contains a pair with same key and value as given pair (not an object comparison)
     */
    public final boolean hasPair(KVPair pair) {
        return pair != null && pair.equals(getPair(pair.getKey()));
    }

    /**
     * Return value for a given key or null if key does not exist.
     *
     * @param key
     * @return value or null if key does not exist.
     */
    public String getValue(String key) {
        return getValue(key, null);
    }

    /**
     * Return value for a given key or default if key does not exist.
     *
     * @param key
     * @param def default value to return if key does not exist
     * @return value or default if key does not exist.
     */
    public String getValue(String key, String def) {
        KVPair pair = getPair(key);
        return (pair != null ? pair.getValue() : def);
    }

    /**
     * Return value for a given key or default if key does not exist.
     *
     * @param key
     * @param def default value to return if key does not exist
     * @return value or default if key does not exist.
     */
    public int getIntValue(String key, int def) {
        return getIntValue(10, key, def);
    }

    /**
     * Return value for a given key or default if key does not exist.
     *
     * @param radix base for converting string to number
     * @param key
     * @param def   default value to return if key does not exist
     * @return value or default if key does not exist.
     */
    public int getIntValue(int radix, String key, int def) {
        KVPair pair = getPair(key);
        return pair != null ? LessNumbers.parseInt(pair.getValue(), def, radix) : def;
    }

    /**
     * Return value for a given key or default if key does not exist.
     *
     * @param key
     * @param def default value to return if key does not exist
     * @return value or default if key does not exist.
     */
    public long getLongValue(String key, long def) {
        return getLongValue(10, key, def);
    }

    /**
     * Return value for a given key or default if key does not exist.
     *
     * @param radix base for converting string to number
     * @param key
     * @param def   default value to return if key does not exist
     * @return value or default if key does not exist.
     */
    public long getLongValue(int radix, String key, long def) {
        KVPair pair = getPair(key);
        return pair != null ? LessNumbers.parseLong(pair.getValue(), def, radix) : def;
    }

    public float getFloatValue(String key, float def) {
        String val = getValue(key);
        return val != null ? LessNumbers.parseFloat(val, def) : def;
    }

    public double getDoubleValue(String key, double def) {
        String val = getValue(key);
        return val != null ? LessNumbers.parseDouble(val, def) : def;
    }

    /**
     * Changes the value for a given key.  Does not add key/value.
     *
     * @param key
     * @param value
     * @return previous value or null if the key doesn't exist
     */
    public final String setValue(String key, String value) {
        String oval = null;
        KVPair p = getPair(key);
        if (p != null) {
            oval = p.getValue();
            p.setValue(value);
        }
        return oval;
    }

    /**
     * @return number of KVPair objects represented.
     */
    public final int count() {
        return pairs.size();
    }

    /**
     * Returns an Iterator of key Strings.
     *
     * @return an Iterator of key Strings
     */
    public final Iterator<String> keys() {
        return pairs.keySet().iterator();
    }

    /**
     * Returns an Iterator of key Strings.
     *
     * @return an Iterator of key Strings
     */
    public final Set<String> keySet() {
        return pairs.keySet();
    }

    public final Collection<KVPair> values() {
        return pairs.values();
    }

    public Iterator<KVPair> iterator() {
        return elements();
    }

    /**
     * Returns an Iterator of KVPair elements.
     *
     * @return an Iterator of KVPair elements
     */
    public final Iterator<KVPair> elements() {
        return pairs.values().iterator();
    }


    public String toURLParams() {
        return toString();
    }

    public void fromURLParams(String str) {
        parseURLPath(str, false);
    }

    public byte[] toBinArray() {
        int size = 0;
        ArrayList<byte[]> arr = new ArrayList<>(count() * 2);
        for (KVPair kv : values()) {
            String v = kv.getValue();
            byte key[] = LessBytes.toBytes(kv.getKey());
            byte val[] = v != null ? LessBytes.toBytes(kv.getValue()) : new byte[0];
            size += key.length + val.length;
            arr.add(key);
            arr.add(val);
        }
        byte out[] = new byte[size + arr.size()];
        size = 0;
        for (byte el[] : arr) {
            for (int i = 0; i < el.length; i++) {
                out[size + i] = el[i] != 0 ? el[i] : (byte) ' ';
            }
            size += el.length + 1;
            out[size - 1] = 0;
        }
        return out;
    }

    public KVPairs fromBinArray(byte bin[]) {
        int pos = 0;
        while (pos < bin.length) {
            String key = null;
            String val = null;
            for (int i = pos; i < bin.length; i++) {
                if (bin[i] == 0) {
                    key = new String(bin, pos, i - pos);
                    pos = i + 1;
                    break;
                }
                if (i == bin.length - 1) {
                    key = new String(bin, pos, i - pos + 1);
                    pos = i + 1;
                    break;
                }
            }
            for (int i = pos; i < bin.length; i++) {
                if (bin[i] == 0) {
                    val = new String(bin, pos, i - pos);
                    pos = i + 1;
                    break;
                }
                if (i == bin.length - 1) {
                    val = new String(bin, pos, i - pos + 1);
                    pos = i + 1;
                    break;
                }
            }
            addValue(key, val);
        }
        return this;
    }

    /**
     * Returns a urlencoded string comprised of all the "name"=value&"name"=value pairs.
     */
    public synchronized String toQuotedKeyString() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<KVPair> i = pairs.values().iterator(); i.hasNext();) {
            sb.append(i.next().toQuotedKeyString());
            if (i.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    /**
     * Returns a urlencoded string comprised of all the name=value&name=value pairs.
     */
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<KVPair> i = pairs.values().iterator(); i.hasNext();) {
            sb.append(i.next().toString());
            if (i.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    /**
     * basic equals function, fast-fail, can be optimized for speed by iterating through keyset only once
     * TODO: override hashcode() method as recommended by java api
     *
     * @return true if the other KVPairs set has the equal keys and values
     */
    public synchronized boolean equals(KVPairs other) {
        if (!keySet().equals(other.keySet())) {
            return false;
        }  // compare key sets (tests for extra/missing pairs in other set)
        for (String key : keySet()) {
            if (!getPair(key).equals(other.getPair(key))) {
                return false;
            }  // compare pairs
        }
        return true;
    }

    /**
     * multi-pair convenience constructors
     */
    public KVPairs(String ka, String va) {
        this();
        addValue(ka, va);
    }

    public KVPairs(String ka, String va, String kb, String vb) {
        this();
        addValue(ka, va);
        addValue(kb, vb);
    }

    public KVPairs(String ka, String va, String kb, String vb, String kc, String vc) {
        this();
        addValue(ka, va);
        addValue(kb, vb);
        addValue(kc, vc);
    }

    public KVPairs(String ka, String va, String kb, String vb, String kc, String vc, String kd, String vd) {
        this();
        addValue(ka, va);
        addValue(kb, vb);
        addValue(kc, vc);
        addValue(kd, vd);
    }
}
