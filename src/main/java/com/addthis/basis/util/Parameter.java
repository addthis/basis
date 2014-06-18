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

public class Parameter {
    private final static String paramFile = Parameter.value("param.file");
    private final static boolean output = Parameter.boolValue("param.output",false) && paramFile != null;

    static {
        if (paramFile != null) {
            File file = new File(paramFile);
            if (file.exists() && file.isFile()) {
                try {
                    FileInputStream in = new FileInputStream(file);
                    System.getProperties().load(in);
                    in.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static Properties track;

    private synchronized static void track(String key, Object value) {
        if (!output) {
            return;
        }
        if (track == null) {
            track = new Properties();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        File file = new File(paramFile);
                        if (!(file.getParentFile().exists() && file.getParentFile().isDirectory())) {
                            return;
                        }
                        FileOutputStream out = new FileOutputStream(file,true);
                        track.store(out,"tracked parameters");
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        if (value != null) {
            track.put(key, value.toString());
        }
    }

    public static String value(String key) {
        String val =  System.getProperty(key);
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
        long val = 0;
        try {
            val = Numbers.parseHumanReadable(value);
        } catch (Exception ex) {
            val = defaultValue;
        }
        track(key, val);
        return val;
    }

}
