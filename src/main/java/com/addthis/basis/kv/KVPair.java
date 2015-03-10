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

import com.addthis.basis.util.LessBytes;
import com.addthis.basis.util.LessStrings;

/**
 * Class that represents a key/value pair.  Has helpers for parsing
 * pairs in common forms, including HTTP headers.  Keys are always
 * converted to uppercase.  Values are always urldecoded for storage.
 * Use toString() to regenerate the KVPair.
 */
public class KVPair {
    /**
     * Construct a KVPair wrapper from a string key and string value.
     *
     * @param key
     * @param val
     */
    public KVPair(String key, String val) {
        setKeyValue(key, val);
    }

    private String key;
    private String val;

    /**
     * Turn an URLencoded 'name=value' string into a KVPair
     *
     * @param str
     * @return
     */
    public static KVPair parsePair(String str) {
        int sep = str.indexOf('=');
        if (sep == 0 || str.length() == 0) {
            return null;
        }
        if (sep < 0) {
            return new KVPair(LessBytes.urldecode(str), "");
        }
        if (sep < str.length() - 1) {
            return new KVPair(LessBytes.urldecode(str.substring(0, sep)), LessBytes.urldecode(str.substring(sep + 1)));
        }
        return new KVPair(LessBytes.urldecode(str.substring(0, sep)), "");
    }

    /**
     * Turn an http header in the form "name: value" into a KVPair.
     *
     * @param str http header line
     * @return KVPair
     */
    public static KVPair parseHttpHeader(String str) {
        int idx = str.indexOf(':');
        if (idx == 0) {
            return null;
        }
        if (idx > 0) {
            if (idx < str.length() - 1) {
                int si = idx + 1;
                while (Character.isWhitespace(str.charAt(si)) && si < str.length() - 1) {
                    si++;
                }
                return new KVPair(str.substring(0, idx), str.substring(si));
            } else {
                return new KVPair(str.substring(0, idx), "");
            }
        } else {
            return new KVPair(str, "");
        }
    }

    /**
     * Set key and value at the same time.
     *
     * @param key
     * @param val
     */
    public void setKeyValue(String key, String val) {
        if (key == null) {
            new Exception("null key").printStackTrace();
        }
        setKey(key);
        setValue(val);
    }

    /**
     * @return key string
     */
    public String getKey() {
        return key;
    }

    /**
     * returns true if keys match ignoring case.
     */
    public boolean keyMatch(String k) {
        return key.equalsIgnoreCase(k);
    }

    /**
     * Set a new key.
     *
     * @param nk key string
     */
    public void setKey(String nk) {
        this.key = nk;
    }

    /**
     * @param nv new value in urlencoded UTF-8 format
     * @return previous value
     */
    public String setValue(String nv) {
        String oval = val;
        this.val = nv;
        return oval;
    }

    /**
     * Returns the urldecoded value.
     *
     * @return the urldecoded value
     */
    public String getValue() {
        return val;
    }

    /**
     * Returns a urlencoded "name"=value pair.
     */
    public String toQuotedKeyString() {
        if (val == null) {
            return LessStrings.cat("\"", LessBytes.urlencode(key), "\"=");
        } else {
            return LessStrings.cat("\"", LessBytes.urlencode(key), "\"=", LessBytes.urlencode(val));
        }
    }

    /**
     * Returns a urlencoded name=value pair.
     */
    public String toString() {
        if (val == null) {
            return LessStrings.cat(LessBytes.urlencode(key), "=");
        } else {
            return LessStrings.cat(LessBytes.urlencode(key), "=", LessBytes.urlencode(val));
        }
    }

    /**
     * basic equals function
     * TODO: override hashcode() method as recommended by java api
     *
     * @return true if other KVPair has equal key (ignore key case) and value
     */
    public boolean equals(KVPair other) {
        return other != null && getKey().equalsIgnoreCase(other.getKey()) && getValue().equals(other.getValue());
    }
}

