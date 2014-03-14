package com.addthis.basis.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdjustableSemaphoreTest {

    @Test
    public void postiveMaxPermitsConstructorTest() {
        AdjustableSemaphore semaphore = new AdjustableSemaphore(10);
        assertEquals(10, semaphore.availablePermits());
        assertEquals(10, semaphore.maxPermits());
        for(int i = 0; i < 10; i++) {
            assertTrue(semaphore.tryAcquire());
        }
        assertFalse(semaphore.tryAcquire());
        assertEquals(0, semaphore.availablePermits());
        assertEquals(10, semaphore.maxPermits());
    }

    @Test
    public void increaseMaxPermitsTest() {
        AdjustableSemaphore semaphore = new AdjustableSemaphore(0);
        assertEquals(0, semaphore.availablePermits());
        assertEquals(0, semaphore.maxPermits());
        semaphore.setMaxPermits(10);
        assertEquals(10, semaphore.availablePermits());
        assertEquals(10, semaphore.maxPermits());
        for(int i = 0; i < 10; i++) {
            assertTrue(semaphore.tryAcquire());
        }
        assertFalse(semaphore.tryAcquire());
        assertEquals(0, semaphore.availablePermits());
        assertEquals(10, semaphore.maxPermits());
    }


    @Test
    public void decreaseMaxPermitsTest() {
        AdjustableSemaphore semaphore = new AdjustableSemaphore(10);
        assertEquals(10, semaphore.availablePermits());
        assertEquals(10, semaphore.maxPermits());
        for(int i = 0; i < 10; i++) {
            assertTrue(semaphore.tryAcquire());
        }
        assertFalse(semaphore.tryAcquire());
        assertEquals(0, semaphore.availablePermits());
        assertEquals(10, semaphore.maxPermits());
        for(int i = 0; i < 10; i++) {
            semaphore.release();
        }
        semaphore.setMaxPermits(5);
        assertEquals(5, semaphore.availablePermits());
        assertEquals(5, semaphore.maxPermits());
        for(int i = 0; i < 5; i++) {
            assertTrue(semaphore.tryAcquire());
        }
        assertFalse(semaphore.tryAcquire());
    }

}
