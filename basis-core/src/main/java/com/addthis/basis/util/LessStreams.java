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
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LessStreams {
    private static final Logger log = LoggerFactory.getLogger(LessStreams.class);

    public static <T> Stream<T> stream(Iterator<T> source) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(source, 0), false);
    }

    public static <T> Stream<T> stream(Iterable<T> source) {
        return StreamSupport.stream(source.spliterator(), false);
    }

    private LessStreams() {}
}
