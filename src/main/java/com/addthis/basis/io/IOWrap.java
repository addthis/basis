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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.zip.GZIPOutputStream;


public final class IOWrap {
    /** */
    public static InputStream buffer(InputStream in, int buffer) {
        return buffer > 0 ? new BufferedInputStream(in, buffer) : in;
    }

    /** */
    public static InputStream gz(InputStream in, int buffer) {
        try {
            return buffer > 0 ? new GZIPInputStreamX(in, buffer) : new GZIPInputStreamX(in);
        } catch (IOException e) {
            e.printStackTrace();
            return in;
        }
    }

    /** */
    public static OutputStream buffer(OutputStream out, int buffer) {
        return buffer > 0 ? new BufferedOutputStream(out) : out;
    }

    /** */
    public static OutputStream gz(OutputStream in, int buffer) {
        try {
            return buffer > 0 ? new GZIPOutputStream(in, buffer) : new GZIPOutputStream(in);
        } catch (IOException e) {
            e.printStackTrace();
            return in;
        }
    }

    /**
     * @throws FileNotFoundException
     */
    public static InputStream fileIn(File file, int buffer, boolean gz) throws FileNotFoundException {
        return gz ? gz(new FileInputStream(file), buffer) : buffer(new FileInputStream(file), buffer);
    }

    /**
     * @throws FileNotFoundException
     */
    public static OutputStream fileOut(File file, int buffer, boolean gz, boolean append) throws FileNotFoundException {
        return gz ? gz(new FileOutputStream(file, append), buffer) : buffer(new FileOutputStream(file, append), buffer);
    }
}
