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

import java.lang.reflect.Field;

import com.google.common.annotations.Beta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.JavaLangAccess;
import sun.misc.SharedSecrets;
import sun.misc.Unsafe;

/**
 * Utility class focusing on non-standard JDK classes that mostly center around performance (eg. the Unsafe class).
 */
@Beta
public final class LessSafe {
    private static final Logger log = LoggerFactory.getLogger(LessSafe.class);

    private static final JavaLangAccess javaLangAccess;
    private static final Unsafe unsafe;
    static {
        JavaLangAccess tryGetJavaLangAccess = null;
        try {
            tryGetJavaLangAccess = SharedSecrets.getJavaLangAccess();
        } catch (Throwable t) {
            log.warn("Problem getting java lang access class", t);
        }
        javaLangAccess = tryGetJavaLangAccess;

        Unsafe tryGetUnsafe = null;
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            tryGetUnsafe = (Unsafe) unsafeField.get(null);
        } catch (Throwable t) {
            log.warn("Problem getting the unsafe class", t);
        }
        unsafe = tryGetUnsafe;
    }

    /**
     * Construct a string without paying for character array copying. This is slightly less safe if you don't entirely
     * own the character array in question, but otherwise is pretty much strictly better. If for some reason, we can't
     * find the non-standard class that exposes this ability, then we will default to doing a standard copy constructor.
     */
    public static String noCopyString(char[] chars) {
        if (javaLangAccess != null) {
            return javaLangAccess.newStringUnsafe(chars);
        } else {
            return new String(chars);
        }
    }

    private LessSafe() {}
}
