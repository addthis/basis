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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.Uninterruptibles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.SharedSecrets;

/**
 * Utility methods related to JVM shutdown. The primary features are an exit method that won't cause JVM hangs, and
 * a utility for enforcing semantics for "if this object is made, it _must_ be closed" (or similar). There are a couple
 * extras as well, and I have a few fairly good ideas for the future: a try-with-resources type object that ensures
 * the JVM won't effectively halt until released might be easier than the function-passing method, and generalization
 * such that it isn't necessarily tied to the actual Runtime (and reliant on all static methods). However, this more
 * than suffices for now.
 */
@Beta
public final class Shutdown {
    private static final Logger log = LoggerFactory.getLogger(Shutdown.class);

    private static final AtomicBoolean CALLED_EXIT_SELF = new AtomicBoolean(false);
    private static final AtomicInteger EXIT_HOLDER = new AtomicInteger(0);
    private static final AtomicReference<Thread> EXIT_THREAD = new AtomicReference<>(null);
    private static final boolean HOOK_ADDED;
    private static final boolean SENTINAL_ADDED;
    static {
        boolean hookAdded = false;
        try {
            SharedSecrets.getJavaLangAccess().registerShutdownHook(3, true, () -> {
                if (EXIT_HOLDER.get() != 0) {
                    Runtime.getRuntime().halt(EXIT_HOLDER.get());
                }
            });
            hookAdded = true;
        } catch (Throwable t) {
            log.error("Could not register super-shutdown hook deluxe", t);
        }
        HOOK_ADDED = hookAdded;
        if (!HOOK_ADDED) {
            Thread sentinelHook = new Thread(() -> {
                log.debug("Shutdown has already started, our secret-weapon hook did not work, and we don't know who " +
                          "started the shutdown or what exit code they will try to use! Therefore, we will make this " +
                          "shutdown hook thread block another 2 seconds and then halt the jvm ourselves.");
                Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
                Runtime.getRuntime().halt(EXIT_HOLDER.get());
            }, "JVM Shutdown Sentinel") {
                @Override public synchronized void start() {
                    Thread invokingThread = Thread.currentThread();
                    // if the thread calling "exit" logic does not seem to be ours, then we cannot trust its exit value
                    if (!invokingThread.equals(EXIT_THREAD.get()) && (EXIT_HOLDER.get() != 0)) {
                        super.start();
                    }
                }
            };
            SENTINAL_ADDED = tryAddShutdownHook(sentinelHook);
        } else {
            SENTINAL_ADDED = false;
        }
    }

    public static void exit(int exitCode) {
        boolean exitBecameNonZero;
        if (exitCode != 0) {
            log.warn("Calling exit with non-zero exit code: {}", exitCode);
            exitBecameNonZero = EXIT_HOLDER.compareAndSet(0, exitCode);
        } else {
            exitBecameNonZero = false;
        }
        int prevExitCode;
        if (exitBecameNonZero) {
            prevExitCode = 0;
        } else {
            prevExitCode = EXIT_HOLDER.get();
        }
        if (!CALLED_EXIT_SELF.compareAndSet(false, true)) {
            if (exitBecameNonZero && HOOK_ADDED) {
                log.warn("Changing exit code to: {}", exitCode);
            } else if (exitBecameNonZero) {
                log.error("Previously, we already started shutdown with a normal exit code! Furthermore, our mega-" +
                          "hook did not get set up right! Halting the JVM immediately to try to ensure error is seen.");
                Runtime.getRuntime().halt(exitCode);
            } else {
                log.error("Tried to call exit with exit code {}, but someone else already called with {}.",
                          exitCode, prevExitCode);
            }
            return;
        }
        if (HOOK_ADDED || SENTINAL_ADDED) {
            Thread exitThread =
                    new Thread(() -> Runtime.getRuntime().exit(exitCode), "JVM Exit Thread (code: " + exitCode + ")");
            EXIT_THREAD.set(exitThread);
            exitThread.start();
        } else if (exitCode != 0) {
            log.error(
                    "Our secret weapon must have failed, _and_ either the JVM started shutting down before this class" +
                    " was ever initialized (try to load it sooner!) or some other very bad thing has occured. In any " +
                    "event, since this is an \"exit\" call anyway, we are just going to halt now because this is the " +
                    "best effort attempt we can make.");
            Runtime.getRuntime().halt(exitCode);
        }
    }

    public static <T> void createWithShutdownHook(Supplier<T> creator, Consumer<T> hook) {
        CompletableFuture<Runnable> creationFuture = new CompletableFuture<>();
        Thread shutdownHook = new Thread(creationFuture.join()::run, "Shutdown Hook for " + creator);
        boolean hookAdded = tryAddShutdownHook(shutdownHook);
        if (hookAdded) {
            try {
                T created = creator.get();
                creationFuture.complete(() -> hook.accept(created));
            } catch (Throwable t) {
                if (!tryRemoveShutdownHook(shutdownHook)) {
                    creationFuture.complete(
                            () -> log.debug("skipping hook: {} because creator: {} failed to run normally",
                                            hook, creator));
                }
                throw t;
            }
        }
    }

    public static boolean tryAddShutdownHook(Thread shutdownHook) {
        try {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            return true;
        } catch (IllegalStateException logged) {
            log.debug("adding shutdown hook failed", logged);
            return false;
        }
    }

    public static boolean tryRemoveShutdownHook(Thread shutdownHook) {
        try {
            return Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException logged) {
            log.debug("removing shutdown hook failed", logged);
            return false;
        }
    }

    private Shutdown() {}
}
