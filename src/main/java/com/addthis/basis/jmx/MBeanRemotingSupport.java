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
package com.addthis.basis.jmx;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import java.io.IOException;

import java.lang.management.ManagementFactory;

import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Collections;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIServerSocketFactory;

public class MBeanRemotingSupport implements RMIServerSocketFactory {
    static final String URL_PATTERN = "service:jmx:rmi:///jndi/rmi://:{PORT}/jmxrmi";
    static final String PROP_RIDS = "java.rmi.server.randomIDs";
    static final String PROP_SOCK = RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE;

    int port;
    Acceptor acceptor;
    JMXServiceURL url;
    JMXConnectorServer connector;

    /**
     * Same as this(port, null);
     */
    public MBeanRemotingSupport(int port) {
        this(port, null);
    }

    /**
     * Creates one of these suckers to listen on the specified port
     *
     * @param port     the port to listen on
     * @param acceptor an acceptor to filter client connections
     * @throws IllegalArgumentException
     */
    public MBeanRemotingSupport(int port, Acceptor acceptor) {
        this.port = port;
        this.acceptor = acceptor;
        this.connector = null;

        try {
            url = new JMXServiceURL(URL_PATTERN.replace("{PORT}", Integer.toString(port)));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("WTF?", e); // shouldn't happen
        }

        System.setProperty(PROP_RIDS, "true");
    }

    /**
     * @return the port number this guy is listening on
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the JMX url this guy will respond to
     */
    public JMXServiceURL getUrl() {
        return url;
    }

    /**
     * @return the underlying JMX connector server (this will be null until
     *         start() has been called, and after stop() is called)
     */
    public JMXConnectorServer getConnectorServer() {
        return connector;
    }

    /**
     * Starts this guy listening on his specified port
     *
     * @throws IOException              if the port couldn't be opened
     * @throws IllegalArgumentException if he's already started
     */
    public void start() throws IOException {
        if (connector != null) {
            throw new IllegalArgumentException("already started");
        }

        LocateRegistry.createRegistry(port);

        connector = JMXConnectorServerFactory.newJMXConnectorServer(
                url,
                Collections.singletonMap(PROP_SOCK, this),
                ManagementFactory.getPlatformMBeanServer());
        connector.start();
    }

    /**
     * Stops this guy listening
     *
     * @throws IOException if an error prevents the stop
     */
    public void stop() throws IOException {
        try {
            if (connector != null) {
                connector.stop();
            }
        } finally {
            connector = null;
        }
    }

    /**
     * @return a server socket that will filter connections through the
     *         supplied acceptor (if there is one)
     */
    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port) {
            @Override
            public Socket accept() throws IOException {
                Socket client = null;
                while (client == null) {
                    client = super.accept();
                    if (acceptor != null && !acceptor.accept(client)) {
                        client.close();
                        client = null;
                    }
                }
                return client;
            }
        };
    }

    /**
     * Interface for something that knows how to accept/reject connections
     * based on originating server.
     */
    public static interface Acceptor {
        /**
         * @return should the supplied socket connection be allowed to
         *         continue?
         */
        public boolean accept(Socket socket);
    }
}
