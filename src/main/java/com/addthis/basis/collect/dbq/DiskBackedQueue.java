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

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import java.nio.file.Path;
import java.time.Duration;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;

/**
 * Thread-safe FIFO queue that writes overflow elements to disk. If memory capacity is not exceeded
 * then all operations occur in memory. {@link #put( Object , byte[])}
 * requests are designed to return quickly and write to disk
 * asynchronously if one or more background threads have been configured.
 * All requests of the {@link DiskBackedQueueInternals#get(boolean, long, TimeUnit)}
 * family perform synchronous reads from the disk. A {@link #close()}
 * will write the contents of the queue to the external storage.
 *
 * <p>This implements a disk-backed queue however it is possible to set a maximum
 * capacity on disk using the {@link Builder#setDiskMaxBytes(long)} parameter.
 * If the maximum disk capacity is exceeded then insertion operations will either
 * block or throw an exception depending on whether the blocking or non-blocking
 * operation is invoked. The disk capacity restriction can be removed by setting the
 * disk max bytes to 0.
 *
 * <p>The class exposes the API of the data structure and the Builder class to construct
 * instances. Nearly all parameter are required to the Builder object.
 * This is a deliberate design decision to discourage a reliance of magical
 * default values that lead to behavior surprises for your application.
 * Refer to {@link DiskBackedQueueInternals} for the
 * the implementation.
 */
public class DiskBackedQueue<E> extends AbstractQueue<E> implements Closeable, Queue<E> {

    private final DiskBackedQueueInternals<E> queue;

    private DiskBackedQueue(DiskBackedQueueInternals<E> queue) {
        this.queue = queue;
    }

    public static class Builder<E> {
        private int pageSize = -1;
        private int memMinCapacity = -1;
        private int memMaxCapacity = -1;
        private long diskMaxBytes = -1;
        private int numBackgroundThreads = -1;
        private int compressionLevel = 9;
        private int compressionBuffer = 1024;
        private boolean memoryDouble = false;
        private boolean sharedScheduler = false;
        private Path path;
        private Serializer<E> serializer;
        private Duration terminationWait;
        private Boolean shutdownHook;
        private Boolean compress;

        // optional
        private boolean silent;

        /**
         * Number of elements that are stored per page. Larger
         * pages amortize the cost of writing to disk but very large
         * pages limit the concurrency of background writes. Suggested
         * values are in the range from 32 to 4096. This parameter is required.
         */
        public Builder<E> setPageSize(int size) {
            this.pageSize = size;
            return this;
        }

        /**
         * Minimum number of elements that are allowed to be stored
         * in memory. An implementation note: half of these elements
         * will be reserved for the reading queue, and half will be
         * reserved fo the write queue. Suggested values are a 10x to 100x
         * multiple of the page size. The value muse be greater than
         * or equal to page size. This parameter is required.
         */
        public Builder<E> setMemMinCapacity(int capacity) {
            this.memMinCapacity = capacity;
            return this;
        }

        /**
         * Maximum number of elements that are allowed to be stored
         * in memory before insertion operations begin writing
         * synchronously to write to disk. The value muse be greater than
         * or equal to memory minimum capacity. This parameter is required.
         */
        public Builder<E> setMemMaxCapacity(int capacity) {
            this.memMaxCapacity = capacity;
            return this;
        }

        /**
         * Maximum number of bytes that are allowed to be stored
         * on disk. Set to 0 to specify no upper bound. This parameter is required.
         */
        public Builder<E> setDiskMaxBytes(long bytes) {
            this.diskMaxBytes = bytes;
            return this;
        }

        /**
         * Number of background threads for asynchronous writes to disk.
         * If 0 then all writes are synchronous. This parameter is required.
         */
        public Builder<E> setNumBackgroundThreads(int threads) {
            this.numBackgroundThreads = threads;
            return this;
        }

        /**
         * Path to the directory for storing external pages. This parameter is required.
         */
        public Builder<E> setPath(Path path) {
            this.path = path;
            return this;
        }

