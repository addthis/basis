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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.Arrays;

import java.nio.charset.Charset;

public class Bytes {

    public static final Charset UTF8 = Charset.forName("UTF-8");
    private static final byte[] emptyBytes = new byte[0];

    /**
     * Efficiently concatenate two byte arrays into one.
     *
     * @param a
     * @param b
     * @return
     */
    public static byte[] cat(byte[] a, byte[] b) {
        byte[] o = new byte[a.length + b.length];
        System.arraycopy(a, 0, o, 0, a.length);
        System.arraycopy(b, 0, o, a.length, b.length);
        return o;
    }

    /**
     * Efficiently concatenate three byte arrays into one.
     *
     * @param a
     * @param b
     * @return
     */
    public static byte[] cat(byte[] a, byte[] b, byte[] c) {
        byte[] o = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, o, 0, a.length);
        System.arraycopy(b, 0, o, a.length, b.length);
        System.arraycopy(c, 0, o, a.length + b.length, c.length);
        return o;
    }

    /**
     * Efficiently concatenate four byte arrays into one.
     *
     * @param a
     * @param b
     * @return
     */
    public static byte[] cat(byte[] a, byte[] b, byte[] c, byte[] d) {
        byte[] o = new byte[a.length + b.length + c.length + d.length];
        System.arraycopy(a, 0, o, 0, a.length);
        System.arraycopy(b, 0, o, a.length, b.length);
        System.arraycopy(c, 0, o, a.length + b.length, c.length);
        System.arraycopy(d, 0, o, a.length + b.length + c.length, d.length);
        return o;
    }

    /**
     * Efficiently concatenate five byte arrays into one.
     *
     * @param a
     * @param b
     * @return
     */
    public static byte[] cat(byte[] a, byte[] b, byte[] c, byte[] d, byte[] e) {
        byte[] o = new byte[a.length + b.length + c.length + d.length + e.length];
        System.arraycopy(a, 0, o, 0, a.length);
        System.arraycopy(b, 0, o, a.length, b.length);
        System.arraycopy(c, 0, o, a.length + b.length, c.length);
        System.arraycopy(d, 0, o, a.length + b.length + c.length, d.length);
        System.arraycopy(e, 0, o, a.length + b.length + c.length + d.length, e.length);
        return o;
    }

    /**
     * replaces bytes in buf[] starting at pat[] with rep[].  replaces pat[].
     * example: 'string 1234 foo bar dude', '1234', 'this is a test' yields
     * returns: 'string this is a test foo bar dude'
     */
    public static byte[] replace(byte[] buf, byte[] pat, byte[] rep) {
        int scanpos = 0;
        int startoff = 0;
        while (scanpos < buf.length) {
            if (buf[scanpos] == pat[startoff]) {
                if (++startoff == pat.length) {
                    // replace @ scanpos - startoff
                    byte[] out = new byte[buf.length - pat.length + rep.length];
                    Arrays.fill(out, (byte) '-');
                    System.arraycopy(buf, 0, out, 0, scanpos - startoff + 1);
                    System.arraycopy(rep, 0, out, scanpos - startoff + 1, rep.length);
                    System.arraycopy(buf, scanpos + 1, out, scanpos - startoff + rep.length + 1, out.length - scanpos - rep.length + pat.length - 1);
                    return out;
                }
            } else {
                startoff = 0;
            }
            scanpos++;
        }
        return buf;
    }

    /**
     * overwrites bytes in buf[] starting at pat[] with rep[].
     * example: 'string 1234 foo bar dude', '1234', 'this is a test' yields
     * returns: 'string this is a testr dude'
     */
    public static boolean overwrite(byte[] buf, byte[] pat, byte[] rep) {
        for (int i = 0; i < buf.length; i++) {
            for (int j = 0; j < pat.length && i + j < buf.length && buf[i + j] == pat[j]; j++) {
                if (j == pat.length - 1) {
                    for (int r = 0; r < rep.length && i + r < buf.length; r++) {
                        buf[i + r] = rep[r];
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the first byte array starts with the second.
     * Analogous to String.startsWith(String).
     *
     * @param data
     * @param prefix
     * @return
     */
    public static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length >= prefix.length) {
            for (int i = 0; i < prefix.length; i++) {
                if (data[i] != prefix[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static long toUnsignedInt(int i) {
        return (((i >> 16) & 0xffffL) << 16) | (i & 0xffffL);
    }

    /**
     * Create a byte array from a String using UTF-8.
     *
     * @param s
     * @return
     */
    public static byte[] toBytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return s.getBytes();
        }
    }

    /**
     * Create an array of byte arrays from an array of Strings using UTF-8
     *
     * @param strings
     * @return
     */
    public static byte[][] toByteArrays(String[] strings) {
        byte[][] bytes = new byte[strings.length][0];
        for (int i = 0; i < strings.length; i++) {
            bytes[i] = strings[i].getBytes(UTF8);
        }
        return bytes;
    }

    /**
     * create an array of Strings from an array of byte arrays using UTF-8
     *
     * @param bytes
     * @return
     */
    public static String[] toStrings(byte[][] bytes) {
        String[] strings = new String[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            strings[i] = new String(bytes[i], UTF8);
        }

        return strings;
    }

    /**
     * Convert an short to a byte[2].
     *
     * @param val
     * @return
     */
    public static byte[] toBytes(short val) {
        byte[] data = new byte[2];
        data[0] = (byte) ((val & 0xFF00) >> 8);
        data[1] = (byte) ((val & 0x00FF) >> 0);
        return data;
    }

    /**
     * Convert char array to byte array
     *
     * @param c
     * @return
     */
    public static byte[] toBytes(char[] c) {
        byte[] b = new byte[c.length * 2];
        for (int i = 0, j = 0; i < c.length; i++) {
            b[j] = (byte) ((c[i] >> 8) & 0xff);
            b[j + 1] = (byte) (c[i] & 0xff);
            j += 2;
        }
        return b;
    }

    /**
     * Convert an int array to bytes. This is the reverse of
     * {@link #toInts(byte[])}.
     * <p/>
     * Example: <code>{0x00112233, 0x44556677}</code> will be converted to
     * <code>{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77}</code>.
     *
     * @param vals
     * @return
     */
    public static byte[] toBytes(int[] vals) {
        byte[] bytes = new byte[vals.length * 4];
        for (int i = 0; i < vals.length; i++) {
            byte[] bytesForInt = toBytes(vals[i]);
            copy(bytesForInt, bytes, i * 4);
        }
        return bytes;
    }

    /**
     * Copy bytes from one array to the specified position in another, such that
     * <code>target[offset]=source[0], target[offset+1]=source[1]</code> and so
     * on...
     * <p/>
     * Left for legacy code support.
     *
     * @param source
     * @param target
     * @param offset starting position in the target array to copy to. 0 means copy
     *               to the start.
     */
    public static void copy(byte[] source, byte[] target, int offset) {
        System.arraycopy(source, 0, target, offset, source.length);
    }

    /**
     * Convert byte array to char array
     *
     * @param b
     * @return
     */
    public static char[] toChars(byte[] b) {
        char[] c = new char[b.length >> 1];
        for (int i = 0, j = 0; i < c.length; i++) {
            c[i] = (char) (((b[j++] << 8) & 0xff00) | (b[j++] & 0x00ff));
        }
        return c;
    }

    /**
     * Convert byte array to char array
     *
     * @param s
     * @return
     */
    public static char[] toChars(String s) {
        return s.toCharArray();
    }

    /**
     * Convert an int to a byte[4].
     *
     * @param val
     * @return
     */
    public static byte[] toBytes(int val) {
        byte[] data = new byte[4];
        data[0] = (byte) ((val & 0xFF000000) >> 24);
        data[1] = (byte) ((val & 0x00FF0000) >> 16);
        data[2] = (byte) ((val & 0x0000FF00) >> 8);
        data[3] = (byte) ((val & 0x000000FF) >> 0);
        return data;
    }

    /**
     * Convert a long to a byte[8].
     *
     * @param val
     * @return
     */
    public static byte[] toBytes(long val) {
        byte[] data = new byte[8];
        data[0] = (byte) ((val & 0xFF00000000000000L) >> 56);
        data[1] = (byte) ((val & 0x00FF000000000000L) >> 48);
        data[2] = (byte) ((val & 0x0000FF0000000000L) >> 40);
        data[3] = (byte) ((val & 0x000000FF00000000L) >> 32);
        data[4] = (byte) ((val & 0x00000000FF000000L) >> 24);
        data[5] = (byte) ((val & 0x0000000000FF0000L) >> 16);
        data[6] = (byte) ((val & 0x000000000000FF00L) >> 8);
        data[7] = (byte) ((val & 0x00000000000000FFL) >> 0);
        return data;
    }

    /**
     * Create a String from a byte array using UTF-8.
     *
     * @param b
     * @return
     */
    public static String toString(byte[] b) {
        return b != null ? new String(b, UTF8) : null;
    }

    /**
     * Convert 4 bytes to an int.
     *
     * @param data
     * @return
     */
    public static short toShort(byte[] data) {
        return data != null && data.length >= 2 ?
                (short) (
                        ((data[0] & 0xff) << 8) |
                                ((data[1] & 0xff))) : 0;
    }

    /**
     * Convert a byte array to ints. This is the reverse of
     * {@link #toBytes(int[])}.
     * <p/>
     * Example: <code>{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77}</code>
     * will be converted to <code>{0x00112233, 0x44556677}</code>.
     *
     * @param b
     * @return
     */
    public static int[] toInts(byte[] b) {
        int[] ints = new int[b.length / 4];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = toInt(b, i * 4, 0);
        }
        return ints;
    }

    /**
     * Convert 4 bytes to an int.
     *
     * @param data
     * @return
     */
    public static int toInt(byte[] data) {
        return toInt(data, 0);
    }

    public static int toInt(byte[] data, int def) {
        return toInt(data, 0, def);
    }

    public static int toInt(byte[] data, int off, int def) {
        if (data != null && data.length >= off + 4) {
            return (data[off] & 0xff) << 24 |
                    ((data[off + 1] & 0xff) << 16) |
                    ((data[off + 2] & 0xff) << 8) |
                    ((data[off + 3] & 0xff));
        } else {
            return def;
        }
    }

    /**
     * Convert 4 bytes to an unsigned long.
     *
     * @param data
     * @return
     */
    public static long toUInt(byte[] data) {
        return toUInt(data, 0L);
    }

    public static long toUInt(byte[] data, long def) {
        return toUInt(data, 0, def);
    }

    public static long toUInt(byte[] data, int off, long def) {
        if (data != null && data.length >= off + 4) {
            return (data[off] & 0xffL) << 24 |
                    ((data[off + 1] & 0xffL) << 16) |
                    ((data[off + 2] & 0xffL) << 8) |
                    ((data[off + 3] & 0xffL));
        } else {
            return def;
        }
    }

    /**
     * Convert 8 bytes to a long.
     *
     * @param data
     * @return
     */
    public static long toLong(byte[] data) {
        return toLong(data, 0L);
    }

    public static long toLong(byte[] data, long def) {
        return toLong(data, 0, def);
    }

    public static long toLong(byte[] data, int off, long def) {
        if (data != null && data.length >= 8 + off) {
            return (data[off] & 0xffL) << 56 | ((data[off + 1] & 0xffL) << 48) |
                    ((data[off + 2] & 0xffL) << 40) | ((data[off + 3] & 0xffL) << 32) |
                    ((data[off + 4] & 0xffL) << 24) | ((data[off + 5] & 0xffL) << 16) |
                    ((data[off + 6] & 0xffL) << 8) | ((data[off + 7] & 0xffL));
        } else {
            return def;
        }
    }

    /**
     * Write a length field to an OutputStream.  Used by write[Bytes|String].
     *
     * @param size
     * @param os
     * @throws IOException
     */
    public static void writeLength(long size, OutputStream os) throws IOException {
        if (size < 0) {
            throw new IllegalArgumentException("writeLength value must be >= 0: " + size);
        }
        if (size == 0) {
            os.write(0);
            return;
        }
        while (size > 0) {
            if (size > 0x7f) {
                os.write((int) (0x80 | (size & 0x7f)));
            } else {
                os.write((int) (size & 0x7f));
            }
            size >>= 7;
        }
    }

    /**
     * Read a length field from an InputStream.  Used by read[Bytes|String].
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static long readLength(InputStream in) throws IOException {
        long size = 0;
        long iter = 0;
        long next = 0;
        do {
            next = in.read();
            if (next < 0) {
                throw new EOFException();
            }
            size |= ((next & 0x7f) << iter);
            iter += 7;
        }
        while ((next & 0x80) == 0x80);
        return size;
    }

    public static short readShort(InputStream in) throws IOException {
        return toShort(readBytes(in, 2));
    }

    public static int readInt(InputStream in) throws IOException {
        return toInt(readBytes(in, 4), -1);
    }

    public static long readUInt(InputStream in) throws IOException {
        return toUInt(readBytes(in, 4), -1);
    }

    public static long readLong(InputStream in) throws IOException {
        return toLong(readBytes(in, 8), -1);
    }

    public static void writeShort(short s, OutputStream os) throws IOException {
        os.write(toBytes(s));
    }

    public static void writeInt(int i, OutputStream os) throws IOException {
        os.write(toBytes(i));
    }

    public static void writeLong(long l, OutputStream os) throws IOException {
        os.write(toBytes(l));
    }

    /**
     * Write a byte array prefixed by a length field.
     *
     * @param b
     * @param os
     * @throws IOException
     */
    public static void writeBytes(byte[] b, OutputStream os) throws IOException {
        writeLength(b.length, os);
        if (b.length > 0) {
            os.write(b);
        }
    }

    /**
     * Write a byte array prefixed by a length field.
     *
     * @param b
     * @param os
     * @throws IOException
     */
    public static void writeBytes(byte[] b, int off, int len, OutputStream os) throws IOException {
        if (len > 0) {
            writeLength(len, os);
            os.write(b, off, len);
        }
    }

    /**
     * Write a char array prefixed by a length field.
     *
     * @param c
     * @param os
     * @throws IOException
     */
    public static void writeChars(char[] c, OutputStream os) throws IOException {
        writeBytes(toBytes(c), os);
    }

    /**
     * Read an InputStream to it's end and return as a byte array
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        byte[] buf = new byte[1024];
        int read = 0;
        while ((read = in.read(buf)) >= 0) {
            bos.write(buf, 0, read);
        }
        return bos.toByteArray();
    }

    /**
     * write all bytes from an InputStream to an OutputStream
     * blocks until EOF is reached and all data has been written to OS
     */
    public static int writeFully(InputStream is, OutputStream os) throws IOException {
        int len, total = 0;
        byte[] buf = new byte[1024];
        while ((len = is.read(buf)) != -1) {
            os.write(buf, 0, len);
            total += len;
        }
        return total;
    }

    /**
     * Read a byte array prefixed by a length field.
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static byte[] readBytes(InputStream in) throws IOException {
        return readBytes(in, (int) readLength(in));
    }

    /**
     * Read a char array prefixed by a length field.
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static char[] readChars(InputStream in) throws IOException {
        return toChars(readBytes(in, (int) readLength(in)));
    }

    /**
     * Read a byte array prefixed by a length field.
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static byte[] readBytes(InputStream in, int len) throws IOException {
        if (len < 0) {
            return null;
        }
        if (len == 0) {
            return emptyBytes;
        }
        byte[] b = new byte[len];
        int got = 0;
        int read = 0;
        while (got < b.length && (read = in.read(b, got, b.length - got)) >= 0) {
            got += read;
        }
        if (read < 0) {
            throw new EOFException();
            //          return null;
        }
        if (got < len) {
            byte[] ret = new byte[got];
            System.arraycopy(b, 0, ret, 0, got);
            b = ret;
        }
        return b;
    }

    /**
     * read len bytes from is into b, starting at off. same as
     * InputStream.read(byte[], int, int) except that it keeps trying until len
     * bytes have been read.
     *
     * @param is
     * @param b
     * @param off
     * @param len
     * @return
     * @throws IOException if the stream threw an exception or if the end of
     *                     stream was reached before len bytes could be read
     */
    public static void readBytes(InputStream is, byte[] b, int off, int len) throws IOException {
        int read = 0;
        while (read < len) {
            int got = is.read(b, off + read, len - read);
            if (got == -1) {
                throw new IOException("reached end of stream before " + len + " bytes could be read");
            } else {
                read += got;
            }
        }
    }

    /**
     * Write a String prefixed by a length field.
     *
     * @param str
     * @param os
     * @throws IOException
     */
    public static void writeString(String str, OutputStream os) throws IOException {
        writeBytes(str != null ? toBytes(str) : emptyBytes, os);
    }

    public static void writeCharString(String str, OutputStream os) throws IOException {
        writeChars(str != null ? toChars(str) : new char[0], os);
    }

    /**
     * Read a String prefixed by a length field.
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static String readString(InputStream in) throws IOException {
        return toString(readBytes(in));
    }

    public static String readString(InputStream in, boolean emptyNull) throws IOException {
        byte[] b = readBytes(in);
        return b.length > 0 ? toString(b) : emptyNull ? null : toString(b);
    }

    public static String readCharString(InputStream in) throws IOException {
        char[] ch = readChars(in);
        return ch != null ? new String(ch) : null;
    }

    public static String urlencode(String s) {
        if (nativeURLCodec) {
            try {
                return URLEncoder.encode(s, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        byte[] c = toBytes(s);
        int vcount = 0;
        boolean plus = false;
        for (byte aC : c) {
            if ((aC >= 'a' && aC <= 'z') || (aC >= 'A' && aC <= 'Z') || (aC >= '0' && aC <= '9') || aC == '.' || aC == '-' || aC == '*' || aC == '_') {
                continue;
            }
            if (aC == ' ') {
                plus = true;
            } else {
                vcount++;
            }
        }
        if (plus || vcount > 0) {
            byte[] nc = new byte[c.length + (2 * vcount)];
            int pos = 0;
            for (byte aC : c) {
                if ((aC >= 'a' && aC <= 'z') || (aC >= 'A' && aC <= 'Z') || (aC >= '0' && aC <= '9') || aC == '.' || aC == '-' || aC == '*' || aC == '_') {
                    nc[pos++] = aC;
                    continue;
                }
                if (aC == ' ') {
                    nc[pos++] = '+';
                    continue;
                }
                nc[pos++] = '%';
                nc[pos++] = Numbers.HEX[(aC >> 4) & 0x0f];
                nc[pos++] = Numbers.HEX[(aC >> 0) & 0x0f];
            }
            return new String(nc, 0, pos, UTF8);
        } else {
            return s;
        }
    }

    /**
     * optimized and works only for UTF-8 - but 2x faster than JDK implementation
     * replaces URLDecoder.decode(val, "UTF-8")
     */
    public static String urldecode(String s) {
        if (s == null || (!s.contains("%") && !s.contains("+"))) {
            // nothing to decode
            return s;
        }
        if (nativeURLCodec) {
            try {
                return URLDecoder.decode(s, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        byte[] c = toBytes(s);
        int vcount = 0;
        boolean changed = false;
        for (int i = 0; i < c.length; i++) {
            if (c[i] == '%' && i < c.length - 2) {
                if (Bytes.hex2dec(c[i + 1]) >= 0 && Bytes.hex2dec(c[i + 2]) >= 0) {
                    vcount++;
                }
            } else if (c[i] == '+') {
                c[i] = ' ';
                changed = true;
            }
        }
        if (vcount > 0) {
            int pos = 0;
            byte[] nc = new byte[c.length - vcount * 2];
            for (int i = 0; i < c.length; i++) {
                if (c[i] == '%' && i < c.length - 2) {
                    int hd1 = Bytes.hex2dec(c[i + 1]);
                    int hd2 = Bytes.hex2dec(c[i + 2]);
                    if (hd1 >= 0 && hd2 >= 0) {
                        nc[pos++] = (byte) (((hd1 << 4) | hd2) & 0xff);
                        i += 2;
                    } else {
                        nc[pos++] = c[i];
                    }
                } else if (c[i] == '+') {
                    nc[pos++] = ' ';
                } else {
                    nc[pos++] = c[i];
                }
            }
            return new String(nc, 0, pos, UTF8);
        } else if (changed) {
            return new String(c, UTF8);
        } else {
            return s;
        }
    }

    // fastest method to reverse int bits (with 32 bit jvm)
    public static int reverseBits(int v1) {
        return
                (BitReverseTable256[v1 & 0xff] << 24) |
                        (BitReverseTable256[(v1 >> 8) & 0xff] << 16) |
                        (BitReverseTable256[(v1 >> 16) & 0xff] << 8) |
                        (BitReverseTable256[(v1 >> 24) & 0xff]);
    }

    public static long reverseBits(long v1) {
        return
                ((long) (BitReverseTable256[(int) v1 & 0xff]) << 56) |
                        ((long) (BitReverseTable256[(int) (v1 >> 8) & 0xff]) << 48) |
                        ((long) (BitReverseTable256[(int) (v1 >> 16) & 0xff]) << 40) |
                        ((long) (BitReverseTable256[(int) (v1 >> 24) & 0xff]) << 32) |
                        ((long) (BitReverseTable256[(int) (v1 >> 32) & 0xff]) << 24) |
                        ((long) (BitReverseTable256[(int) (v1 >> 40) & 0xff]) << 16) |
                        ((long) (BitReverseTable256[(int) (v1 >> 48) & 0xff]) << 8) |
                        ((long) (BitReverseTable256[(int) (v1 >> 56) & 0xff]));
    }

    // safety quick switch to old mode if we find problems
    private static final boolean nativeURLCodec = System.getProperty("nativeURLCodec", "1").equals("1");
    private static final int[] BitReverseTable256 =
            {
                    0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60, 0xE0, 0x10, 0x90, 0x50, 0xD0, 0x30, 0xB0, 0x70, 0xF0,
                    0x08, 0x88, 0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8, 0x18, 0x98, 0x58, 0xD8, 0x38, 0xB8, 0x78, 0xF8,
                    0x04, 0x84, 0x44, 0xC4, 0x24, 0xA4, 0x64, 0xE4, 0x14, 0x94, 0x54, 0xD4, 0x34, 0xB4, 0x74, 0xF4,
                    0x0C, 0x8C, 0x4C, 0xCC, 0x2C, 0xAC, 0x6C, 0xEC, 0x1C, 0x9C, 0x5C, 0xDC, 0x3C, 0xBC, 0x7C, 0xFC,
                    0x02, 0x82, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2, 0x12, 0x92, 0x52, 0xD2, 0x32, 0xB2, 0x72, 0xF2,
                    0x0A, 0x8A, 0x4A, 0xCA, 0x2A, 0xAA, 0x6A, 0xEA, 0x1A, 0x9A, 0x5A, 0xDA, 0x3A, 0xBA, 0x7A, 0xFA,
                    0x06, 0x86, 0x46, 0xC6, 0x26, 0xA6, 0x66, 0xE6, 0x16, 0x96, 0x56, 0xD6, 0x36, 0xB6, 0x76, 0xF6,
                    0x0E, 0x8E, 0x4E, 0xCE, 0x2E, 0xAE, 0x6E, 0xEE, 0x1E, 0x9E, 0x5E, 0xDE, 0x3E, 0xBE, 0x7E, 0xFE,
                    0x01, 0x81, 0x41, 0xC1, 0x21, 0xA1, 0x61, 0xE1, 0x11, 0x91, 0x51, 0xD1, 0x31, 0xB1, 0x71, 0xF1,
                    0x09, 0x89, 0x49, 0xC9, 0x29, 0xA9, 0x69, 0xE9, 0x19, 0x99, 0x59, 0xD9, 0x39, 0xB9, 0x79, 0xF9,
                    0x05, 0x85, 0x45, 0xC5, 0x25, 0xA5, 0x65, 0xE5, 0x15, 0x95, 0x55, 0xD5, 0x35, 0xB5, 0x75, 0xF5,
                    0x0D, 0x8D, 0x4D, 0xCD, 0x2D, 0xAD, 0x6D, 0xED, 0x1D, 0x9D, 0x5D, 0xDD, 0x3D, 0xBD, 0x7D, 0xFD,
                    0x03, 0x83, 0x43, 0xC3, 0x23, 0xA3, 0x63, 0xE3, 0x13, 0x93, 0x53, 0xD3, 0x33, 0xB3, 0x73, 0xF3,
                    0x0B, 0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB, 0x1B, 0x9B, 0x5B, 0xDB, 0x3B, 0xBB, 0x7B, 0xFB,
                    0x07, 0x87, 0x47, 0xC7, 0x27, 0xA7, 0x67, 0xE7, 0x17, 0x97, 0x57, 0xD7, 0x37, 0xB7, 0x77, 0xF7,
                    0x0F, 0x8F, 0x4F, 0xCF, 0x2F, 0xAF, 0x6F, 0xEF, 0x1F, 0x9F, 0x5F, 0xDF, 0x3F, 0xBF, 0x7F, 0xFF
            };

    /**
     * similar return to Comparator.compare() for Strings
     *
     * @param a
     * @param b
     * @return
     */
    public static int compare(byte[] a, byte[] b) {
        for (int al = a.length, bl = b.length, i = 0; i < al; i++) {
            if (bl <= i) {
                return 1;
            }
            int val = (a[i] & 0xff) - (b[i] & 0xff);
            if (val == 0) {
                continue;
            }
            return val;
        }
        return a.length == b.length ? 0 : -1;
    }

    /**
     * Returns true if both arrays are of the same length and have equal content
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(byte[] a, byte[] b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(byte[] arr) {
        return arr == null || arr.length == 0;
    }

    /**
     * Extract a subset of a byte array (like substring).
     *
     * @param src
     * @param off
     * @param len
     * @return
     */
    public static byte[] cut(byte[] src, int off, int len) {
        byte[] ret = new byte[len];
        System.arraycopy(src, off, ret, 0, len);
        return ret;
    }

    /**
     * turns 0-9,a-f into a value from 0-15
     * helper method for urldecode()
     */
    public static int hex2dec(char c) {
        c |= 0x20; // to lower
        if (c >= 'a' && c <= 'f') {
            return 10 + (c - 'a');
        } else if (c >= 'A' && c <= 'F') {
            return 10 + (c - 'A');
        } else if (c >= '0' && c <= '9') {
            return c - '0';
        }
        return -1;
    }

    public static int hex2dec(byte c) {
        c |= 0x20; // to lower
        if (c >= 'a' && c <= 'f') {
            return 10 + (c - 'a');
        } else if (c >= 'A' && c <= 'F') {
            return 10 + (c - 'A');
        } else if (c >= '0' && c <= '9') {
            return c - '0';
        }
        return -1;
    }

    /**
     * @return String of the same length containing only supplied char
     */
    public static String clear(String s, char ch) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            c[i] = ch;
        }
        return new String(c);
    }

    public static String pad0(long val, int zeros) {
        String sval = Long.toString(val);
        try {
            return sval.length() > 8 ? sval : "00000000".substring(8 - zeros + sval.length()) + sval;
        } catch (Exception ex) {
            ex.printStackTrace();
            return sval;
        }
    }
}
