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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Parameter {

    /**
     * There is some code duplication among the inner and outer classes
     * but the intent is to avoid any bizarre ClassLoader thread safety
     * issues when initializing the static variables.
     */
    private static class ParameterLoader {
        private static boolean parseBoolean(String input, boolean defaultValue) {
            boolean result = defaultValue;
            try {
                if (input != null) {
                    result = Numbers.parseHumanReadable(input) > 0;
                }
            } catch (NumberFormatException ex) {
                result = input.equalsIgnoreCase("true");
            }
            return (result);
        }
    }

    private final static Logger log = LoggerFactory.getLogger(Parameter.class);;
    private final static String paramFile;
    private final static boolean input;
    private final static boolean output;
    private final static Properties track;

    static {
        paramFile = System.getProperty("param.file");
        input = ParameterLoader.parseBoolean(System.getProperty("param.input"), false);
        output = ParameterLoader.parseBoolean(System.getProperty("param.output"), false);
        if (input && paramFile != null) {
            File file = new File(paramFile);
            try {
                if (file.exists() && file.isFile()) {
                    FileInputStream in = new FileInputStream(file);
                    System.getProperties().load(in);
                    in.close();
                }
            } catch (IOException ex) {
                log.error("Error reading from '" + file.getPath() + "'", ex);
            }
        }
        if (output && paramFile != null) {
            track = new Properties();
            track.put("param.file", paramFile.toString());
            track.put("param.input", Boolean.valueOf(input).toString());
            track.put("param.output", Boolean.valueOf(output).toString());
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    File file = new File(paramFile);
                    try {
                        if (file.getParentFile().exists() && file.getParentFile().isDirectory()) {
                            FileOutputStream out = new FileOutputStream(file, true);
                            track.store(out, "tracked parameters");
                            out.close();
                        }
                    } catch (IOException ex) {
                        log.error("Error writing to '" + file.getPath() + "'", ex);
                    }
                }
            });
        } else {
            track = null;
        }
    }

    private static void track(String key, Object value) {
        if (track == null) {
            return;
        }
        if (value != null) {
            track.setProperty(key, value.toString());
        }
    }

    public static String value(String key) {
        String val = System.getProperty(key);
        track(key, val);
        return val;
    }

    public static String value(String key, String defaultValue) {
        String val = System.getProperty(key, defaultValue);
        track(key, val);
        return val;
    }

    public static boolean boolValue(String key, boolean defaultValue) {
        if (value(key) == null) {
            track(key, defaultValue);
            return defaultValue;
        }
        boolean val = intValue(key, 0) > 0 || value(key, "").equalsIgnoreCase("true");
        track(key, val);
        return val;
    }

    public static int intValue(String key, int defaultValue) {
        int val = (int) longValue(key, defaultValue);
        track(key, val);
        return val;
    }

    public static long longValue(String key, long defaultValue) {
        String value = value(key);
        if (value == null) {
            track(key, defaultValue);
            return defaultValue;
        }
        long val;
        try {
            val = Numbers.parseHumanReadable(value);
        } catch (Exception ex) {
            val = defaultValue;
        }
        track(key, val);
        return val;
    }

}
