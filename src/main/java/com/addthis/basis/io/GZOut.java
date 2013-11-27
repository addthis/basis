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
package com.addthis.basis.io;

import java.io.IOException;
import java.io.OutputStream;

import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * GZ output with control over compression level
 */
public final class GZOut extends DeflaterOutputStream {
    /**
     * CRC-32 of uncompressed data.
     */
    protected CRC32 crc = new CRC32();

    /**
     * GZIP header magic number.
     */
    private static final int GZIP_MAGIC = 0x8b1f;

    /**
     * Trailer size in bytes.
     */
    private static final int TRAILER_SIZE = 8;

    private boolean closed = false;

    /**
     * Creates a new output stream with the specified buffer size.
     *
     * @param out         the output stream
     * @param buffer      the output buffer size
     * @param compression Deflater compression level
     * @throws java.io.IOException      If an I/O error has occurred.
     * @throws IllegalArgumentException if size is <= 0
     */
    public GZOut(OutputStream out, int buffer, int compression) throws IOException {
        super(out, new Deflater(compression, true), buffer);
        writeHeader();
        crc.reset();
    }

    /**
     * Creates a new output stream with a default buffer size.
     *
     * @param out the output stream
     * @throws java.io.IOException If an I/O error has occurred.
     */
    public GZOut(OutputStream out) throws IOException {
        this(out, 512, Deflater.DEFAULT_COMPRESSION);
    }

    /**
     * Writes array of bytes to the compressed output stream. This method will
     * block until all the bytes are written.
     *
     * @param buf the data to be written
     * @param off the start offset of the data
     * @param len the length of the data
     * @throws java.io.IOException If an I/O error has occurred.
     */
    public synchronized void write(byte[] buf, int off, int len) throws IOException {
        super.write(buf, off, len);
        crc.update(buf, off, len);
    }

    /**
     * Finishes writing compressed data to the output stream without closing the
     * underlying stream. Use this method when applying multiple filters in
     * succession to the same output stream.
     *
     * @throws java.io.IOException if an I/O error has occurred
     */
    public void finish() throws IOException {
        if (!def.finished()) {
            def.finish();
            while (!def.finished()) {
                int len = def.deflate(buf, 0, buf.length);
                if (def.finished() && len <= buf.length - TRAILER_SIZE) {
                    // last deflater buffer. Fit trailer at the end
                    writeTrailer(buf, len);
                    len = len + TRAILER_SIZE;
                    out.write(buf, 0, len);
                    return;
                }
                if (len > 0) {
                    out.write(buf, 0, len);
                }
            }
            // if we can't fit the trailer at the end of the last
            // deflater buffer, we write it separately
            byte[] trailer = new byte[TRAILER_SIZE];
            writeTrailer(trailer, 0);
            out.write(trailer);
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            finish();
            def.end();
            super.close();
            closed = true;
        }
    }

    /**
     * Writes GZIP member header.
     */
    private static final byte[] header = {(byte) GZIP_MAGIC, // Magic number
            // (short)
            (byte) (GZIP_MAGIC >> 8), // Magic number (short)
            Deflater.DEFLATED, // Compression method (CM)
            0, // Flags (FLG)
            0, // Modification time MTIME (int)
            0, // Modification time MTIME (int)
            0, // Modification time MTIME (int)
            0, // Modification time MTIME (int)
            0, // Extra flags (XFLG)
            0 // Operating system (OS)
    };

    private void writeHeader() throws IOException {
        out.write(header);
    }

    /**
     * Writes GZIP member trailer to a byte array, starting at a given offset.
     */
    private void writeTrailer(byte[] buf, int offset) {
        writeInt((int) crc.getValue(), buf, offset); // CRC-32 of uncompr. data
        writeInt(def.getTotalIn(), buf, offset + 4); // Number of uncompr. bytes
    }

    /**
     * Writes integer in Intel byte order to a byte array, starting at a given
     * offset.
     */
    private void writeInt(int i, byte[] buf, int offset) {
        writeShort(i & 0xffff, buf, offset);
        writeShort((i >> 16) & 0xffff, buf, offset + 2);
    }

    /**
     * Writes short integer in Intel byte order to a byte array, starting at a
     * given offset
     */
    private void writeShort(int s, byte[] buf, int offset) {
        buf[offset] = (byte) (s & 0xff);
        buf[offset + 1] = (byte) ((s >> 8) & 0xff);
    }
}
