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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import com.addthis.basis.jvm.Shutdown;
import com.addthis.basis.util.LessFiles;
import com.addthis.basis.util.LessPaths;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DiskBackedQueueInternals<E> implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(DiskBackedQueue.class);

    final int pageSize;

    final int minReadPages;

    final int minWritePages;

    final int maxPages;

    final long maxDiskBytes;

    final Serializer<E> serializer;

    final Duration terminationWait;

    @Nonnull
    final GZIPOptions gzipOptions;

    final boolean silent;

    final boolean memoryDouble;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Path to the directory for storing external pages.
     * The current implementation uses {@code synchronized {external}}
     * code blocks to coordinate across consumers and producers
     * of the external files.
     */
    @GuardedBy("external")
    final Path external;

    /**
     * Defined as {@code lock.newCondition()}.
     * Wakes up any readers that are waiting for writers.
     * Is signalled whenever an element is inserted
     * into the queue or when a page is read
     * from disk.
     */
    @GuardedBy("lock")
    private final Condition notEmpty = lock.newCondition();

    /**
     * Defined as {@code lock.newCondition()}.
     * Wakes up any writers that are waiting for readers
     * to catch up. Is signalled whenever the readPage reference
     * is updated.
     */
    @GuardedBy("lock")
    private final Condition notFull = lock.newCondition();

    /**
     * Pages waiting to be evicted to disk. Any page
     * in the disk queue must not referenced by the
     * {@code writePage} or the {@code readPage}.
     */
    @Nullable
    private final ConcurrentSkipListMap<Long, Page<E>> diskQueue;

    /**
     * Estimate the current size of the diskQueue.
     */
    @Nullable
    final AtomicInteger diskQueueSize;

    /**
     * Estimate the current number of bytes stored on disk.
     */
    final AtomicLong diskByteUsage = new AtomicLong();

    /**
     * The number of items in the queue.
     */
    final AtomicLong queueSize = new AtomicLong();

    /**
     * Pages pulled from the disk and waiting to
     * placed into the {@code readPage}.
     */
    @GuardedBy("lock")
    private final NavigableMap<Long, Page<E>> readQueue = new TreeMap<>();

    @GuardedBy("lock")
    private Page<E> writePage;

    @GuardedBy("lock")
    private Page<E> readPage;

    private final AtomicReference<IOException> error = new AtomicReference<>(null);

    private final ScheduledThreadPoolExecutor executor;

    private static final ScheduledThreadPoolExecutor sharedExecutor = new ScheduledThreadPoolExecutor(
            0, new ThreadFactoryBuilder().setNameFormat("disk-backed-queue-shared-writer-%d").build());

    private final List<ScheduledFuture<?>> backgroundTasks;

    final AtomicInteger backgroundActiveTasks = new AtomicInteger();

    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final AtomicLong fastWrite = new AtomicLong();

    private final AtomicLong slowWrite = new AtomicLong();

    /**
     * To prevent a thundering herd of page loads only a single
     * thread is permitted to load at any time.
     */
    private final Semaphore loadPageSemaphore = new Semaphore(1);

    private static final Path SIZEFILE = Paths.get("size");

    private static final Predicate<Path> EXCLUDE_SIZEFILE = filepath -> !filepath.getFileName().equals(SIZEFILE);

    private static Comparator<Path> COMPARE_FILENAMES = new Comparator<Path>() {

        @Override public int compare(Path first, Path second) {
            int firstInt = Integer.parseInt(first.getFileName().toString());
            int secondInt = Integer.parseInt(second.getFileName().toString());
            int result = Integer.compare(firstInt, secondInt);
            return result;
        }
    };

    /**
     * Use {@link DiskBackedQueue.Builder} to construct a disk-backed queue.
     * Throws an exception the external directory cannot be created or opened.
     */
    DiskBackedQueueInternals(int pageSize, int minPages, int maxPages, long maxDiskBytes,
                             int numBackgroundThreads, Path path, Serializer<E> serializer,
                             Duration terminationWait, boolean shutdownHook, boolean silent,
                             boolean compress, int compressionLevel, int compressionBuffer,
                             boolean memoryDouble, boolean sharedScheduler) throws IOException {
        this.pageSize = pageSize;
        this.maxPages = maxPages;
        this.maxDiskBytes = maxDiskBytes;
        this.external = path;
        this.serializer = serializer;
        this.terminationWait = terminationWait;
        this.silent = silent;
        this.gzipOptions = new GZIPOptions(compress, compressionBuffer, compressionLevel);
        this.memoryDouble = memoryDouble;
        if (maxPages <= 1) {
            this.diskQueue = null;
            this.diskQueueSize = null;
            this.executor = null;
            this.backgroundTasks = null;
            this.minReadPages = minPages;
            this.minWritePages = 0;
        } else {
            this.diskQueue = new ConcurrentSkipListMap<>();
            this.diskQueueSize = new AtomicInteger();
            if (sharedScheduler) {
                this.executor = null;
                sharedExecutor.setCorePoolSize(Math.max(numBackgroundThreads, sharedExecutor.getCorePoolSize()));
            } else {
                this.executor = new ScheduledThreadPoolExecutor(
                        numBackgroundThreads,
                        new ThreadFactoryBuilder().setNameFormat("disk-backed-queue-writer-%d").build());
            }
            this.minReadPages = Math.max(minPages / 2, 1); // always fetch at least one page
            this.minWritePages = minPages / 2;
            this.backgroundTasks = new ArrayList<>(numBackgroundThreads);
            for (int i = 0; i < numBackgroundThreads; i++) {
                ScheduledExecutorService service = sharedScheduler ? sharedExecutor : executor;
                backgroundTasks.add(service.scheduleWithFixedDelay(new DiskWriteTask(),
                                                                    0, 10, TimeUnit.MILLISECONDS));
            }
        }
        Files.createDirectories(external);
        Optional<Path> minFile = Files.list(external).filter(EXCLUDE_SIZEFILE).min(
                (f1, f2) -> (COMPARE_FILENAMES.compare(f1, f2)));
        Optional<Path> maxFile = Files.list(external).filter(EXCLUDE_SIZEFILE).max(
                (f1, f2) -> (COMPARE_FILENAMES.compare(f1, f2)));
        if (minFile.isPresent() && maxFile.isPresent()) {
            long readPageId, writePageId;
            readPageId  = Long.parseLong(minFile.get().getFileName().toString());
            writePageId = Long.parseLong(maxFile.get().getFileName().toString()) + 1;
            NavigableMap<Long, Page<E>> readPages = readPagesFromExternal(readPageId, minReadPages);
            readPage = readPages.remove(readPageId);
            readQueue.putAll(readPages);
            writePage = new Page<>(this, writePageId);
            diskByteUsage.set(LessFiles.directorySize(external.toFile()));
            long size = Long.parseLong(new String(Files.readAllBytes(external.resolve("size"))));
            queueSize.set(size);
        } else {
            writePage = new Page<>(this, 0);
            readPage = writePage;
        }
        if (shutdownHook) {
            Shutdown.tryAddShutdownHook(new Thread(this::close, "disk-backed-queue-shutdown"));
        }
    }

    private boolean flushQueue(NavigableMap<Long, Page<E>> queue, long endTime, boolean diskQueue) throws IOException {
        if ((endTime > 0) && (System.currentTimeMillis() >= endTime)) {
            return false;
        }
        Map.Entry<Long,Page<E>> minEntry;
        while ((minEntry = queue.pollFirstEntry()) != null) {
            if (diskQueue) {
                diskQueueSize.decrementAndGet();
            }
            minEntry.getValue().writeToFile();
            signalConsumers(minEntry.getKey());
            if ((endTime > 0) && (System.currentTimeMillis() > endTime)) {
                return false;
            }
        }
        return true;
    }

    private NavigableMap<Long,Page<E>> readPagesFromExternal(long id, int count) throws IOException {
        NavigableMap<Long,Page<E>> results = new TreeMap<>();
        for(long i = id; i < (id + count); i++) {
            ByteArrayOutputStream copyStream = new ByteArrayOutputStream();
            synchronized (external) {
                Path file = external.resolve(Long.toString(i));
                if (!Files.exists(file)) {
                    return results;
                }
                InputStream inputStream = Files.newInputStream(file);
                if (gzipOptions.compress) {
                    inputStream = new GZIPInputStream(inputStream);
                }
                try {
                    ByteStreams.copy(inputStream, copyStream);
                } finally {
                    inputStream.close();
                }
                diskByteUsage.addAndGet(-Files.size(file));
                Files.delete(file);
            }
            Page<E> page = new Page<>(this, i, new ByteArrayInputStream(copyStream.toByteArray()));
            results.put(i, page);
        }
        return results;
    }

    /**
     * Load a page from the file. Returns true if
     * one or more elements are available for reading.
     */
    private boolean loadPageFromFile(long nextId) throws IOException {
        assert(lock.isHeldByCurrentThread());
        /**
         * If the page load semaphore was contested and no elements
         * were available when we reacquired the lock then retry
         * the page load semaphore until either we are the winner
         * of the page load semaphore or an element is available.
         */
        NavigableMap<Long,Page<E>> loadedPages;
        lock.unlock();
        loadPageSemaphore.acquireUninterruptibly();
        try {
            loadedPages = readPagesFromExternal(nextId, minReadPages);
        } finally {
            loadPageSemaphore.release();
            lock.lock();
        }
        if (loadedPages != null) {
            readQueue.putAll(loadedPages);
        }
        if (!readPage.empty()) {
            return true;
        }
        // lock was released and reacquired so we must recalculate nextId
        nextId = readPage.id + 1;
        if (fetchFromQueues(nextId)) {
            return true;
        }
        if ((nextId == writePage.id) && (readPage != writePage)) {
            readPage = writePage;
            notFull.signalAll();
            return !readPage.empty();
        }
        return false;
    }

    private boolean fetchFromQueues(long id) {
        boolean success = fetchFromQueue(readQueue, id, false);
        if (success) {
            return true;
        } else if (diskQueue == null) {
            return false;
        } else {
            return fetchFromQueue(diskQueue, id, true);
        }
    }

    /**
     * Retrieve a target page from either the readQueue or the diskQueue.
     * If we remove from the disk queue then we must remember to update
     * its size estimate.
     *
     * @param queue      queue to remove page
     * @param id         id of the target page
     * @param diskQueue  if true then update disk queue size
     * @return true if the target page was found
     **/
    private boolean fetchFromQueue(NavigableMap<Long,Page<E>> queue, long id, boolean diskQueue) {
        assert(lock.isHeldByCurrentThread());
        assert(readPage.empty());
        assert(id == (readPage.id + 1));
        Page<E> nextPage = queue.remove(id);
        if (nextPage != null) {
            if (diskQueue) {
                diskQueueSize.decrementAndGet();
            }
            readPage = nextPage;
            notEmpty.signalAll();
            notFull.signalAll();
            return true;
        } else {
            return false;
        }
    }

    int drainTo(Collection<? super E> collection, int maxElements) throws IOException {
        if (closed.get()) {
            throw new IllegalStateException("attempted read after close()");
        } else if (collection == null) {
            throw new NullPointerException();
        } else if (collection == this) {
            throw new IllegalArgumentException();
        } else if (maxElements <= 0) {
            return 0;
        }
        int count = 0;
        propagateError();
        lock.lock();
        try {
            while (true) {
                if (closed.get()) {
                    throw new IllegalStateException("read did not complete before close()");
                }
                long nextId = readPage.id + 1;
                if (!readPage.empty()) {
                    count = readPage.drainTo(collection, count, maxElements);
                } else if (fetchFromQueues(nextId)) {
                    count = readPage.drainTo(collection, count, maxElements);
                } else if ((nextId == writePage.id) && (readPage != writePage)) {
                    readPage = writePage;
                    notFull.signalAll();
                } else if ((nextId < writePage.id) && loadPageFromFile(nextId)) {
                    count = readPage.drainTo(collection, count, maxElements);
                } else {
                    return count;
                }
                if (count == maxElements) {
                    return count;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves an element from the end of the queue.
     * For an unbounded waiting call with {@code unit} as null.
     * To return immediately with no waiting call with {@code unit} as
     * non-null and {@code timeout} as 0.
     *
     * @param remove  If true then remove the element from the queue
     * @param timeout amount of time to wait. Ignored if {@code unit} is null.
     * @param unit    If non-null then maximum time to wait for an element.
     */
    E get(boolean remove, long timeout, TimeUnit unit) throws InterruptedException, IOException {
        if (closed.get()) {
            throw new IllegalStateException("attempted read after close()");
        }
        propagateError();
        /**
         * Follow example in
         * https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/Condition.html#awaitNanos-long-
         * to handle spurious wakeups.
         */
        long nanos = (unit == null) ? 0 : unit.toNanos(timeout);
        lock.lock();
        try {
            while (true) {
                if (closed.get()) {
                    throw new IllegalStateException("read did not complete before close()");
                }
                long nextId = readPage.id + 1;
                if (!readPage.empty()) {
                    return readPage.get(remove);
                } else if (fetchFromQueues(nextId)) {
                    return readPage.get(remove);
                } else if ((nextId == writePage.id) && (readPage != writePage)) {
                    readPage = writePage;
                    notFull.signalAll();
                } else if ((nextId < writePage.id) && loadPageFromFile(nextId)) {
                    return readPage.get(remove);
                } else if (unit == null) {
                    notEmpty.await();
                } else if (nanos <= 0L) {
                    return null;
                } else {
                    nanos = notEmpty.awaitNanos(nanos);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void testNotEmpty() {
        // signal if the new element is the next in FIFO order for the consumers
        if ((((readPage.id + 1) == writePage.id) && readPage.empty()) ||
            ((readPage == writePage) && (writePage.count == 1))) {
            notEmpty.signal();
        }
    }

    /**
     * Inserts the specified element into this queue if it is possible
     * to do so without violating disk capacity restrictions.
     *
     * @param e the element to add
     * @param bytearray optionally specify the serialized representation of e
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     * @throws IOException if error reading the backing store
     * @throws InterruptedException
     */
    boolean offer(E e, byte[] bytearray, long timeout, TimeUnit unit) throws IOException, InterruptedException {
        if (closed.get()) {
            throw new IllegalStateException("attempted write after close()");
        }
        Preconditions.checkNotNull(e);
        propagateError();
        /**
         * Follow example in
         * https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/Condition.html#awaitNanos-long-
         * to handle spurious wakeups.
         */
        long nanos = (unit == null) ? 0 : unit.toNanos(timeout);
        lock.lock();
        try {
            while(true) {
                if (closed.get()) {
                    throw new IllegalStateException("write did not complete before close()");
                }
                if (!writePage.full()) {
                    writePage.add(e, memoryDouble ? bytearray : null);
                    testNotEmpty();
                    fastWrite.getAndIncrement();
                    return true;
                } else if ((maxDiskBytes > 0) && (diskByteUsage.get() > maxDiskBytes)) {
                    if (unit == null) {
                        notFull.await();
                    } else if (nanos <= 0L) {
                        return false;
                    } else {
                        nanos = notFull.awaitNanos(nanos);
                    }
                } else {
                    Page<E> oldPage = writePage;
                    writePage = new Page<>(this, oldPage.id + 1);
                    writePage.add(e, memoryDouble ? bytearray : null);
                    if (readPage != oldPage) {
                        if ((diskQueue == null) || (diskQueueSize.get() > maxPages)) {
                            foregroundWrite(oldPage);
                            slowWrite.getAndIncrement();
                        } else {
                            Page previous = diskQueue.put(oldPage.id, oldPage);
                            assert (previous == null);
                            diskQueueSize.incrementAndGet();
                            fastWrite.getAndIncrement();
                        }
                    } else {
                        fastWrite.getAndIncrement();
                    }
                    testNotEmpty();
                    return true;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void foregroundWrite(Page<E> page) throws IOException {
        assert(lock.isHeldByCurrentThread());
        lock.unlock();
        try {
            page.writeToFile();
        } finally {
            lock.lock();
            signalConsumers(page.id);
        }
    }

    private IOException getError() {
        return error.get();
    }

    private void setError(IOException ex) {
        error.compareAndSet(null, ex);
    }

    private void propagateError() throws IOException {
        IOException ex = error.get();
        if (ex != null) {
            throw ex;
        }
    }

    public void clear() throws IOException {
        lock.lock();
        try {
            writePage = new Page<>(this, 0);
            readPage = writePage;
            queueSize.set(0);
            if (diskQueue != null) {
                diskQueue.clear();
            }
            if (diskQueueSize != null) {
                diskQueueSize.set(0);
            }
            synchronized (external) {
                LessPaths.recursiveDelete(external);
                Files.createDirectories(external);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override public void close() {
        if (!closed.getAndSet(true)) {
            long startTime = System.currentTimeMillis();
            long endTime = startTime + terminationWait.toMillis();
            try {
                if (backgroundTasks != null) {
                    for (ScheduledFuture<?> future : backgroundTasks) {
                        future.cancel(true);
                    }
                }
                if (executor != null) {
                    executor.shutdown();
                    try {
                        if (!silent) {
                            log.info("Waiting on background threads to write approximately {} pages",
                                     diskQueueSize.get());
                        }
                        executor.awaitTermination(terminationWait.toMillis(), TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ex) {
                        closeFuture.completeExceptionally(ex);
                        Throwables.propagate(ex);
                    } finally {
                        executor.shutdownNow();
                    }
                }
                int unwritten = calculateDirtyPageCount();
                if (!silent) {
                    log.info("Foreground thread must write approximately {} pages", unwritten);
                }
                boolean hasTime = (System.currentTimeMillis() < endTime);
                if (hasTime && !readPage.empty()) {
                    lock.lock();
                    try {
                        foregroundWrite(readPage);
                        readPage.clear();
                    } finally {
                        lock.unlock();
                    }
                }
                hasTime = (System.currentTimeMillis() < endTime);
                if (hasTime) {
                    hasTime = flushQueue(readQueue, endTime, false);
                }
                if (hasTime && (diskQueue != null)) {
                    hasTime = flushQueue(diskQueue, endTime, true);
                }
                if (hasTime && !writePage.empty()) {
                    lock.lock();
                    try {
                        foregroundWrite(writePage);
                        writePage.clear();
                    } finally {
                        lock.unlock();
                    }
                }
                long writeSize = queueSize.get();
                unwritten = calculateDirtyPageCount();
                if (unwritten > 0) {
                    log.warn("Closing of disk-backed queue timed out before writing all pages to disk. " +
                         "Approximately {} pages were not written to disk.", unwritten);
                    writeSize -= unwritten * pageSize;
                }
                Files.write(external.resolve("size"), Long.toString(writeSize).getBytes());
                IOException previous = error.get();
                if (previous != null) {
                    closeFuture.completeExceptionally(previous);
                    Throwables.propagate(previous);
                } else {
                    closeFuture.complete(null);
                }
            } catch (IOException ex) {
                closeFuture.completeExceptionally(ex);
                Throwables.propagate(ex);
            }
        } else {
            try {
                closeFuture.get();
            } catch (InterruptedException|ExecutionException ex) {
                throw Throwables.propagate(ex);
            }
        }
    }

    public long getDiskByteUsage() { return diskByteUsage.get(); }

    public long size() { return queueSize.get(); }

    public Path getPath() { return external; }

    public long getFastWrite() { return fastWrite.get(); }

    public long getSlowWrite() { return slowWrite.get(); }

    private int calculateDirtyPageCount() {
        int remaining = (readPage.empty() ? 0 : 1);
        remaining += readQueue.size();
        if (diskQueueSize != null) {
            remaining += diskQueueSize.get();
        }
        if ((readPage != writePage) && !writePage.empty()) {
            remaining += 1;
        }
        return remaining;
    }

    private class DiskWriteTask implements Runnable {

        @Override
        public void run() {
            backgroundActiveTasks.getAndIncrement();
            try {
                while ((getError() == null) && (diskQueueSize.get() > minWritePages)) {
                    Map.Entry<Long, Page<E>> minEntry = diskQueue.pollFirstEntry();
                    if (minEntry != null) {
                        diskQueueSize.decrementAndGet();
                        minEntry.getValue().writeToFile();
                        signalConsumers(minEntry.getKey());
                    } else {
                        break;
                    }
                    if (Thread.interrupted()) {
                        break;
                    }
                }
            } catch (IOException ex) {
                setError(ex);
            } finally {
                backgroundActiveTasks.getAndDecrement();
            }
        }
    }

    private void signalConsumers(long id) {
        lock.lock();
        try {
            if (readPage.empty() && ((readPage.id + 1) == id)) {
                notEmpty.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    static class GZIPOptions {
        final boolean compress;
        final int compressionBuffer;
        final int compressionLevel;

        public GZIPOptions(boolean compress, int compressionBuffer, int compressionLevel) {
            this.compress = compress;
            this.compressionBuffer = compressionBuffer;
            this.compressionLevel = compressionLevel;
        }
    }

}
