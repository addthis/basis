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

import javax.annotation.Nonnull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;

import java.util.ArrayList;
import java.util.List;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LessFiles {

    private static final boolean useStackTraceTempDirname = Parameter.boolValue("debug.tempdir.stacktrace", false);

    private LessFiles() {}

    /* Deletes all files and subdirectories under dir.
     * Returns true if all deletions were successful.
     * If a deletion fails, the method attempting to complete the delete but returns false.
     */
    public static boolean deleteDir(File dir) {
        boolean rval = true;
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                rval = rval && deleteDir(child);
            }
        }
        // The directory is now empty so delete it
        return rval && dir.delete();
    }

    public static boolean flushDir(File dir) {
        boolean rval = true;
        if (dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                rval = rval && deleteDir(child);
            }
        }
        return rval;
    }

    public static void write(String path, byte[] data, boolean append) throws IOException {
        File f = new File(path);
        LessFiles.write(f, data, append);
    }

    /**
     * write bytes to a file with the option to append
     */
    public static void write(File out, byte[] data, boolean append) throws IOException {
        try (FileOutputStream fot = new FileOutputStream(out, append)) {
            fot.write(data);
        }
    }

    /**
     * read all bytes from a file
     */
    public static byte[] read(File in) throws IOException {
        try (FileInputStream fin = new FileInputStream(in)) {
            return LessBytes.readFully(fin);
        }
    }

    /**
     * create a temp directory
     */
    public static File createTempDir() throws IOException {
        String prefix = "ctd";

        if (useStackTraceTempDirname) {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            if (stack.length > 2) {
                prefix = stack[2].getClassName() + "." + stack[2].getMethodName();
            }
        }

        return createTempDir(prefix, "tmp");
    }

    public static File createTempDir(String prefix, String suffix) throws IOException {
        File tmp = File.createTempFile(prefix, suffix);
        tmp.delete();
        tmp.mkdirs();
        return tmp;
    }

    public static boolean isFileReadable(String filename) {
        boolean isReadable = false;
        try {
            File file = new File(filename);
            isReadable = file.canRead();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isReadable;
    }


    public static BufferedReader getReader(String filepath) throws IOException {
        return getReader(new File(filepath)); //will throw NPE if filepath==null
    }

    public static BufferedReader getReader(File file) throws IOException {
        return new BufferedReader(new FileReader(file));
    }

    public static BufferedReader getReader(InputStream is) {
        return new BufferedReader(new InputStreamReader(is));
    }

    /** */
    public static File initDirectory(String dir) {
        return initDirectory(new File(dir));
    }

    /**
     * throws RuntimeException if directory doesn't exist and/or can't be created
     */
    public static File initDirectory(File file) {
        if (file.isDirectory()) {
            return file;
        }
        if (file.isFile()) {
            throw new RuntimeException("Requested directory '" + file + "' is a file");
        }
        if (!file.mkdirs()) {
            throw new RuntimeException("Unable to create directory '" + file + "'");
        }
        return file;
    }

    /**
     * throws IOException if directory doesn't exist and/or can't be created
     */
    public static File openDirectory(File file) throws IOException {
        if (file.isDirectory()) {
            return file;
        }
        if (file.isFile()) {
            throw new IOException("Requested directory '" + file + "' is a file");
        }
        if (!file.mkdirs()) {
            throw new IOException("Unable to create directory '" + file + "'");
        }
        return file;
    }

    public static String setupUIDFile(File dir, String uidFileName) throws IOException {
        File uid = new File(dir, uidFileName);
        String guid = CUID.createCUID();
        if (uid.exists() && uid.isFile() && uid.canRead()) {
            try (FileReader fr = new FileReader(uid)) {
                BufferedReader br = new BufferedReader(fr);
                guid = br.readLine();
                if ((guid == null) || guid.trim().isEmpty()) {
                    guid = CUID.createCUID();
                }
            }
        }
        try (FileOutputStream fos = new FileOutputStream(uid)) {
            fos.write(guid.getBytes());
            fos.flush();
        }
        return guid;
    }

    /**
     * truncate a file to the specified length
     */
    public static void truncate(File file, long size) throws IOException {
        if (file == null) {
            return;
        }
        if (size > file.length()) {
            throw new IllegalArgumentException("file " + file.getName() + " cannot be truncated, desired size (" + size + ") is greater than file length.");
        }
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(size);
        }
    }

    public static void expandPath(String root, List<File> expanded) {
        int off = 0;
        if ((off = root.indexOf("/*")) >= 0) {
            String left = root.substring(0, off);
            String right = off + 3 < root.length() ? root.substring(off + 3, root.length()) : "";
            File dir = new File(left);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    expandPath(left + "/" + file.getName() + "/" + right, expanded);
                }
            }
        } else {
            expanded.add(new File(root));
        }
    }

    public static File[] matchFiles(String dirTemplate) {
        ArrayList<File> expanded = new ArrayList<>();
        expandPath(dirTemplate, expanded);
        return expanded.toArray(new File[expanded.size()]);
    }

    public static String getSuffix(File file) {
        return getSuffix(file.getName());
    }

    public static String getSuffix(String fname) {
        int suffix_pos = fname.lastIndexOf('.');
        if (suffix_pos == -1) {
            return "";
        }
        return fname.substring(1 + suffix_pos);
    }

    public static File replaceSuffix(File file, String new_suffix) {
        String name = file.getName();
        int suffix_pos = name.lastIndexOf('.');
        return new File(file.getParent(),
                suffix_pos == -1 ? name + new_suffix : name.substring(0, suffix_pos) + new_suffix);
    }

    public static long directorySize(File directory) throws IOException {
        long size = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile()) {
                size += file.length();
            } else {
                size += directorySize(directory);
            }
        }
        return size;
    }
}