        /**
         * Serializer for reading/writing elements to/from disk.
         * This parameter is required.
         */
        public Builder<E> setSerializer(Serializer<E> serializer) {
            this.serializer = serializer;
            return this;
        }

        /**
         * Time interval to wait for any outstanding writes to be
         * flushed to disk when queue is closed. This parameter is required.
         */
        public Builder<E> setTerminationWait(Duration wait) {
            this.terminationWait = wait;
            return this;
        }

        /**
         * Whether or not to create a shutdown hook that will
         * close the disk backed queue on JVM shutdown.
         * This parameter is required.
         */
        public Builder<E> setShutdownHook(boolean hook) {
            this.shutdownHook = hook;
            return this;
        }

        /**
         * If true then enable gzip compression of the external storage.
         * This parameter is required.
         */
        public Builder<E> setCompress(boolean compress) {
            this.compress = compress;
            return this;
        }

        /**
         * If true then store the serialized representation of objects
         * along with the objects themselves. This improves
         * disk-writing performance at the cost of additional memory overhead.
         * This parameter is required. Memory doubling is ignored
         * when {@link #put(Object, byte[])} is called with a null byte array.
         * This parameter is optional. Default is false.
         */
        public Builder<E> setMemoryDouble(boolean enable) {
            this.memoryDouble = enable;
            return this;
        }

        /**
         * If compression is enabled then this parameter sets the gzip
         * compression level. This parameter is optional. Default is 9.
         */
        public Builder<E> setCompressionLevel(int compressionLevel) {
            this.compressionLevel = compressionLevel;
            return this;
        }

        /**
         * If compression is enabled then this parameter sets the gzip
         * compression buffer size in bytes. This parameter is optional. Default is 1024.
         */
        public Builder<E> setCompressionBuffer(int compressionBuffer) {
            this.compressionBuffer = compressionBuffer;
            return this;
        }

        /**
         * If enabled then this instance will use the shared scheduler for
         * background threads. This parameter is optional. Default is false.
         * The number of threads in the shared scheduler will be set to the
         * maximum of the previous value and the number specified in this builder.
         */
        public Builder<E> setSharedScheduler(boolean enable) {
            this.sharedScheduler = enable;
            return this;
        }

        /**
         * If true then do not print informational log messages.
         * Convenience method the same functionality can be
         * configured from the logger. This parameter is optional.
         */
        public Builder<E> setSilent(boolean silence) {
            this.silent = silence;
            return this;
        }

        public DiskBackedQueue<E> build() throws IOException {
            Preconditions.checkArgument(pageSize > 0, "pageSize must be > 0");
            Preconditions.checkArgument(memMinCapacity > 0, "memMinCapacity must be > 0");
            Preconditions.checkArgument(memMaxCapacity > 0, "memMaxCapacity must be > 0");
            Preconditions.checkArgument(diskMaxBytes >= 0, "diskMaxBytes must be >= 0");
            Preconditions.checkArgument(numBackgroundThreads >= 0, "numBackgroundThreads must be >= 0");
            Preconditions.checkNotNull(path, "path must be non-null");
            Preconditions.checkNotNull(serializer, "serializer must be non-null");
            Preconditions.checkArgument(memMaxCapacity >= pageSize, "memMaxCapacity must be >= pageSize");
            Preconditions.checkArgument(memMinCapacity <= memMaxCapacity, "memMinCapacity must be <= memMaxCapacity");
            Preconditions.checkNotNull(terminationWait, "terminationWait must be specified");
            Preconditions.checkNotNull(shutdownHook, "shutdownHook usage must be specified");
            Preconditions.checkNotNull(compress, "compress usage must be specified");
            Preconditions.checkArgument(compressionLevel >= 0 && compressionLevel <= 9, "compression level must be between 0 and 9");
            Preconditions.checkArgument(compressionBuffer > 0, "compression buffer must greater than 0");
            return new DiskBackedQueue<>(
                    new DiskBackedQueueInternals<>(pageSize, memMinCapacity / pageSize,
                                                   memMaxCapacity / pageSize,
                                                   diskMaxBytes,
                                                   numBackgroundThreads, path, serializer,
                                                   terminationWait, shutdownHook, silent, compress,
                                                   compressionLevel, compressionBuffer, memoryDouble,
                                                   sharedScheduler));
        }
    }

