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
package com.addthis.basis.jvm;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LessSafeTest {
    private static final Logger log = LoggerFactory.getLogger(LessSafeTest.class);

    @Test public void makeString() throws Exception {
        char[] chars = "abcdef".toCharArray();
        String unsafeString = LessSafe.noCopyString(chars);
        char[] expected = "0bcdef".toCharArray();
        chars[0] = '0';
        Assert.assertArrayEquals(expected, unsafeString.toCharArray());
    }
}