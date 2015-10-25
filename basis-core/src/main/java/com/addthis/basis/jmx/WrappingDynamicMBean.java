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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

/**
 * Implements the DynamicMBean interface by wrapping another dynamic
 * MBean (which may change over the lifetime of this object).
 */
public class WrappingDynamicMBean implements DynamicMBean {
    protected DynamicMBean wrapped;

    public Object getAttribute(String att)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        return wrapped.getAttribute(att);
    }

    public AttributeList getAttributes(String[] atts) {
        return wrapped.getAttributes(atts);
    }

    public MBeanInfo getMBeanInfo() {
        return wrapped.getMBeanInfo();
    }

    public Object invoke(String action, Object[] args, String[] sig)
            throws MBeanException, ReflectionException {
        return wrapped.invoke(action, args, sig);
    }

    public void setAttribute(Attribute att)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        wrapped.setAttribute(att);
    }

    public AttributeList setAttributes(AttributeList atts) {
        return wrapped.setAttributes(atts);
    }
}
