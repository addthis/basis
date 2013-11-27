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
import java.io.InputStream;
import java.io.PushbackInputStream;

import java.util.zip.GZIPInputStream;

/**
 * Extends GZIPInputStream to handle multiple catted streams.
 * Unknown performance characteristics.
 */
public class GZIPInputStreamX extends GZIPInputStream {

    public GZIPInputStreamX(InputStream in, int size) throws IOException {
        // Wrap the stream in a PushbackInputStream...
        super(new PushbackInputStream(in, size), size);
        this.size = size;
    }

    public GZIPInputStreamX(InputStream in) throws IOException {
        // Wrap the stream in a PushbackInputStream...
        super(new PushbackInputStream(in, 2048));
        this.size = -1;
    }

    private GZIPInputStreamX(GZIPInputStreamX newparent) throws IOException {
        super(newparent.in);
        setParent(newparent);
        this.size = -1;
    }

    private GZIPInputStreamX(GZIPInputStreamX newparent, int size) throws IOException {
        super(newparent.in, size);
        setParent(newparent);
        this.size = size;
    }

    private void setParent(GZIPInputStreamX newparent) {
        while (newparent.parent != null) {
            newparent = newparent.parent;
        }
        this.parent = newparent;
        this.parent.child = this;
    }

    private GZIPInputStreamX parent, child;
    private int size;
    private boolean eos;

    public int read(byte[] inbuf, int inoff, int inlen) throws IOException {
        if (eos) {
            return -1;
        }
        if (this.child != null) {
            return this.child.read(inbuf, inoff, inlen);
        }

        int read = 0;
        try {
            read = super.read(inbuf, inoff, inlen);
        } catch (IOException e) {
            /* workaround for jdk 1.5.0_07 and earlier 2GB limit bug */
            if (e.getMessage().contains("Corrupt GZIP trailer")) {
                read = -1;
            } else {
                throw e;
            }
        }
        if (read == -1) {
            // Push any remaining buffered data back onto the stream
            // If the stream is then not empty, use it to construct
            // a new instance of this class and delegate this and any
            // future calls to it...
            int n = inf.getRemaining() - 8;
            if (n > 0) {
                // More than 8 bytes remaining in deflater
                // First 8 are gzip trailer. Add the rest to
                // any un-read data...
                ((PushbackInputStream) this.in).unread(buf, len - n, n);
            } else {
                // Nothing in the buffer. We need to know whether or not
                // there is unread data available in the underlying stream
                // since the base class will not handle an empty file.
                // Read a byte to see if there is data and if so,
                // push it back onto the stream...
                byte[] b = new byte[1];
                int ret = in.read(b, 0, 1);
                if (ret == -1) {
                    eos = true;
                    return -1;
                } else {
                    ((PushbackInputStream) this.in).unread(b, 0, 1);
                }
            }

            GZIPInputStreamX child = this.size == -1 ? new GZIPInputStreamX(this) : new GZIPInputStreamX(this, this.size);
            return child.read(inbuf, inoff, inlen);
        } else {
            return read;
        }
    }
}
