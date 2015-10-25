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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * estimates the retained size of an object graph in memory
 */
public final class MemoryCounter {
    private static final ConcurrentHashMap<Class<?>, FieldCache[]> fieldCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MemEstimator> estimators = new IdentityHashMap<>();
    private static Instrumentation instrumentation;

    private static final int booleanClass = boolean.class.hashCode();
    private static final int byteClass = byte.class.hashCode();
    private static final int charClass = char.class.hashCode();
    private static final int shortClass = short.class.hashCode();
    private static final int intClass = int.class.hashCode();
    private static final int floatClass = float.class.hashCode();
    private static final int doubleClass = double.class.hashCode();
    private static final int longClass = long.class.hashCode();

    public static void premain(String args, Instrumentation inst) {
        System.out.println("using native jvm instrumentation: " + inst);
        instrumentation = inst;
    }

    public static void registerEstimator(Class<?> clazz, MemEstimator est) {
        estimators.put(clazz, est);
    }

    static {
        registerEstimator(String.class, new StringEstimator());
    }

    /**
     * control sizing estimation
     */
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface Mem {
        boolean estimate() default true;

        int size() default -1;
    }

    public static interface MemEstimator {
        public long getMemorySize(Object object);
    }

    public static class StringEstimator implements MemEstimator {
        private static final long base_size = estimateSize(new String(""));

        @Override
        public long getMemorySize(Object object) {
            return base_size + (((String) object).length() * 2);
        }
    }

    private final Map<Object, Object> visited = new IdentityHashMap<>();
    private final LinkedList<Object> stack = new LinkedList<>();

    /**
     * public api for static use
     */
    public static long estimateSize(Object o) {
        return new MemoryCounter().estimate(o);
    }

    private long estimate(Object obj) {
        long result = _estimate(obj);
        while (!stack.isEmpty()) {
            result += _estimate(stack.pop());
        }
        return result;
    }

    private boolean skipObject(Object obj) {
        return (obj == null) || visited.containsKey(obj);
    }

    /**
     * cache relevant field info
     */
    private static FieldCache[] getFieldCache(Class<?> clazz) {
        FieldCache fields[] = fieldCache.get(clazz);
        if (fields == null) {
            Field[] rawfields = clazz.getDeclaredFields();
            ArrayList<FieldCache> list = new ArrayList<>(rawfields.length);
            for (Field rawfield : rawfields) {
                if (!(Modifier.isStatic(rawfield.getModifiers()) || rawfield.isSynthetic())) {
                    FieldCache cachedField = new FieldCache();
                    cachedField.field = rawfield;
                    if (cachedField.field.getType().isPrimitive()) {
                        cachedField.primitive = getPrimitiveFieldSize(cachedField.field.getType());
                    } else {
                        cachedField.policy = cachedField.field.getAnnotation(Mem.class);
                        cachedField.field.setAccessible(true);
                    }
                    list.add(cachedField);
                }
            }
            fields = list.toArray(new FieldCache[list.size()]);
            fieldCache.put(clazz, fields);
        }
        return fields;
    }

    private long _estimate(Object obj) {
        if (skipObject(obj)) {
            return 0;
        }
        visited.put(obj, null);
        long result = 0;
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            return _estimateArray(obj);
        }
        if (clazz == Thread.class || clazz == ThreadGroup.class) {
            System.err.println("estimator rejecting " + clazz + " = " + obj);
            return 0;
        }
        MemEstimator est = estimators.get(clazz);
        if (est != null) {
            return roundUpToNearestEightBytes(est.getMemorySize(obj));
        }
        if (instrumentation != null) {
            result = instrumentation.getObjectSize(obj);
        }
        while (clazz != null) {
            FieldCache fields[] = getFieldCache(clazz);
            for (FieldCache field : fields) {
                if (field.primitive > 0) {
                    if (instrumentation == null) {
                        result += field.primitive;
                    }
                } else {
                    Annotation policy = field.policy;
                    if (policy != null) {
                        Mem mp = (Mem) policy;
                        if (mp.size() >= 0) {
                            result += mp.size();
                            continue;
                        }
                        if (!mp.estimate()) {
                            continue;
                        }
                    }
                    if (instrumentation == null) {
                        result += getPointerSize();
                    }
                    try {
                        Object toBeDone = field.field.get(obj);
                        if (toBeDone != null) {
                            stack.add(toBeDone);
                        }
                    } catch (IllegalAccessException ex) {
                        assert false;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return roundUpToNearestEightBytes(result + getClassSize());
    }

    private long roundUpToNearestEightBytes(long result) {
        long mod = result % 8;
        if (mod != 0) {
            result += 8 - mod;
        }
        return result;
    }

    private long _estimateArray(Object obj) {
        long result = 16;
        int length = Array.getLength(obj);
        if (length != 0) {
            Class<?> arrayElementClazz = obj.getClass().getComponentType();
            if (arrayElementClazz.isPrimitive()) {
                result += length * getPrimitiveArrayElementSize(arrayElementClazz);
            } else {
                for (int i = 0; i < length; i++) {
                    result += getPointerSize() + _estimate(Array.get(obj, i));
                }
            }
        }
        return result;
    }

    private static int getPrimitiveFieldSize(Class<?> clazz) {
        int hc = clazz.hashCode();
        if (hc == booleanClass) {
            return 1;
        }
        if (hc == byteClass) {
            return 1;
        }
        if (hc == charClass) {
            return 2;
        }
        if (hc == shortClass) {
            return 2;
        }
        if (hc == intClass) {
            return 4;
        }
        if (hc == floatClass) {
            return 4;
        }
        if (hc == doubleClass) {
            return 8;
        }
        if (hc == longClass) {
            return 8;
        }
        return 0;
    }

    private static int getPrimitiveArrayElementSize(Class<?> clazz) {
        return getPrimitiveFieldSize(clazz);
    }

    private static int getPointerSize() {
        return 4;
    }

    private static int getClassSize() {
        return 8;
    }

    /**
     * cache object for a class' field
     */
    private static final class FieldCache {
        private Field field;
        private int primitive;
        private Annotation policy;
    }
}
