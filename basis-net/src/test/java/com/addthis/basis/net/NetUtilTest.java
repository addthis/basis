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

import java.net.InetAddress;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NetUtilTest {

    @Ignore
    @Test
    public void testResolve() throws Exception {
        System.out.println(NetUtil.resolveDottedIP("addthis.com"));
        System.out.println(NetUtil.resolveDottedIP("209.61.188.91"));
    }

    @Test
    public void testBase() throws Exception {
        System.out.println("-----------");
        testDomain("widgets.clearspring.com", "clearspring.com");
        testDomain("foo.com", "foo.com");
        testDomain("abc.foo.com", "foo.com");
        testDomain("www.foo.com", "foo.com");
        testDomain("foo.com.au", "foo.com.au");
        testDomain("abc.foo.com.au", "foo.com.au");
        testDomain("foo.co.uk", "foo.co.uk");
        testDomain("abc.foo.co.uk", "foo.co.uk");
        testDomain("abc.foo.com", "foo.com");
        testDomain("www.foo.com", "foo.com");
        testDomain("www.abc.foo.com", "abc.foo.com");
        testDomain("12.34.56.78", "12.34.56.78");
        System.out.println("-----------");
        testDomain("widgets.clearspring.com.", "clearspring.com");
        testDomain("foo.com.", "foo.com");
        testDomain("abc.foo.com.", "foo.com");
        testDomain("www.foo.com.", "foo.com");
        testDomain("foo.com.au.", "foo.com.au");
        testDomain("abc.foo.com.au.", "foo.com.au");
        testDomain("foo.co.uk.", "foo.co.uk");
        testDomain("abc.foo.co.uk.", "foo.co.uk");
        testDomain("abc.foo.com.", "foo.com");
        testDomain("www.foo.com.", "foo.com");
        testDomain("www.abc.foo.com.", "abc.foo.com");
        testDomain("12.34.56.78.", "12.34.56.78");
        System.out.println("***********");
    }

    @Ignore
    @Test
    public void testHex() throws Exception {
        testHexIP("addthis.com");
        testHexIP("google.com");
    }

    private static void testDomain(String host, String domain) {
        String res = NetUtil.getBaseDomain(host);
        boolean ok = res.equals(domain);
        assertTrue(ok);
        System.out.println((ok ? "OK" : "FAIL") + " >> " + host + " -> " + res);
    }

    private static void testHexIP(String host) throws Exception {
        byte hex[] = NetUtil.getInetAddressAsHex(InetAddress.getByName(host));
        String hs = new String(hex, "UTF-8");
        System.out.println(host + " -> " + hs);
        InetAddress ia = NetUtil.getInetAddressFromHex(hs);
        System.out.println(hs + " -> " + ia);
    }

    @Test
    public void testParseHost() {
        // host:port
        NetUtil.Host host = NetUtil.parseHost("host:80");
        assertEquals("host", host.name);
        assertEquals(80, host.port);

        // host
        host = NetUtil.parseHost("host");
        assertEquals("host", host.name);
        assertEquals(0, host.port);

        // host, default port
        host = NetUtil.parseHost("host", 80);
        assertEquals("host", host.name);
        assertEquals(80, host.port);

        // host:port, default port
        host = NetUtil.parseHost("host:80", 8000);
        assertEquals("host", host.name);
        assertEquals(80, host.port);

        // host:
        host = NetUtil.parseHost("host:");
        assertEquals("host", host.name);
        assertEquals(0, host.port);

        // :port
        host = NetUtil.parseHost(":80");
        assertEquals("80", host.name);
        assertEquals(0, host.port);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseHostInvalidPort() {
        NetUtil.parseHost("host:port");
    }

    @Test
    public void ipToLongTest() {
        String ip = "207.97.226.235";
        assertEquals(NetUtil.ipToLong(ip), 3479298795L);
    }
}
