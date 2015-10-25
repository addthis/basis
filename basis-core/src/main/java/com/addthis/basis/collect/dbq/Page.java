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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import java.nio.file.Files;
import java.nio.file.Path;

import com.addthis.basis.io.GZOut;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;

/**
 * Fixed length circular buffer of elements.
 */
class Page<E> {

    private static class ObjectByteArrayPair<E> {
        @Nonnull final E value;
        @Nullable final byte[] bytearray;

        ObjectByteArrayPair(@Nonnull E value, @Nullable byte[] bytearray) {
            this.value = value;
            this.bytearray = bytearray;
        }
    }

    final long id;

    @Nonnull
    final ObjectByteArrayPair[] elements;

    final int pageSize;

    @Nonnull
    final Serializer<E> serializer;

    @Nonnull
    final AtomicLong diskByteUsage;

    @Nonnull
    final AtomicLong queueSize;

    @Nonnull
    final DiskBackedQueueInternals.GZIPOptions gzipOptions;

    @GuardedBy("external")
    private final Path external;

    int readerIndex;

    int writerIndex;

    int count;

    Page(DiskBackedQueueInternals<E> queue, long id, InputStream stream) throws IOException {
        try {
            this.id = id;
            this.pageSize = queue.pageSize;
            this.serializer = queue.serializer;
            this.diskByteUsage = queue.diskByteUsage;
            this.queueSize = queue.queueSize;
            this.gzipOptions = queue.gzipOptions;
            this.external = queue.external;
            this.elements = new ObjectByteArrayPair[pageSize];
            this.count = readInt(stream);
            for (int i = 0; i < count; i++) {
                ObjectByteArrayPair<E> pair = new ObjectByteArrayPair<>(serializer.fromInputStream(stream), null);
                elements[i] = pair;
            }
            this.readerIndex = 0;
            this.writerIndex = count;
        } finally {
            stream.close();
        }
    }

    Page(DiskBackedQueueInternals<E> queue, long id) {
        this.id = id;
        this.pageSize = queue.pageSize;
        this.serializer = queue.serializer;
        this.diskByteUsage = queue.diskByteUsage;
        this.queueSize = queue.queueSize;
        this.gzipOptions = queue.gzipOptions;
        this.external = queue.external;
        this.elements = new ObjectByteArrayPair[pageSize];
        this.count = 0;
        this.readerIndex = 0;
        this.writerIndex = 0;
    }

    boolean empty() {
        return (count == 0);
    }

    boolean full() {
        return (count == pageSize);
    }

    void add(E e, byte[] bytearray) {
        assert(!full());
        elements[writerIndex] = new ObjectByteArrayPair<>(e, bytearray);
        writerIndex = (writerIndex + 1) % pageSize;
        count++;
        queueSize.getAndIncrement();
    }

    void clear() {
        count = 0;
        readerIndex = 0;
        writerIndex = 0;
    }

    int drainTo(Collection<? super E> collection, int drainCount, int maxElements) {
        assert(!empty());
        while ((count > 0) && (drainCount < maxElements)) {
            collection.add((E) elements[readerIndex].value);
            readerIndex = (readerIndex + 1) % pageSize;
            queueSize.getAndDecrement();
            count--;
            drainCount++;
        }
        return drainCount;
    }

    E get(boolean remove) {
        assert(!empty());
        ObjectByteArrayPair<E> result = elements[readerIndex];
        if (remove) {
            readerIndex = (readerIndex + 1) % pageSize;
            count--;
            queueSize.getAndDecrement();
        }
        return result.value;
    }

    void writeToFile() throws IOException {
        assert(!empty());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os;
        if (gzipOptions.compress) {
            os = new GZOut(baos, gzipOptions.compressionBuffer, gzipOptions.compressionLevel);
        } else {
            os = baos;
        }
        writeInt(os, count);
        for (int i = 0; i < count; i++) {
            ObjectByteArrayPair<E> next = elements[(readerIndex + i) % pageSize];
            if (next.bytearray != null) {
                os.write(next.bytearray);
            } else {
                serializer.toOutputStream(next.value, os);
            }
        }
        if (gzipOptions.compress) {
            ((GZOut) os).finish();
        }
        os.flush();
        synchronized (external) {
            Path file = external.resolve(Long.toString(id));
            assert(!Files.exists(file));
            OutputStream outputStream = Files.newOutputStream(file);
            try {
                byte[] bytes = baos.toByteArray();
                outputStream.write(bytes);
                diskByteUsage.getAndAdd(bytes.length);
            } finally {
                outputStream.close();
            }

        }
    }

    @VisibleForTesting
    static int readInt(InputStream stream) throws IOException {
        byte[] data = new byte[4];
        ByteStreams.readFully(stream, data);
        return Ints.fromByteArray(data);
    }

    @VisibleForTesting
    static void writeInt(OutputStream stream, int val) throws IOException {
        byte[] data = Ints.toByteArray(val);
        stream.write(data);
    }
}
