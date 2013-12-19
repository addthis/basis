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

import java.util.zip.GZIPOutputStream;

/**
 * GZIPOutputStream with control over compression level
 */
public final class GZOut extends GZIPOutputStream {

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
        super(out, buffer);
        setLevel(compression);
    }

    /**
     * Change the compression level.
     * @param newLevel new compression level to use
     */
    public void setLevel (int newLevel) {
        def.setLevel(newLevel);
    }
}
