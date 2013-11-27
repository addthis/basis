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

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FilesTest {
    @Test
    public void testMatchDirs() throws Exception {
        for (String s : new String[]{"test/path/to/a/from/x", "test/path/to/b/from/y", "test/path/to/c/from/z", "test/path/to/c/from/zz"}) {
            new File(s).mkdirs();
        }
        for (File f : Files.matchFiles("test/path/to/*/from/*")) {
            System.out.println("found1 -> " + f);
        }
        for (File f : Files.matchFiles("test/path/to/*")) {
            System.out.println("found2 -> " + f);
        }
        for (File f : Files.matchFiles("test/*/*/*/from")) {
            System.out.println("found3 -> " + f);
        }
    }

    @Test
    public void testSuffix() {
        assertEquals("foo", Files.getSuffix("asdf.foo"));
        assertEquals("", Files.getSuffix(new File("/foo/bar.baz/zot")));
        assertEquals(new File("/foo/bar.baz"), Files.replaceSuffix(new File("/foo/bar.zot"), ".baz"));
        assertEquals(new File("/foo/bar"), Files.replaceSuffix(new File("/foo/bar.zot"), ""));
    }
}
