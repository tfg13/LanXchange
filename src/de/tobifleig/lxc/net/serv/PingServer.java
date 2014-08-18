/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014 Tobias Fleig (tobifleig gmail com)
 *
 * All rights reserved.
 *
 * This file is part of LanXchange.
 *
 * LanXchange is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LanXchange is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LanXchange. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tobifleig.lxc.net.serv;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ping-Server, listens for Pings/Heartbeats
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class PingServer {

    /**
     * The sockets currently in use.
     */
    private ConcurrentHashMap<NetworkInterface, InterfaceHandler> listenSockets;
    /**
     * The Listener to call when pings are received.
     */
    private PingServerListener listener;

    /**
     * Creates a new PingServer.
     * PingServers listen for Ping/KeepAlive-Signals by other instances.
     *
     * @param listener
     */
    public PingServer(PingServerListener listener) {
        this.listener = listener;
        listenSockets = new ConcurrentHashMap<NetworkInterface, InterfaceHandler>();
    }

    /**
     * Sets the list of used interfaces.
     *
     * @param interf the list
     */
    public void updateInterfaces(List<NetworkInterface> interf) {
        // close interfaces that should no longer be used
        ArrayList<NetworkInterface> removeList = new ArrayList<NetworkInterface>();
        for (NetworkInterface inter : listenSockets.keySet()) {
            if (!interf.contains(inter)) {
                removeList.add(inter);
            }
        }
        // remove sockets now (mark+sweep-method avoids concurrentmodificationexception)
        for (NetworkInterface inter : removeList) {
            listenSockets.get(inter).close();
            listenSockets.remove(inter);
        }
        // open new interfaces
        for (NetworkInterface inter : interf) {
            if (!listenSockets.containsKey(inter)) {
                listenTo(inter);
            }
        }
    }

    /**
     * Starts a thread to listen to the given NetworkInterface.
     *
     * @param inter the NetworkInterface to listen to
     */
    private void listenTo(final NetworkInterface inter) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InterfaceHandler handler = new InterfaceHandler(inter);
                    listenSockets.put(inter, handler);
                    handler.handle(inter);
                } catch (SocketException ex) {
                    System.out.println("No longer listening to " + inter.getName());
                } catch (IOException ex) {
                    System.out.println("Cannot listen to interface \"" + inter.getDisplayName() + "\"");
                }
            }
        }, "lxc_udplisten_" + inter.getName());
        t.setDaemon(true);
        t.start();
    }

    /**
     * Handles one NetworkInterface.
     * Joins the multicast groups for IPv4, IPv6 or both.
     */
    private class InterfaceHandler {

        /**
         * The MulticastSocket.
         */
        private final MulticastSocket socket;
        /**
         * Use IPv4 for this NetworkInterface.
         */
        private final boolean useIPv4;
        /**
         * Use IPv6 for this NetworkInterface.
         */
        private final boolean useIPv6;

        /**
         * Create a new Handler and set up the MulticastSocket.
         *
         * @param interf the target interface.
         * @throws IOException when there is trouble creating the socket or joining groups
         */
        private InterfaceHandler(final NetworkInterface interf) throws IOException {
            socket = new MulticastSocket(27716);
            // figure out if this interface supports IPv4, IPv6 or both
            Enumeration<InetAddress> addresses = interf.getInetAddresses();
            boolean v4 = false, v6 = false;
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address) {
                    v4 = true;
                } else if (address instanceof Inet6Address) {
                    v6 = true;
                }
            }
            useIPv4 = v4;
            useIPv6 = v6;
            // Configure interface. Not required for IPv6-only interfaces because of the zone index
            if (useIPv4) {
                socket.setNetworkInterface(interf);
                socket.joinGroup(InetAddress.getByName("225.4.5.6"));
            }
            if (useIPv6) {
                socket.joinGroup(Inet6Address.getByAddress("ff15::4c61:6e58:6368:616e:6765", new byte[]{(byte) 0xff, (byte) 0x15, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x4c, (byte) 0x61, (byte) 0x6e, (byte) 0x58, (byte) 0x63, (byte) 0x68, (byte) 0x61, (byte) 0x6e, (byte) 0x67, (byte) 0x65}, interf));
            }
            System.out.println("Listening to " + interf.getName() + ", which supports IPv4:" + useIPv4 + " IPv6:" + useIPv6);
        }

        /**
         * Blocks, receives request in a endless loop.
         *
         * @throws IOException on reception errors
         */
        private void handle(final NetworkInterface interf) throws IOException {
            // don't do anything when no supported protocol was found
            if (!useIPv4 && !useIPv6) {
                System.out.println(interf.getName() + " does not seem to support any known protocol!");
                return;
            }
            byte[] buffer = new byte[5];
            DatagramPacket packet = new DatagramPacket(buffer, 5);
            while (true) {
                socket.receive(packet); // Blocks
                // got ping/heartbeat
                listener.pingReceived(packet.getData(), packet.getAddress());
            }
        }

        /**
         * Closes the socket.
         */
        private void close() {
            socket.close();
        }

    }
}
