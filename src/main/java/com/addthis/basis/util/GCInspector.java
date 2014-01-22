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

import com.google.common.util.concurrent.Uninterruptibles;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is adapted from Apache Cassandra's GCInspector version
 * e79d9fbf84a35021cafa21d428e08fdd9bee584e
 */
public class GCInspector {
    private static final Logger logger = LoggerFactory.getLogger(GCInspector.class);
    static final long INTERVAL_IN_MS = 1000;
    static final long MIN_DURATION = Parameter.intValue("basis.gcinspector.min_duration", 200);

    public static final GCInspector instance = new GCInspector();

    private final Map<String, Long> gctimes = new HashMap<String, Long>();
    private final Map<String, Long> gccounts = new HashMap<String, Long>();

    final List<GarbageCollectorMXBean> beans = new ArrayList<GarbageCollectorMXBean>();
    final MemoryMXBean membean = ManagementFactory.getMemoryMXBean();

    public GCInspector() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName gcName = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");
            for (ObjectName name : server.queryNames(gcName, null)) {
                GarbageCollectorMXBean gc = ManagementFactory.newPlatformMXBeanProxy(server, name.getCanonicalName(), GarbageCollectorMXBean.class);
                beans.add(gc);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        try {
            // don't bother starting a thread that will do nothing.
            if (beans.size() == 0) {
                return;
            }

            Thread t = new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                            logGCResults();
                            Uninterruptibles.sleepUninterruptibly(INTERVAL_IN_MS, TimeUnit.MILLISECONDS);
                        }
                    }
                };
            t.setName("GCInspector");
            t.setDaemon(true);
            t.start();
        } catch (Throwable e) {
            logger.warn("Unable to start GCInspector.  The may be because your JVM does not support the same beans as HotSpot", e);
        }
    }

    private void logGCResults() {
        for (GarbageCollectorMXBean gc : beans) {
            Long previousTotal = gctimes.get(gc.getName());
            Long total = gc.getCollectionTime();
            if (previousTotal == null) {
                previousTotal = 0L;
            }
            if (previousTotal.equals(total)) {
                continue;
            }
            gctimes.put(gc.getName(), total);
            Long duration = total - previousTotal; // may be zero for a really fast collection

            Long previousCount = gccounts.get(gc.getName());
            Long count = gc.getCollectionCount();

            if (previousCount == null) {
                previousCount = 0L;
            }
            if (count.equals(previousCount)) {
                continue;
            }

            gccounts.put(gc.getName(), count);

            MemoryUsage mu = membean.getHeapMemoryUsage();
            long memoryUsed = mu.getUsed();
            long memoryMax = mu.getMax();

            String st = String.format("GC for %s: %s ms for %s collections, %s used; max is %s",
                                      gc.getName(), duration, count - previousCount, memoryUsed, memoryMax);
            long durationPerCollection = duration / (count - previousCount);
            if (durationPerCollection > MIN_DURATION) {
                logger.info(st);
            } else if (logger.isDebugEnabled()) {
                logger.debug(st);
            }
        }
    }
}