    /**
     * {@inheritDoc}
     * @throws UncheckedIOException if error reading from or writing to the backing store
     */
    @Override
    public boolean offer(E e) {
        return offer(e, null);
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     * @throws UncheckedIOException if error reading from or writing to the backing store
     */
    @Override
    public E poll() throws UncheckedIOException {
        try {
            return queue.get(true, 0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            // This should never happen. The call was nonblocking.
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * {@inheritDoc}
     * @throws UncheckedIOException if error reading from or writing to the backing store
     */
    @Override
    public E peek() {
        try {
            return queue.get(false, 0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            // This should never happen. The call was nonblocking.
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the head of this queue, or {@code null} if the
     *         specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     * @throws UncheckedIOException if error reading from or writing to the backing store
     */
    public E poll(long timeout, TimeUnit unit) throws UncheckedIOException, InterruptedException {
        Preconditions.checkNotNull(unit);
        try {
            return queue.get(true, timeout, unit);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     * @throws UncheckedIOException if error reading from or writing to the backing store
     */
    public E take() throws UncheckedIOException, InterruptedException {
        try {
            return queue.get(true, 0, null);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Inserts the specified element into this queue,
     * waiting if necessary for disk capacity restrictions to be met.
     * The {@code data} parameter is ignored if memory doubling is disabled.
     *
     * @param e the element to add
     * @param data optionally specify the serialized representation of e
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     * @throws InterruptedException if interrupted while waiting
     * @throws UncheckedIOException if error reading from or writing to the backing store
     */
    public void put(@Nonnull E e, @Nullable byte[] data) throws UncheckedIOException, InterruptedException {
        try {
            queue.offer(e, data, 0, null);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Inserts the specified element into this queue if it is possible
     * to do so without violating disk capacity restrictions.
     * The {@code data} parameter is ignored if memory doubling is disabled.
     *
     * @param e the element to add
     * @param data optionally specify the serialized representation of e
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     * @throws UncheckedIOException if error reading from or writing to the backing store
     */
    public boolean offer(@Nonnull E e, @Nullable byte[] data) throws UncheckedIOException {
        try {
            return queue.offer(e, data, 0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            // This should never happen. The call was nonblocking.
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Inserts the specified element into this queue, waiting up to the specified
     * wait time if disk capacity has been exceeded. The {@code data} parameter is
     * ignored if memory doubling is disabled.
     *
     * @param e the element to add
     * @param data optionally specify the serialized representation of e
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     * @throws InterruptedException if interrupted while waiting
     * @throws UncheckedIOException if error reading from or writing to the backing store
     */
    public boolean offer(@Nonnull E e, @Nullable byte[] data, long timeout, TimeUnit unit) throws UncheckedIOException,
                                                                                                  InterruptedException {
        Preconditions.checkNotNull(unit);
        try {
            return queue.offer(e, data, timeout, unit);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public long getDiskByteUsage() { return queue.getDiskByteUsage(); }

    public Path getPath() { return queue.getPath(); }

    @Override
    public void close() {
        queue.close();
    }

    /**
     * {@inheritDoc}
     * @throws UncheckedIOException if error reading from or writing to the backing store
     */
    @Override
    public void clear() {
        try {
            queue.clear();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public double fastToSlowWriteRatio() {
        long fastWrite = queue.getFastWrite();
        long slowWrite = queue.getSlowWrite();
        return ((double) fastWrite) / (slowWrite);
    }

    @Override public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override public int size() {
        return Ints.saturatedCast(queue.size());
    }

    @VisibleForTesting
    int diskQueueSize() {
        if (queue.diskQueueSize == null) {
            return 0;
        } else {
            return queue.diskQueueSize.get();
        }
    }

    @VisibleForTesting
    int backgroundActiveTasks() {
        return queue.backgroundActiveTasks.get();
    }
}
