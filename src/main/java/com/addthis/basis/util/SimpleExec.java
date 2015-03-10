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

import java.io.IOException;
import java.io.InputStream;

public class SimpleExec {
    private Process process;
    private byte[] stdout;
    private byte[] stderr;
    private int exit;

    public SimpleExec(String cmd) throws IOException {
        process = Runtime.getRuntime().exec(cmd);
    }

    public SimpleExec(String cmd[]) throws IOException {
        process = Runtime.getRuntime().exec(cmd);
    }

    public SimpleExec join() throws InterruptedException, IOException {
        InputStream stdoutIn = process.getInputStream();
        InputStream stderrIn = process.getErrorStream();
        stdout = LessBytes.readFully(stdoutIn);
        stderr = LessBytes.readFully(stderrIn);
        exit = process.waitFor();
        return this;
    }

    public byte[] stdout() {
        return stdout;
    }

    public String stdoutString() {
        return LessBytes.toString(stdout);
    }

    public byte[] stderr() {
        return stderr;
    }

    public String stderrString() {
        return LessBytes.toString(stderr);
    }

    public int exitCode() {
        return exit;
    }
}
