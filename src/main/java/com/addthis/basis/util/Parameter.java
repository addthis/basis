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


public class Parameter {
    public static String value(String key) {
        return System.getProperty(key);
    }

    public static String value(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    public static boolean boolValue(String key, boolean defaultValue) {
        if (value(key) == null) {
            return defaultValue;
        }
        return intValue(key, 0) > 0 || value(key, "").equalsIgnoreCase("true");
    }

    public static int intValue(String key, int defaultValue) {
        return (int) longValue(key, defaultValue);
    }

    public static long longValue(String key, long defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Numbers.parseHumanReadable(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

}
