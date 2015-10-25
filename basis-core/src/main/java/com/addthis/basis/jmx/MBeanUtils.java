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

import java.io.IOException;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.Attribute;
import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Utilities to make dealing with JMX easier.
 */
public class MBeanUtils {
    /**
     * ObjectName for the MBean server delegate
     */
    public static final ObjectName DELEGATE = parseName("JMImplementation:type=MBeanServerDelegate");

    /**
     * Notification type for registration
     */
    public static final String MBEAN_ADD = MBeanServerNotification.REGISTRATION_NOTIFICATION;

    /**
     * Notification type for unregistration
     */
    public static final String MBEAN_DEL = MBeanServerNotification.UNREGISTRATION_NOTIFICATION;

    /**
     * URL pattern for connecting remotely
     */
    public static final String URL_PATTERN = "service:jmx:rmi:///jndi/rmi://{{host}}:{{port}}/jmxrmi";

    // ===================================================================
    // NAMING STUFF
    // ===================================================================

    /**
     * Parses an object name, softening any exceptions that result.  Useful
     * for when you know the object name is going to be valid, or you don't
     * care about catching the resulting exception.
     *
     * @param name the name to parse
     * @return the parsed version of the name
     * @throws IllegalArgumentException if the name can't be parsed
     */
    public static ObjectName parseName(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException("bad name " + name, e);
        }
    }

    /**
     * Builds an object name, softening any exceptions that result.  Useful
     * for when you know the object name is going to be valid, or you don't
     * care about catching the resulting exception.
     *
     * @param domain the domain of the resulting name
     * @param props  (String,Object) pairs representing key properties
     * @return the parsed version of the name
     * @throws IllegalArgumentException if the name can't be parsed
     */
    public static ObjectName buildName(String domain, Object... props) {
        StringBuilder str = new StringBuilder();
        str.append(domain).append(":");
        for (int i = 0; i < props.length; i += 2) {
            if (i > 0) {
                str.append(",");
            }
            str.append(props[i]).append("=").append(String.valueOf(props[i + 1]));
        }
        return parseName(str.toString());
    }

    /**
     * Determines whether a name matches a pattern
     *
     * @param pattern the pattern to match (null to match anything)
     * @param target  the target object name
     * @return does the target match the pattern?
     */
    public static boolean match(ObjectName pattern, ObjectName target) {
        return pattern == null || pattern.apply(target);
    }

    // ===================================================================
    // REGISTERING
    // ===================================================================

    /*
     * Maps objects we've registered to their object names (so we can do
     * an unregister based on the object).  We can have more than one object
     * name per registered object, so we keep them in sets.
     */
    static final Map<Integer, Set<ObjectName>> registered = new HashMap<>();

    private static void putName(int code, ObjectName name) {
        Set<ObjectName> set = registered.get(code);
        if (set == null) {
            registered.put(code, set = new HashSet<>());
        }
        set.add(name);
    }

    /**
     * Register an object which is already an MBean into JMX, softening
     * any exceptions that occur
     *
     * @param name   the JMX name of the object
     * @param target the object to register
     * @throws RuntimeException if the registration fails
     */
    public static void register(ObjectName name, Object target) {
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(target, name);
            putName(System.identityHashCode(target), name);
        } catch (JMException e) {
            throw new RuntimeException("oops! couldn't register " + name, e);
        }
    }

    /**
     * Register an object which implements one or more interfaces into JMX,
     * softening any exceptions that result
     *
     * @param name     the JMX name of the object
     * @param target   the object to register
     * @param metadata metadata about methods of the object that should be
     *                 exposed; can either be Class<?> or String
     * @throws RuntimeException if the registration fails
     */
    public static void register(ObjectName name, Object target, Object... metadata) {
        List<Class<?>> ifcs = new LinkedList<>();
        List<String> methods = new LinkedList<>();

        for (Object o : metadata) {
            if (o instanceof Class<?>) {
                ifcs.add((Class<?>) o);
            } else {
                methods.add(o.toString());
            }
        }

        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(
                    createDynamicMBean(target, ifcs, methods),
                    name);
            putName(System.identityHashCode(target), name);
        } catch (JMException e) {
            throw new RuntimeException("oops! couldn't register " + name, e);
        }
    }

    /**
     * Register an object which exposes one or more methods into JMX,
     * softening any exceptions that result
     *
     * @param name    the JMX name of the object
     * @param obj     the object to register
     * @param methods methods exposed to JMX
     * @throws RuntimeException if the registration fails
     */
    public static void register(ObjectName name, Object obj, String... methods) {
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(
                    createDynamicMBean(obj, null, Arrays.asList(methods)),
                    name);
            putName(System.identityHashCode(obj), name);
        } catch (JMException e) {
            throw new RuntimeException("oops! couldn't register " + name, e);
        }
    }

    /**
     * Unregisters an MBean, softening any exceptions
     *
     * @param name the JMX name of the object
     * @throws RuntimeException if the unregistration fails
     */
    public static void unregister(ObjectName name) {
        try {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
        } catch (JMException e) {
            throw new RuntimeException("oops! couldn't register " + name, e);
        }
    }

    /**
     * Unregisters an MBean, softening any exceptions
     *
     * @param obj the MBean that was registered
     * @throws RuntimeException if the unregistration fails
     */
    public static void unregister(Object obj) {
        for (ObjectName name : registered.remove(System.identityHashCode(obj))) {
            unregister(name);
        }
    }

    // ===================================================================
    // LISTENERS
    // ===================================================================

    static final Map<Integer, NotificationListener> listeners = new HashMap<>();

    /**
     * Adds a listener to be notified when MBeans matching the specified
     * pattern get registered.
     *
     * @param listener the listener to add
     * @param pattern  the pattern for object names of interest (null for all)
     * @throws RuntimeException if something prevents adding the listener
     */
    public static void listen(final MBeanListener listener, final ObjectName pattern) {
        listen(listener, pattern, System.identityHashCode(listener));
    }

    /**
     * Creates a map which is tied to a JMX listener.  The maps contents are
     * concurrently updated as MBeans matching the name pattern come and go
     * from JMX.
     */
    public static <T> Map<ObjectName, T> listen(final ObjectName pattern, final Class<T> type) {
        final Map<ObjectName, T> map = new ConcurrentHashMap<>();

        final MBeanListener listener = new MBeanListener() {
            @Override
            public void mbeanAdded(ObjectName name) {
                map.put(name, createProxy(name, type));
            }

            @Override
            public void mbeanRemoved(ObjectName name) {
                map.remove(name);
            }
        };

        listen(listener, pattern, System.identityHashCode(map));
        return map;
    }

    /*
     * Registers a listener with JMX and scans current contents to look for
     * matching MBeans that have already been registered.  May or may not
     * update the internal dictionary of listeners.
     */
    protected static void listen(final MBeanListener listener, final ObjectName pattern, int key) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            NotificationListener jmxlisten = new NotificationListener() {
                @Override
                public void handleNotification(Notification notification, Object handback) {
                    MBeanServerNotification n = (MBeanServerNotification) notification;
                    if (match(pattern, n.getMBeanName())) {
                        if (MBEAN_ADD.equals(n.getType())) {
                            listener.mbeanAdded(n.getMBeanName());
                        }
                        if (MBEAN_DEL.equals(n.getType())) {
                            listener.mbeanRemoved(n.getMBeanName());
                        }
                    }
                }
            };

            server.addNotificationListener(DELEGATE, jmxlisten, null, null);

            listeners.put(key, jmxlisten);

            for (ObjectName name : server.queryNames(pattern, null)) {
                listener.mbeanAdded(name);
            }
        } catch (JMException e) {
            throw new RuntimeException("error adding listener to " + pattern, e);
        }
    }

    /**
     * Removes a listener that was previously registered
     */
    public static void unlisten(Object listener) {
        NotificationListener jmxlisten = listeners.remove(System.identityHashCode(listener));
        try {
            ManagementFactory.getPlatformMBeanServer().removeNotificationListener(DELEGATE, jmxlisten);
        } catch (JMException e) {
            throw new RuntimeException("couldn't unlisten", e);
        }
    }

    // ===================================================================
    // PROXIES ETC.
    // ===================================================================

    /**
     * Creates a dynamic MBean exposing the specified object with the
     * specified management interfaces & methods
     */
    public static DynamicMBean createDynamicMBean(Object target, Collection<Class<?>> ifcs, Collection<String> methods)
            throws JMException {
        return new DynamicMBeanImpl(target, ifcs, methods);
    }

    /**
     * Same as the three-arg version, but uses the classloader of the
     * supplied management interface
     */
    public static <T> T createProxy(ObjectName name, Class<T> ifc) {
        return createProxy(ifc.getClassLoader(), name, ifc);
    }

    /**
     * Creates a proxy on an MBean, allowing typesafe (and easier) access to
     * its operations through a known interface.
     *
     * @param loader the classloader for the proxy
     * @param name   the name of the target MBean
     * @param ifc    the supported interface
     * @return the proxy
     * @throws IllegalArgumentException if the named MBean doesn't support the
     *                                  required interface
     */
    public static <T> T createProxy(ClassLoader loader, ObjectName name, Class<T> ifc) {
        return createProxy(ManagementFactory.getPlatformMBeanServer(), loader, name, ifc);
    }

    /**
     * Creates a proxy on an MBean, allowing typesafe (and easier) access to
     * its operations through a known interface.  All operations go through
     * the supplied MBeanConnection.
     *
     * @param conn   the MBean server connection for the proxy
     * @param loader the classloader for the proxy
     * @param name   the name of the target MBean
     * @param ifc    the supported interface
     * @return the proxy
     * @throws IllegalArgumentException if the named MBean doesn't support the
     *                                  required interface
     */
    public static <T> T createProxy(final MBeanServerConnection conn, ClassLoader loader, ObjectName name, Class<T> ifc) {
        final ObjectName target = name;

        if (!supports(target, ifc)) {
            throw new IllegalArgumentException(name + " doesn't support " + ifc.getName());
        }

        InvocationHandler handler = new InvocationHandler() {
            MBeanInfo info = null;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                if (info == null) {
                    info = conn.getMBeanInfo(target);
                }

                if (args == null) {
                    args = new Object[0];
                }

                String mname = method.getName();
                String aname = getAttributeName(method);

                if (aname != null) {
                    aname = aname.toLowerCase();

                    for (MBeanAttributeInfo att : info.getAttributes()) {
                        if (att.getName().equalsIgnoreCase(aname)) {
                            aname = att.getName();
                            break;
                        }
                    }

                    if (mname.startsWith("get")) {
                        return conn.getAttribute(target, aname);
                    } else {
                        conn.setAttribute(target, new Attribute(aname, args[0]));
                        return null;
                    }
                } else {
                    String[] sig = new String[args.length];
                    for (int i = 0; i < sig.length; i++) {
                        sig[i] = method.getParameterTypes()[i].getName();
                    }
                    return conn.invoke(target, method.getName(), args, sig);
                }
            }
        };

        return ifc.cast(Proxy.newProxyInstance(loader, new Class<?>[]{ifc}, handler));
    }

    // ===================================================================
    // REFLECTION
    // ===================================================================

    /**
     * Examines an MBean to determine if it supports all the methods of the
     * supplied interface (as determined by supports(MBeanInfo,Method))
     *
     * @param name the name of the MBean in question
     * @param ifc  the interface in question
     * @return does the MBean support the interface (returns false if any
     *         exceptions prevent the operation)
     */
    public static boolean supports(ObjectName name, Class<?> ifc) {
        MBeanInfo info = null;
        try {
            info = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(name);
        } catch (JMException e) {
            return false;
        }
        return supports(info, ifc);
    }

    /**
     * Examines MBeanInfo to determine if the described MBean supports all
     * the methods of the supplied interface (as determined by
     * support(MBeanInfo,Method).
     *
     * @param info the MBean in question
     * @param ifc  the interface in question
     * @return does the MBean support the interface?
     */
    public static boolean supports(MBeanInfo info, Class<?> ifc) {
        for (Method method : ifc.getMethods()) {
            if (!supports(info, method)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Examines MBeanInfo to determine if the described MBean supports the
     * method in question.  Comparison is simple, based on the exact method
     * name and number and type of arguments (no reasoning about parameter
     * subclasses or overridden methods is done).
     *
     * @param info   MBeanInfo for the MBean in question
     * @param method the method in question
     * @return true if the MBean supports the method
     */
    public static boolean supports(MBeanInfo info, Method method) {
        String mname = method.getName();
        String aname = getAttributeName(method);

        if (aname != null) {
            for (MBeanAttributeInfo att : info.getAttributes()) {
                if (att.getName().equalsIgnoreCase(aname)) {
                    if (mname.startsWith("get") && att.isReadable()) {
                        return true;
                    } else if (mname.startsWith("set") && att.isWritable()) {
                        return true;
                    }
                    return false;
                }
            }
            return false;
        } else {
            List<String> osig = new LinkedList<>();
            List<String> msig = new LinkedList<>();
            for (Class<?> cls : method.getParameterTypes()) {
                msig.add(cls.getName());
            }

            for (MBeanOperationInfo op : info.getOperations()) {
                osig.clear();
                for (MBeanParameterInfo param : op.getSignature()) {
                    osig.add(param.getType());
                }
                if (op.getName().equals(method.getName()) && osig.equals(msig)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * @return the name of the attribute this method refers to, if it's a
     *         setter or getter; null otherwise
     */
    public static String getAttributeName(Method method) {
        String name = method.getName();

        if (name.startsWith("set") &&
                method.getReturnType() == void.class &&
                method.getParameterTypes().length == 1) {
            return name.substring(3);
        }

        if (name.startsWith("get") &&
                method.getReturnType() != void.class &&
                method.getParameterTypes().length == 0) {
            return name.substring(3);
        }

        return null;
    }

    // ===================================================================
    // REMOTENESS
    // ===================================================================

    /**
     * @return an MBeanServerConnection to the specified host and port
     */
    public static MBeanServerConnection connect(String host, int port) throws IOException {
        String u = URL_PATTERN;
        u = u.replace("{{host}}", host);
        u = u.replace("{{port}}", Integer.toString(port));
        return JMXConnectorFactory.connect(new JMXServiceURL(u)).getMBeanServerConnection();
    }
}
