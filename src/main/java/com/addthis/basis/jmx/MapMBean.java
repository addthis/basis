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
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * It is often the case that one possess a Map that has useful data in
 * it and it would be nice to see those values via jmx.  This class
 * takes a map and wraps a DynamicMBean around it.  While any subclass
 * of Object is accepted as a value, it is your fault if jconsole
 * explodes with a whacky value.
 * <p/>
 * This class can not know what locking scheme you use for your Map.
 * It is suggested that you pass a series of immutable maps with
 * setMap to provide updated values.
 * <p/>
 * TODO: It is unclear how the world of java beans will deal with a
 * changing set of attributes.
 */
public class MapMBean implements DynamicMBean {

    private Map<String, ?> map;


    public MapMBean(Map<String, ?> map) {
        setMap(map);
    }

    public void setMap(Map<String, ?> map) {
        if (map == null) {
            this.map = new HashMap<String, Object>();
        } else {
            this.map = map;
        }
    }


    @Override
    public Object getAttribute(String attribute) {
        return map.get(attribute);
    }


    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList attrLst = new AttributeList(map.size());
        for (String key : attributes) {
            if (map.containsKey(key)) {
                attrLst.add(new Attribute(key, map.get(key)));
            }
        }
        return attrLst;
    }


    @Override
    public MBeanInfo getMBeanInfo() {
        List<MBeanAttributeInfo> aInfo = new ArrayList<>();
        for (String key : map.keySet()) {
            aInfo.add(new MBeanAttributeInfo(key, "java.lang.String", "", true, false, false));
        }
        MBeanAttributeInfo[] aArray = aInfo.toArray(new MBeanAttributeInfo[aInfo.size()]);
        return new MBeanInfo("com.addthis.basis.jmx.MapMBean",
                null,
                aArray,
                null, null, null);
    }


    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) {
        return null;  // Throw not implemented?
    }


    @Override
    public void setAttribute(Attribute attribute) {
        // Throw not implemented?
    }


    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return null;  // Throw not implemented?
    }

}

