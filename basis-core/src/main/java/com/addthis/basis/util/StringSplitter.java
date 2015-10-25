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

import java.util.Iterator;
import java.util.StringTokenizer;

public class StringSplitter extends StringTokenizer implements Iterable<String> {

    public StringSplitter(String str, String delim) {
        super(str, delim);
    }

    public StringSplitter(String str, String delim, boolean includeTok) {
        super(str, delim, includeTok);
    }

    public Iterator<String> iterator() {
        return new Iterator<String>() {

            public boolean hasNext() {
                return hasMoreTokens();
            }

            public String next() {
                return nextToken();
            }

            public void remove() {
            }

        };
    }
}
