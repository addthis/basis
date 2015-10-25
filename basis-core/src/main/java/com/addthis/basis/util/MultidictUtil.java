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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.Iterator;
import java.util.Map;

public class MultidictUtil {

    /**
     * parse parameters from an url query string into a multidict
     *
     * @param query
     * @return
     */
    public static Multimap<String, String> parse(String query) {
        Multimap<String, String> map = LinkedHashMultimap.create();
        return parse(query, map);
    }

    /**
     * parse parameters from an url query string into the provided multidict
     *
     * @param query
     * @return
     */
    public static Multimap<String, String> parse(String query, Multimap<String, String> map) {
        assert (map != null);
        int i = 0;
        int j = query.indexOf("&");
        while (j >= 0) {
            if (j > 0) {
                parseParam(query.substring(i, j), map);
            }
            i = j + 1;
            j = query.indexOf("&", i);
        }
        parseParam(query.substring(i), map);
        return map;
    }

    /**
     * parse a parameter and add it to the map
     *
     * @param str parameter in key=value format
     * @param map map to which the parameter should be added
     * @return reference to the map for convenience
     */
    public static Multimap<String, String> parseParam(String str, Multimap<String, String> map) {
        if (LessStrings.isEmpty(str)) {
            return map;
        }
        int sep = str.indexOf('=');
        if (sep < 0) {  // no delimiter, put null value
            map.put(urlDecode(str), null);
        } else if (sep == 0) {  // no key, discard
        } else if (sep < str.length() - 1) {  // has
            map.put(urlDecode(str.substring(0, sep)), urlDecode(str.substring(sep + 1)));
        } else {  // empty value
            map.put(urlDecode(str.substring(0, sep)), "");
        }
        return map;
    }

    /**
     * serialize a string map to url query format
     *
     * @param map
     * @return map contents in key1=value1&key2=value2 format
     */
    public static String toString(Multimap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<Map.Entry<String, String>> i = map.entries().iterator(); i.hasNext();) {
            Map.Entry<String, String> entry = i.next();
            if (entry.getValue() == null) {
                sb.append(urlEncode(entry.getKey()));
            } else {
                sb.append(urlEncode(entry.getKey())).append("=").append(urlEncode(entry.getValue()));
            }
            if (i.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    /**
     * Flatten this Multidict into a dict.  Arbitrarily chooses values for keys that have more than one value.
     *
     * @return
     */
    public static Dict asDict(Multidict multidict) {
        Dict dict = new Dict();
        for (String key : multidict.keySet()) {
            dict.put(key, multidict.get(key));
        }
        return dict;
    }

    /**
     * URLEncoder helper, defaults to UTF-8 and eats encode exception. copied
     * code here to reduce dependencies on our other libraries.
     *
     * @param string
     * @return url encoded version of string
     */
    private static String urlEncode(String string) {
        try {
            return string == null ? string : URLEncoder.encode(string, "UTF-8");
        } catch (Exception e) {
            return string;
        }
    }

    /**
     * URLDecoder helper, defaults to UTF-8 and eats encode exception. copied
     * code here to reduce dependencies on our other libraries.
     *
     * @param string
     * @return url encoded version of string
     */
    private static String urlDecode(String string) {
        try {
            return string == null ? string : URLDecoder.decode(string, "UTF-8");
        } catch (Exception e) {
            return string;
        }
    }
}
