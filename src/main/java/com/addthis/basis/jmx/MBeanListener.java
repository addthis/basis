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

import javax.management.ObjectName;

/**
 * Interface for a component interested in knowing when MBeans come and go.
 * A simplification of the JMX NotificationListener.
 */
public interface MBeanListener {
    /**
     * Called when a new MBean of interested arrives
     */
    public void mbeanAdded(ObjectName name);

    /**
     * Called after an MBean has departed
     */
    public void mbeanRemoved(ObjectName name);
}
