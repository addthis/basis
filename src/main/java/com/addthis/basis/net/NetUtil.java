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
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A collection of static methods implementing common helper functions
 * used in most classes.
 */
public final class NetUtil {
    private static final String localDomain = System.getProperty("net.domain", ".localhost");
    private static final String localHost = System.getProperty("net.host");

    /**
     * Array of bytes for generating hex strings.
     */
    public static final byte hex[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Helper class that holds a "host:port" pair.  Used to implement trust lists
     * and support easier command-line parsing of host:port pairs.
     */
    public static class Host {
        public Host(InetSocketAddress addr) {
            this.name = addr.getHostName();
            this.port = addr.getPort();
        }

        public Host(String name, int port) {
            this.name = name;
            this.port = port;
        }

        public String name;
        public int port;

        public String toString() {
            return name + ":" + port;
        }

        public InetSocketAddress getAddress() {
            return new InetSocketAddress(name, port);
        }

        /**
         * Return true of
         *
         * @param hostname
         * @return
         */
        public boolean matchName(String hostname) {
            if (hostname.equals(name)) {
                return true;
            }
            try {
                if (hostname.startsWith(".")) {
                    return name.endsWith(hostname) || InetAddress.getByName(name).getHostName().endsWith(hostname);
                }
                if (name.startsWith(".")) {
                    return hostname.endsWith(name) || InetAddress.getByName(hostname).getHostName().endsWith(name);
                }
                return
                        InetAddress.getByName(hostname).getHostAddress().equals(name) ||
                                InetAddress.getByName(name).getHostAddress().equals(hostname);
            } catch (UnknownHostException ex) {
                // ignore and return false
            }
            return false;
        }
    }

    /**
     * Parses a string in the form "host:port" into a NetUtil.Host object.
     */
    public static Host parseHost(String str) {
        return parseHost(str, 0);
    }

    /**
     * Parses a string in the form "host[:port]" into a NetUtil.Host object using defaultPort
     * as the port if none is provided
     */
    public static Host parseHost(String str, int defaultPort) {
        StringTokenizer st = new StringTokenizer(str, ":");
        String host = st.nextToken();
        int port = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : defaultPort;
        return new Host(host, port);
    }

    /**
     * Parses a string in the form "host:port,host:port,host:port..." into
     * a List of NetUtil.Host objects.
     */
    public static List<Host> parseHostList(String str) {
        LinkedList<Host> ll = new LinkedList<>();
        StringTokenizer st = new StringTokenizer(str, ",");
        while (st.hasMoreTokens()) {
            String nt = st.nextToken();
            ll.add(parseHost(nt));
        }
        return ll;
    }

    /**
     * Matches a host name against a List of Hosts
     *
     * @param hostname in the form 'foo.clearspring.com', '.clearspring.com' or '1.2.3.4'
     * @param list     List of Host objects
     * @return true if hostname matches an entry in the list, false otherwise
     */
    public static boolean hostNameMatch(String hostname, List<Host> list) {
        if (list == null || hostname == null) {
            return false;
        }
        for (Host aList : list) {
            if (aList.matchName(hostname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @see hostNameMatch(String, List)
     */
    public static boolean hostNameMatch(InetAddress host, List<Host> list) {
        if (list == null || host == null) {
            return false;
        }
        return hostNameMatch(host.getHostName(), list);
    }

    /**
     * @return an InetAddress in the form 'aabbccdd'
     * @throws UnknownHostException
     */
    public static byte[] getInetAddressAsHex(InetAddress addr) {
        byte raddr[] = addr.getAddress();
        byte naddr[] = new byte[raddr.length * 2];
        for (int i = 0; i < raddr.length; i++) {
            naddr[i * 2] = hex[(raddr[i] >> 4) & 0xf];
            naddr[i * 2 + 1] = hex[raddr[i] & 0xf];
        }
        return naddr;
    }

    /**
     * @return an InetAddress given a address in the form 'aabbccdd'
     * @throws UnknownHostException
     */
    public static InetAddress getInetAddressFromHex(String hex) throws UnknownHostException {
        byte addr[] = new byte[hex.length() / 2];
        for (int i = 0; i < addr.length; i++) {
            addr[i] = (byte) (Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16) & 0xff);
        }
        return InetAddress.getByAddress(addr);
    }

    public static String inferDomain() {
        try {
            String myHostName = localHost != null ? localHost : InetAddress.getLocalHost().getHostName();
            int lio = myHostName.indexOf('.');
            if (lio > 0) {
                return myHostName.substring(lio);
            }
        } catch (Exception ex) {
        }
        return localDomain;
    }

    /**
     * Given a host name, returns it's "base" domain via some American
     * centric heuristics
     */
    public static String getBaseDomain(String host) {
        return getBaseDomain(host, true);
    }

    public static String getBaseDomain(String host, boolean tight) {
        int he = host.length();
        if (he < 5) {
            return host;
        }
        host = host.toLowerCase();
        char lastChar = host.charAt(he - 1);
        // strip trailing '.'
        if (lastChar == '.') {
            he--;
        }
        // look for more than one '.'
        int of = host.indexOf('.');
        int d1 = host.lastIndexOf('.', he - 1);
        int d2 = d1 > 0 ? host.lastIndexOf('.', d1 - 1) : -1;
        int d3 = d2 > 0 ? host.lastIndexOf('.', d2 - 1) : -1;
        boolean lastNum = host.charAt(he - 1) - '0' < 10;
        boolean common = (he - d1 == 4); // 3 digit tld (COM, NET, EDU, etc)
        // if host does not end with number (12.34.56.78) and it has more than one '.' but one of those
        // '.' is not trailing and host begins with 'www' or ends with 'com', 'net', 'org', drop first token
        int hs = 0;
        if (!lastNum && ((host.startsWith("www.") || (tight && ((d2 > 0 && d1 > d2 && common) || (d3 > 0 && d2 > d3)))))) {
            hs += of + 1;
        }
        //System.out.println(host+" d1="+d1+" d2="+d2+" d3="+d3+" hs="+hs+" he="+he+" ln="+lastNum+" cm="+common);
        host = host.substring(hs, he);
        return host;
    }

    public static String resolveDottedIP(String ip) {
        if (ip.length() == 0 || !(Character.isDigit(ip.charAt(ip.length() - 1)) && Character.isDigit(ip.charAt(0)))) {
            return ip;
        }
        try {
            String newhost = InetAddress.getByName(ip).getHostName();
            if (newhost != null) {
                return newhost;
            }
        } catch (Exception ex) {
        }
        return ip;
    }

    /**
     * convert an ip address string into it's equivalent 32 bit value stored in a long.
     *
     * @param ip
     * @return long value or -1 if there was an error parsing the ip address
     */
    public static long ipToLong(String ip) {
        if (ip == null) {
            return -1;
        }
        String[] parts = ip.split("\\.");
        try {
            return Long.parseLong(parts[0]) << 24 | Long.parseLong(parts[1]) << 16 |
                    Long.parseLong(parts[2]) << 8 | Long.parseLong(parts[3]);
        } catch (NumberFormatException e) {
            return -1;
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        }
    }

    /**
     * convert a long representation of an ip address into a string
     *
     * @param i
     * @return
     */
    public static String longToIp(long i) {
        return ((i >> 24) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                (i & 0xFF);
    }

}

