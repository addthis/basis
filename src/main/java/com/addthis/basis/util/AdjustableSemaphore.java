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

import java.util.concurrent.Semaphore;

/**
 * A simple implementation of an adjustable semaphore.
 * <p/>
 * Written by Marshall Pierce and released to the public domain
 * See: http://blog.teamlazerbeez.com/2009/04/20/javas-semaphore-resizing/
 */
final public class AdjustableSemaphore {

    /**
     * semaphore starts at 0 capacity; must be set by setMaxPermits before use
     */
    private final ResizeableSemaphore semaphore = new ResizeableSemaphore();

    /**
     * how many permits are allowed as governed by this semaphore.
     * Access must be synchronized on this object.
     */
    private volatile int maxPermits = 0;

    public AdjustableSemaphore() {
    }

    public AdjustableSemaphore(int maxPermits) {
        if (maxPermits < 0) {
            throw new IllegalArgumentException("maxPermits must be a non-negative value");
        } else if (maxPermits > 0) {
            setMaxPermits(maxPermits);
        }
    }

    /**
     * Returns the number of max permits.
     * This method may not reflect the changes of a
     * concurrent {@link #setMaxPermits(int)} operation.
     *
     * @return
     */
    public int maxPermits() {
        return maxPermits;
    }
 
    /*
     * Must be synchronized because the underlying int is not thread safe
     */

    /**
     * Set the max number of permits. Must be greater than zero.
     * <p/>
     * Note that if there are more than the new max number of permits currently
     * outstanding, any currently blocking threads or any new threads that start
     * to block after the call will wait until enough permits have been released to
     * have the number of outstanding permits fall below the new maximum. In
     * other words, it does what you probably think it should.
     *
     * @param newMax
     */
    public synchronized void setMaxPermits(int newMax) {
        if (newMax < 1) {
            throw new IllegalArgumentException("Semaphore size must be at least 1,"
                                               + " was " + newMax);
        }

        int delta = newMax - this.maxPermits;

        if (delta == 0) {
            return;
        } else if (delta > 0) {
            // new max is higher, so release that many permits
            this.semaphore.release(delta);
        } else {
            delta = -delta;
            // delta < 0.
            // reducePermits needs a positive #, though.
            this.semaphore.reducePermits(delta);
        }

        this.maxPermits = newMax;
    }

    /**
     * Release a permit back to the semaphore. Make sure not to double-release.
     */
    public void release() {
        this.semaphore.release();
    }

    /**
     * Get a permit, blocking if necessary.
     *
     * @throws InterruptedException if interrupted while waiting for a permit
     */
    public void acquire() throws InterruptedException {
        this.semaphore.acquire();
    }

    /**
     * Get a permit if one is available when called.
     *
     * @return True if a permit was available, false otherwise.
     */
    public boolean tryAcquire() {
        return this.semaphore.tryAcquire();
    }

    public int availablePermits() {
        return this.semaphore.availablePermits();
    }

    /**
     * A trivial subclass of <code>Semaphore</code> that exposes the reducePermits
     * call to the parent class. Doug Lea says it's ok...
     * http://osdir.com/ml/java.jsr.166-concurrency/2003-10/msg00042.html
     */
    private static final class ResizeableSemaphore extends Semaphore {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        /**
         * Create a new semaphore with 0 permits.
         */
        ResizeableSemaphore() {
            super(0);
        }

        @Override
        protected void reducePermits(int reduction) {
            super.reducePermits(reduction);
        }
    }
}
