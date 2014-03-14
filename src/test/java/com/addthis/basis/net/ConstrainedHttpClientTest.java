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
package com.addthis.basis.net;

import javax.naming.ServiceUnavailableException;

import java.io.IOException;

import org.apache.http.client.methods.HttpGet;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConstrainedHttpClientTest {

    private static int numThreads = 32;

    private static class AddThisThread extends Thread {

        ConstrainedHttpClient client;
        boolean unavailable = false;

        AddThisThread(ConstrainedHttpClient client) {
            this.client = client;
        }

        public void run() {
            try {
                client.execute(new HttpGet("http://addthis.com"), 10000);
            } catch (ServiceUnavailableException ex) {
                unavailable = true;
            } catch (IOException e) {
            }
        }

        public boolean getUnavaiable() {
            return unavailable;
        }

    }

    @Test
    public void basicGetTest() throws Exception {
        HttpGet request = new HttpGet("http://addthis.com");
        ConstrainedHttpClient httpClient = new ConstrainedHttpClient(1);

        byte[] response = httpClient.execute(request, 10000).getBody();
        assertNotNull("null response", response);
        assertTrue("zero response length", response.length > 0);
    }

    @Test
    public void unavailableTest() throws Exception {
        AddThisThread[] threads = new AddThisThread[numThreads];
        ConstrainedHttpClient client = new ConstrainedHttpClient(1);
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new AddThisThread(client);
        }
        for (int i = 0; i < numThreads; i++) {
            threads[i].start();
        }
        for (int i = 0; i < numThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
            }
        }
        boolean unavailable = false;
        for (int i = 0; i < numThreads; i++) {
            unavailable |= threads[i].getUnavaiable();
        }
        assertTrue("client unavailable", unavailable);
    }
}
