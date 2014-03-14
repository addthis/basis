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

import java.io.IOException;
import java.net.URISyntaxException;
import javax.naming.ServiceUnavailableException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.http.client.methods.HttpGet;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ConstrainedHttpClientTest {

    @Test
    public void basicGetTest() throws Exception {
        HttpGet request = new HttpGet("http://google.com");
        ConstrainedHttpClient httpClient = new ConstrainedHttpClient(100) {
		};
		
    	byte[] response = httpClient.execute(request, 1000).getBody();
        assertNotNull("null response", response);
        assertTrue("zero response length", response.length > 0);
    }
}
