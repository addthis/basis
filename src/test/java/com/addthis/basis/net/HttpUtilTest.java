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

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;


@Ignore
public class HttpUtilTest {
    // some random urls to hit, should be pretty reliable
    private static final String[] urls = {
            "http://cache.addthis.com/icons/v1/thumbs/32x32/facebook.png",
            "http://cache.addthis.com/icons/v1/thumbs/32x32/live.png",
            "http://cache.addthis.com/icons/v1/thumbs/32x32/twitter.png",
            "http://cache.addthis.com/icons/v1/thumbs/32x32/digg.png",
            "http://cache.addthis.com/icons/v1/thumbs/32x32/google.png",
            "http://cache.addthis.com/icons/v1/thumbs/32x32/delicious.png"
    };
    private static final List<String> urlList = Arrays.asList(urls);

    @Test
    public void basicGetTest() throws IOException, URISyntaxException {
        byte[] response = HttpUtil.httpGet("http://google.com", 0).getBody();
        assertNotNull("null response", response);
        assertTrue("zero response length", response.length > 0);
    }

    @Test
    public void getListTest() {
        List<byte[]> responses = HttpUtil.httpGet(urlList, 3, 0);
        assertNotNull("null response list", responses);
        for (byte[] response : responses) {
            assertNotNull("null response", response);
        }
    }
}
