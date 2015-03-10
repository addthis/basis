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

public class KAPair extends KVPair {

    private String key;
    private String[] vals;

    public KAPair(String key, String val) {
        super(key, val);
    }

    public KAPair(String key, String... vals) {
        super(key, join(vals));
        this.key = key;
        this.vals = vals;
    }

    public void setValues(String... vals) {
        this.vals = vals;
    }

    public String[] getValues() {
        return vals;
    }

    private static String join(String... vals) {
        if (vals == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < vals.length) {
            if (vals[i] != null) {
                sb.append(vals[i]);
            }
            if (++i < vals.length) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private static String[] encode(String... vals) {
        if (vals == null) {
            return null;
        }
        String[] enc = new String[vals.length];
        for (int i = 0; i < vals.length; i++) {
            enc[i] = LessBytes.urlencode(vals[i]);
        }
        return enc;
    }

    @Override
    public String toString() {
        if (vals == null) {
            return LessStrings.cat(LessBytes.urlencode(key), "=");
        } else {
            return LessStrings.cat(LessBytes.urlencode(key), "=", join(encode(vals)));
        }
    }

}
