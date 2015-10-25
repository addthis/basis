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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;

/**
 * Implements the DynamicMBean interface by using reflection to make all
 * of its public instance fields available through JMX.  He's capable of
 * registering observers that get notified when his fields are changed
 * through JMX.
 */
public class FieldBasedDynamicMBean extends Observable implements DynamicMBean {
    protected boolean readonly;
    protected Map<String, Field> fields;

    protected FieldBasedDynamicMBean() {
        this(true);
    }

    protected FieldBasedDynamicMBean(boolean readonly) {
        this.readonly = readonly;
        this.fields = findFields();
    }

    @Override
    public Object getAttribute(String att)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        Field f = fields.get(att);
        if (f == null) {
            throw new AttributeNotFoundException(att);
        }

        try {
            return f.get(this);
        } catch (Exception e) {
            throw new ReflectionException(e, "error getting attribute " + att);
        }
    }

    @Override
    public AttributeList getAttributes(String[] atts) {
        AttributeList list = new AttributeList();
        for (String att : atts) {
            try {
                list.add(new Attribute(att, getAttribute(att)));
            } catch (Exception e) {
            }
        }
        return list;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        List<MBeanAttributeInfo> atts = new LinkedList<>();

        for (Field field : fields.values()) {
            MBeanAttributeInfo att = new MBeanAttributeInfo(
                    field.getName(),
                    field.getType().getName(),
                    null,
                    true,
                    !(readonly || Modifier.isFinal(field.getModifiers())),
                    field.getType() == Boolean.TYPE,
                    null);

            atts.add(att);
        }

        return new MBeanInfo(
                getClass().getName(), null,
                atts.toArray(new MBeanAttributeInfo[atts.size()]),
                new MBeanConstructorInfo[0],
                new MBeanOperationInfo[0],
                new MBeanNotificationInfo[0]);
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        throw new UnsupportedOperationException(actionName);
    }

    @Override
    public void setAttribute(Attribute att)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        Field f = fields.get(att.getName());
        if (f == null) {
            throw new AttributeNotFoundException(att.getName());
        }

        if (readonly || Modifier.isFinal(f.getModifiers())) {
            throw new UnsupportedOperationException(att.getName() + " is read-only");
        }

        try {
            f.set(this, att.getValue());
            notifyObservers(f.getName());
        } catch (Exception e) {
            throw new ReflectionException(e, "error setting attribute " + att);
        }
    }

    @Override
    public AttributeList setAttributes(AttributeList atts) {
        if (readonly) {
            return new AttributeList();
        }

        List<String> names = new LinkedList<>();

        for (Object att : atts) {
            try {
                setAttribute((Attribute) att);
                names.add(((Attribute) att).getName());
            } catch (Exception e) {
            }
        }

        return getAttributes(names.toArray(new String[names.size()]));
    }

    protected Map<String, Field> findFields() {
        Map<String, Field> map = new HashMap<>();

        for (Field f : getClass().getFields()) {
            int m = f.getModifiers();
            if (Modifier.isPublic(m) && !Modifier.isStatic(m)) {
                map.put(f.getName(), f);
            }
        }

        return map;
    }
}
