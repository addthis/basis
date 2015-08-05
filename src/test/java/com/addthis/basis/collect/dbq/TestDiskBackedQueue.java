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
package com.addthis.basis.collect.dbq;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.addthis.basis.util.LessPaths;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestDiskBackedQueue {

    private static final Logger log = LoggerFactory.getLogger(TestDiskBackedQueue.class);

    public static final SerializableSerializer<String> serializableSerializer = new SerializableSerializer<>();

    private static ImmutableList<String> ELEMENTS = ImmutableList.of("hello", "world", "foo", "bar", "baz", "quux");

    public static final Serializer<String> serializer = new Serializer<String>() {

        @Override public void toOutputStream(String input, OutputStream output) throws IOException {
            byte[] data = input.getBytes();
            Page.writeInt(output, data.length);
            output.write(data);
        }

        @Override public String fromInputStream(InputStream input) throws IOException {
            int length = Page.readInt(input);
            byte[] data = new byte[length];
            int read = input.read(data);
            if (read < length) {
                throw new IOException("Expected " + length
                                      + " bytes and received " + read + " + bytes");
            }
            return new String(data);
        }
    };

    @Test
    public void inMemoryWithoutBackgroundThreads() throws Exception {
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        builder.setPageSize(1024);
        builder.setMemMinCapacity(1024);
        builder.setMemMaxCapacity(1024);
        builder.setDiskMaxBytes(0);
        builder.setSerializer(serializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(0);
        builder.setTerminationWait(Duration.ofMinutes(2));
        builder.setShutdownHook(false);
        builder.setCompress(true);
        builder.setMemoryDouble(false);
        DiskBackedQueue<String> queue = builder.build();
        queue.put("hello", null);
        queue.put("world", null);
        assertEquals("hello", queue.poll());
        assertEquals("world", queue.poll());
        assertNull(queue.poll());
        assertEquals(0, filecount(path));
        queue.close();
        LessPaths.recursiveDelete(path);
    }

    @Test
    public void multiPageWithoutBackgroundThreads() throws Exception {
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        builder.setPageSize(2);
        builder.setMemMinCapacity(2);
        builder.setMemMaxCapacity(2);
        builder.setDiskMaxBytes(0);
        builder.setSerializer(serializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(0);
        builder.setShutdownHook(false);
        builder.setCompress(true);
        builder.setMemoryDouble(false);
        builder.setTerminationWait(Duration.ofMinutes(2));
        DiskBackedQueue<String> queue = builder.build();
        queue.put("hello", null);
        queue.put("world", null);
        queue.put("foo", null);
        queue.put("barbaz", null);
        // we cannot serialize a page that is referenced by the readPage
        assertEquals(0, filecount(path));
        assertEquals("hello", queue.poll());
        assertEquals("world", queue.poll());
        assertEquals("foo", queue.poll());
        assertEquals("barbaz", queue.poll());
        assertNull(queue.poll());
        assertEquals(0, filecount(path));
        queue.close();
        LessPaths.recursiveDelete(path);
    }

    @Test
    public void onDiskWithoutBackgroundThreads() throws Exception {
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        builder.setPageSize(2);
        builder.setMemMinCapacity(2);
        builder.setMemMaxCapacity(2);
        builder.setDiskMaxBytes(0);
        builder.setSerializer(serializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(0);
        builder.setShutdownHook(false);
        builder.setCompress(true);
        builder.setMemoryDouble(false);
        builder.setTerminationWait(Duration.ofMinutes(2));
        DiskBackedQueue<String> queue = builder.build();
        queue.put("hello", null);
        queue.put("world", null);
        queue.put("foo", null);
        queue.put("bar", null);
        queue.put("baz", null);
        queue.put("quux", null);
        assertTrue(filecount(path) > 0);
        assertEquals("hello", queue.poll());
        assertEquals("world", queue.poll());
        assertEquals("foo", queue.poll());
        assertEquals("bar", queue.poll());
        assertEquals("baz", queue.poll());
        assertEquals("quux", queue.poll());
        assertNull(queue.poll());
        queue.close();
        LessPaths.recursiveDelete(path);
    }

    @Test
    public void onDiskWithBackgroundThreads() throws Exception {
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        builder.setPageSize(2);
        builder.setMemMinCapacity(2);
        builder.setMemMaxCapacity(2);
        builder.setDiskMaxBytes(0);
        builder.setSerializer(serializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(0);
        builder.setShutdownHook(false);
        builder.setCompress(true);
        builder.setMemoryDouble(false);
        builder.setTerminationWait(Duration.ofMinutes(2));
        DiskBackedQueue<String> queue = builder.build();
        for(int i = 0; i < 1000; i++) {
            queue.put(Integer.toString(i), null);
        }
        assertTrue(filecount(path) > 0);
        queue.close();
        queue = builder.build();
        for(int i = 0; i < 1000; i++) {
            assertEquals(Integer.toString(i), queue.poll());
        }
        LessPaths.recursiveDelete(path);
    }

    private void drainToWithMaxElements(int maxElements) throws Exception {
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        List<String> drain = new ArrayList<>();
        builder.setPageSize(2);
        builder.setMemMinCapacity(2);
        builder.setMemMaxCapacity(2);
        builder.setDiskMaxBytes(0);
        builder.setSerializer(serializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(0);
        builder.setShutdownHook(false);
        builder.setCompress(true);
        builder.setMemoryDouble(false);
        builder.setTerminationWait(Duration.ofMinutes(2));
        DiskBackedQueue<String> queue = builder.build();
        for (int i = 0; i < ELEMENTS.size(); i++) {
            queue.put(ELEMENTS.get(i), null);
        }
        assertTrue(filecount(path) > 0);
        int drained = queue.drainTo(drain, maxElements);
        assertEquals(Math.min(ELEMENTS.size(), maxElements), drained);
        assertEquals(drained, drain.size());
        if (drained == ELEMENTS.size()) {
            assertEquals(null, queue.poll());
        } else {
            assertEquals(ELEMENTS.get(drained), queue.poll());
        }
        queue.close();
        LessPaths.recursiveDelete(path);
    }

    @Test
    public void drainToWithMaxElements() throws Exception {
        for (int i = 0; i <= (ELEMENTS.size() + 1); i++) {
            drainToWithMaxElements(i);
        }
    }

    @Test
    public void maxDiskCapacity() throws Exception {
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        builder.setPageSize(2);
        builder.setMemMinCapacity(2);
        builder.setMemMaxCapacity(2);
        builder.setDiskMaxBytes(30);
        builder.setSerializer(serializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(0);
        builder.setShutdownHook(false);
        builder.setCompress(false);
        builder.setMemoryDouble(false);
        builder.setTerminationWait(Duration.ofMinutes(2));
        DiskBackedQueue<String> queue = builder.build();
        assertTrue(queue.offer("aaaaaaaaaaaa", null));
        assertTrue(queue.offer("bbbbbbbbbbbb", null));
        assertTrue(queue.offer("cccccccccccc", null));
        assertTrue(queue.offer("dddddddddddd", null));
        assertTrue(queue.offer("eeeeeeeeeeee", null));
        assertTrue(queue.offer("ffffffffffff", null));
        assertFalse(queue.offer("gggggggggggg", null));
        assertEquals("aaaaaaaaaaaa", queue.poll());
        assertEquals("bbbbbbbbbbbb", queue.poll());
        assertEquals("cccccccccccc", queue.poll());
        assertEquals("dddddddddddd", queue.poll());
        assertEquals("eeeeeeeeeeee", queue.poll());
        assertEquals("ffffffffffff", queue.poll());
        assertNull(queue.poll());
        assertTrue(queue.offer("gggggggggggg", null));
        queue.close();
        LessPaths.recursiveDelete(path);
    }

    @Test
    public void closeAndReopenEmpty() throws Exception {
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        builder.setPageSize(1024);
        builder.setMemMinCapacity(1024);
        builder.setMemMaxCapacity(1024);
        builder.setDiskMaxBytes(0);
        builder.setSerializer(serializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(0);
        builder.setShutdownHook(false);
        builder.setCompress(true);
        builder.setMemoryDouble(false);
        builder.setTerminationWait(Duration.ofMinutes(2));
        DiskBackedQueue<String> queue = builder.build();
        assertTrue(queue.getDiskByteUsage() == 0);
        queue.close();
        assertEquals(1, filecount(path));
        assertEquals(0, queue.getDiskByteUsage());
        queue = builder.build();
        assertNull(queue.poll());
        queue.close();
        LessPaths.recursiveDelete(path);
    }

    @Test
    public void closeAndReopen() throws Exception {
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        builder.setPageSize(1024);
        builder.setMemMinCapacity(1024);
        builder.setMemMaxCapacity(1024);
        builder.setDiskMaxBytes(0);
        builder.setSerializer(serializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(0);
        builder.setShutdownHook(false);
        builder.setCompress(true);
        builder.setMemoryDouble(false);
        builder.setTerminationWait(Duration.ofMinutes(2));
        DiskBackedQueue<String> queue = builder.build();
        assertTrue(queue.getDiskByteUsage() == 0);
        queue.put("hello", null);
        queue.put("world", null);
        assertEquals(2, queue.size());
        queue.close();
        assertTrue(filecount(path) > 0);
        assertTrue(queue.getDiskByteUsage() > 0);
        queue = builder.build();
        assertEquals(2, queue.size());
        assertEquals("hello", queue.poll());
        assertEquals("world", queue.poll());
        assertNull(queue.poll());
        queue.close();
        LessPaths.recursiveDelete(path);
    }

    @Test
    public void serializableSerializer() throws Exception {
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        builder.setPageSize(1024);
        builder.setMemMinCapacity(1024);
        builder.setMemMaxCapacity(1024);
        builder.setDiskMaxBytes(0);
        builder.setSerializer(serializableSerializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(0);
        builder.setShutdownHook(false);
        builder.setCompress(true);
        builder.setMemoryDouble(false);
        builder.setTerminationWait(Duration.ofMinutes(2));
        DiskBackedQueue<String> queue = builder.build();
        assertTrue(queue.getDiskByteUsage() == 0);
        queue.put("hello", null);
        queue.put("world", null);
        assertEquals(2, queue.size());
        queue.close();
        assertTrue(filecount(path) > 0);
        assertTrue(queue.getDiskByteUsage() > 0);
        queue = builder.build();
        assertEquals(2, queue.size());
        assertEquals("hello", queue.poll());
        assertEquals("world", queue.poll());
        assertNull(queue.poll());
        queue.close();
        LessPaths.recursiveDelete(path);
    }

    private static int filecount(Path path) {
        return path.toFile().list().length;
    }

    @Test
    public void sharedExecutor() throws Exception {
        concurrentReadsWrites(4, 4, 4, 100_000, true);
    }

    @Test
    public void concurrentReadsWrites() throws Exception {
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                for (int k = 0; k <= 4; k++) {
                    concurrentReadsWrites(i, j, k, 100_000, false);
                }
            }
        }
    }

    private void concurrentReadsWrites(int numReaders, int numWriters,
                                       int numBackgroundThreads,
                                       int elements,
                                       boolean sharedSheduler) throws Exception {
        log.info("Testing disk backed queue with {} readers, " +
                 "{} writers, and {} background threads",
                 numReaders, numWriters, numBackgroundThreads);
        Path path = Files.createTempDirectory("dbq-test");
        DiskBackedQueue.Builder<String> builder = new DiskBackedQueue.Builder<>();
        builder.setPageSize(32);
        builder.setMemMinCapacity(128);
        builder.setMemMaxCapacity(512);
        builder.setDiskMaxBytes(0);
        builder.setSerializer(serializer);
        builder.setPath(path);
        builder.setNumBackgroundThreads(numBackgroundThreads);
        builder.setShutdownHook(false);
        builder.setCompress(true);
        builder.setMemoryDouble(false);
        builder.setTerminationWait(Duration.ofMinutes(2));
        builder.setSharedScheduler(sharedSheduler);
        DiskBackedQueue<String> queue = builder.build();
        Thread[] readers = new Thread[numReaders];
        Thread[] writers = new Thread[numWriters];
        AtomicInteger generator = new AtomicInteger();
        AtomicBoolean finishedWriters = new AtomicBoolean();
        WritersPhaser writersPhaser = new WritersPhaser(finishedWriters);
        ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
        for (int i = 0; i < numReaders; i++) {
            readers[i] = new Thread(new ReaderTask(values, queue, finishedWriters), "ReaderTask");
            readers[i].start();
        }
        for (int i = 0; i < numWriters; i++) {
            writers[i] = new Thread(new WriterTask(elements, generator, queue, writersPhaser), "WriterTask");
            writers[i].start();
        }
        for (int i = 0; i < numWriters; i++) {
            writers[i].join();
        }
        for (int i = 0; i < numReaders; i++) {
            readers[i].join();
        }
        assertEquals(elements, values.size());
        queue.close();
        LessPaths.recursiveDelete(path);
    }

    private static class WriterTask implements Runnable {

        private final int max;

        private final AtomicInteger generator;

        private final DiskBackedQueue<String> queue;

        private final WritersPhaser phaser;

        WriterTask(int max, AtomicInteger generator,
                   DiskBackedQueue<String> queue, WritersPhaser phaser) {
            this.max = max;
            this.generator = generator;
            this.queue = queue;
            this.phaser = phaser;
            phaser.register();
        }

        @Override public void run() {
            int next;
            try {
                while ((next = generator.getAndIncrement()) < max) {
                    queue.put(Integer.toString(next), null);
                }
            } catch (Exception ex) {
                fail(ex.toString());
            }
            phaser.arriveAndDeregister();
        }
    }

    private static class WritersPhaser extends Phaser {

        private final AtomicBoolean finishedWriters;

        WritersPhaser(AtomicBoolean finishedWriters) {
            this.finishedWriters = finishedWriters;
        }

        @Override
        public boolean onAdvance(int phase, int registeredParties) {
            finishedWriters.set(true);
            return true;
        }
    }

    private static class ReaderTask implements Runnable {

        private final ConcurrentHashMap<String,String> values;

        private final DiskBackedQueue<String> queue;

        private final AtomicBoolean finishedWriters;

        ReaderTask(ConcurrentHashMap<String,String> values,
                   DiskBackedQueue<String> queue, AtomicBoolean finishedWriters) {
            this.values = values;
            this.queue = queue;
            this.finishedWriters = finishedWriters;
        }



        @Override public void run() {
            boolean exit = false;
            try {
                while (true) {
                    String next = queue.poll();
                    if (next != null) {
                        values.put(next, next);
                    } else if (exit && (queue.diskQueueSize() == 0) && (queue.backgroundActiveTasks() == 0)) {
                        return;
                    } else if (finishedWriters.get()) {
                        exit = true;
                    }
                }
            } catch (Exception ex) {
                fail(ex.toString());
            }
        }
    }

}
