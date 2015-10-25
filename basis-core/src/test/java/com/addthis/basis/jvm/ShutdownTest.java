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

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShutdownTest {
    private static final Logger log = LoggerFactory.getLogger(ShutdownTest.class);

    @Ignore("surefire doesn't like it when you do this, but running via intellij shows it doesn't get stuck as normal")
    @Test public void nonblockingShutdown() throws Exception {
        final Thread testingThread = new Thread(() -> Shutdown.exit(12));
        Shutdown.tryAddShutdownHook(new Thread(() -> {
            Shutdown.exit(12);
        }));
        testingThread.start();
    }
}