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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.zip.GZIPOutputStream;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RollingLog extends OutputStream implements FileLogger {
    protected static final Logger log = LoggerFactory.getLogger(RollingLog.class);
    private final DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMdd-HHmmss");

    // The heritage of IFileLogger is that multiple threads might want
    // to write to this logger, and should also be able to start and
    // stop the logger.  No need for CAS (AtomicBoolean) in this case.
    // Note also that the default of 'true' is different from FileLogger
    private volatile boolean loggingEnabled = true;

    private File dir;
    private String prefix = "";
    private String suffix = "";
    private boolean compress;
    private long maxSize;
    private long maxAge;

    private long open;
    private File file;
    private File fileRename;
    private File cFile;
    private OutputStream out;

    public RollingLog(File dir, String prefix, boolean compress, long maxSize, long maxAge) {
        this(dir, prefix, "", compress, maxSize, maxAge);
    }

    public RollingLog(File dir, String pre, String suf, boolean compress, long maxSize, long maxAge) {
        dir.mkdirs();
        this.dir = LessFiles.initDirectory(dir);
        this.prefix = pre != null ? pre : "";
        this.suffix = suf != null ? suf : "";
        if (prefix != "" && !prefix.endsWith("-")) {
            prefix = prefix + "-";
        }
        if (suffix != "" && !suffix.startsWith(".")) {
            suffix = "." + suffix;
        }
        this.compress = compress;
        this.maxSize = maxSize;
        this.maxAge = maxAge;
    }

    private void openNext() throws IOException {
        close();

        open = System.currentTimeMillis();
        String fname = prefix + format.print(open) + suffix;
        file = new File(dir, fname + ".tmp");
        out = new FileOutputStream(file);
        fileRename = new File(dir, fname);

        if (compress) {
            cFile = new File(dir, fname + ".gz");
        }
    }

    private void checkNext() throws IOException {
        if ((out == null) ||
                (maxAge > 0 && (System.currentTimeMillis() - open) > maxAge) ||
                (maxSize > 0 && file.length() > maxSize)) {
            openNext();
        }
    }

    @Override
    public synchronized void writeLine(String line) {
        if (loggingEnabled) {
            try {
                write(LessBytes.toBytes(line));
                write('\n');
            } catch (IOException e) {
                log.debug("error writing to log file", e);
            }
        }
    }

    @Override
    public synchronized void write(int b) throws IOException {
        checkNext();
        out.write(b);
    }

    @Override
    public synchronized void write(byte b[]) throws IOException {
        checkNext();
        out.write(b);
    }

    @Override
    public synchronized void write(byte b[], int off, int len) throws IOException {
        checkNext();
        out.write(b, off, len);
    }

    @Override
    public synchronized void close() throws IOException {
        if (out != null) {
            out.flush();
            out.close();

            if (!file.renameTo(fileRename)) {
                log.info("file rename failed :: " + file + " --> " + fileRename);
            } else if (compress) {
                final File from = fileRename, to = cFile;
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        compressFile(from, to);
                    }
                });
                t.setDaemon(true);
                t.start();
            }
            out = null;
        }
    }

    // Interface stop/start niceties
    @Override
    public boolean isLogging() {
        return loggingEnabled;
    }

    @Override
    public void startLogging() {
        loggingEnabled = true;
    }

    @Override
    public void stopLogging() {
        loggingEnabled = false;
    }

    private void compressFile(File current, File compressed) {
        try {
            GZIPOutputStream cStream = new GZIPOutputStream(new FileOutputStream(compressed));
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(current));
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                cStream.write(buffer, 0, read);
            }
            in.close();
            cStream.finish();
            cStream.close();

            if (!current.delete()) {
                log.info("file delete failed :: " + current);
            }
        } catch (IOException ioe) {
            log.debug("error compressing file", ioe);
            compressed.delete();
        }
    }
}
