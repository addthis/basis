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

import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * for cloning iterators of sync-unsafe classes.  provides a mechanism
 * to hand out an iterator that will not throw a concurrent modification
 * exception or cause one to be thrown.
 *
 * @param <T>
 */
public class IteratorClone<T> implements Iterator<T> {
    protected static final Logger log = LoggerFactory.getLogger(IteratorClone.class);

    private final Iterator<T> clone;

    public IteratorClone(Iterator<T> source) {
        this(source, 16);
    }

    public IteratorClone(Iterator<T> source, int size) {
        ArrayList<T> list = new ArrayList<>(size);
        while (source.hasNext()) {
            list.add(source.next());
        }
        clone = list.iterator();
    }

    @Override
    public String toString() {
        return "IC[" + hasNext() + "]";
    }

    @Override
    public boolean hasNext() {
        return clone.hasNext();
    }

    @Override
    public T next() {
        return clone.next();
    }

    @Override
    public void remove() {
        clone.remove();
    }
}
