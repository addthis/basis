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
package com.addthis.basis.jmx;

import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

/**
 * Implementation of DynamicMBean over a target object and information
 * about methods of the object that should be exposed.  This information
 * comes either from formally declared interfaces, or simply from method
 * names.  NOTE: this implementation does NOT support overloaded methods,
 * so if you have any, results will be unpredictable.
 */
public class DynamicMBeanImpl implements DynamicMBean {
    Object target;
    Map<String, Method> methods;
    MBeanInfo info;

    public DynamicMBeanImpl(Object obj, Collection<Class<?>> ifcs, Collection<String> names)
            throws IntrospectionException {
        this.target = obj;
        this.methods = new HashMap<>();

        // we store methods in a map with keys like this: methodName/N, where
        // N is the number of args in the method (we don't support overloading,
        // so we don't care about argument types

        if (ifcs != null) {
            for (Class<?> ifc : ifcs) {
                for (Method method : ifc.getMethods()) {
                    if (method.getDeclaringClass() != Object.class) {
                        String key = method.getName() + "/" + method.getParameterTypes().length;
                        if (!methods.containsKey(key)) {
                            methods.put(key, method);
                        }
                    }
                }
            }
        }

        if (names != null && names.size() > 0) {
            for (Method method : target.getClass().getMethods()) {
                if (names.contains(method.getName()) && method.getDeclaringClass() != Object.class) {
                    String key = method.getName() + "/" + method.getParameterTypes().length;
                    if (!methods.containsKey(key)) {
                        methods.put(key, method);
                    }
                }
            }
        }

        // once we know about all our exposed methods, we can assemble
        // our MBeanInfo.  all known methods are exposed as operations,
        // and ones that looks like getters/setters define attributes.

        List<MBeanOperationInfo> ops = new LinkedList<>();
        for (Method method : methods.values()) {
            ops.add(new MBeanOperationInfo(null, method));
        }

        List<MBeanAttributeInfo> atts = new LinkedList<>();
        for (String key : methods.keySet()) {
            if (key.startsWith("get") && key.endsWith("/0")) {
                String name = key.substring(3, key.indexOf("/"));
                Method getter = methods.get(key);
                Method setter = methods.get("set" + name + "/1");
                atts.add(new MBeanAttributeInfo(name, null, getter, setter));
            }
        }

        MBeanOperationInfo[] o = ops.toArray(new MBeanOperationInfo[ops.size()]);
        MBeanAttributeInfo[] a = atts.toArray(new MBeanAttributeInfo[atts.size()]);
        info = new MBeanInfo(target.getClass().getName(), null, a, null, o, null);
    }

    @Override
    public Object getAttribute(String att)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        Method getter = methods.get("get" + att + "/0");

        if (getter == null) {
            throw new ReflectionException(null, "no getter for " + att);
        }

        try {
            return getter.invoke(target);
        } catch (Exception e) {
            throw new MBeanException(e, "error getting att " + att);
        }
    }

    @Override
    public AttributeList getAttributes(String[] atts) {
        AttributeList list = new AttributeList();
        for (String att : atts) {
            try {
                list.add(new Attribute(att, getAttribute(att)));
            } catch (JMException e) {
            }
        }
        return list;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return info;
    }

    @Override
    public Object invoke(String name, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        String text = name + "(" + Arrays.toString(signature) + ")";

        Method method = methods.get(name + "/" + signature.length);
        if (method == null) {
            throw new ReflectionException(null, "no such method: " + text);
        }

        try {
            return method.invoke(target, params);
        } catch (Exception e) {
            throw new MBeanException(e, "error invoking method " + text);
        }
    }

    @Override
    public void setAttribute(Attribute att)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        Method setter = methods.get("set" + att.getName() + "/1");
        if (setter == null) {
            throw new ReflectionException(null, "no setter for " + att);
        }

        try {
            setter.invoke(target, att.getValue());
        } catch (Exception e) {
            throw new MBeanException(e, "error setting att " + att);
        }
    }

    @Override
    public AttributeList setAttributes(AttributeList atts) {
        AttributeList list = new AttributeList();
        for (Attribute att : atts.asList()) {
            try {
                setAttribute(att);
                list.add(att);
            } catch (JMException e) {
            }
        }
        return list;
    }
}
