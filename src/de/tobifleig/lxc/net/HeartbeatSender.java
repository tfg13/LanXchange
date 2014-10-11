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
package de.tobifleig.lxc.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Sends UDP-based multicasts. (heartbeats)
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
class HeartbeatSender {

    /**
     * Contains all sockets used to deploy multicasts.
     */
    private final HashMap<NetworkInterface, InterfaceHandler> sockets;
    /**
     * A private timer for sending heartbeats.
     */
    private final Timer timer;
    /**
     * The heartbeat-packet. Always the same and therefore cached.
     */
    private final byte[] packet;

    /**
     * Creates a new HeartbeatSender
     */
    HeartbeatSender() {
        sockets = new HashMap<NetworkInterface, InterfaceHandler>();
        // create packet
        packet = new byte[5];
        int id = LXCInstance.local.id;
        packet[3] = (byte) (id);
        packet[2] = (byte) (id >>> 8);
        packet[1] = (byte) (id >>> 16);
        packet[0] = (byte) (id >>> 24);
        packet[4] = 'h';
        timer = new Timer();
    }

    /**
     * Starts the HeartbeatSender.
     * Starts sending heartbeats every 20 seconds.
     * Sends 5 heartbeats to get started.
     */
    void start() {
        // start sending heartbeats
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                sendHeartbeat(-1);
            }
        };
        timer.schedule(task, 0, 20000);
        // inital heartbeat
        sendHeartbeat(4);
    }

    /**
     * Stops sending heartbeats and sends a special, last offline-beat.
     * Needs all known remote instances to send offline-signals to them.
     */
    void stop(final Iterable<LXCInstance> remoteInstances) {
        // Set packet content to "offline-signal"
        packet[4] = 'o';
        // cancel heartbeats:
        timer.cancel();
        // Send one last signal in a new thread (required for android)
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                multicast(packet);
                // offline signals to all known instances
                for (LXCInstance instance : remoteInstances) {
                    direct(packet, instance.getDownloadAddress());
                }
                // shutdown timer thread
                timer.cancel();
            }
        });
        t.start();
        // wait for thread to finish
        try {
            t.join(1000);// should not take too long
        } catch (InterruptedException ex) {
            // ignore, offline-signals are best-effort anyway
        }
        // done, sockets will be closed by the interface manager
    }

    /**
     * Sends heartbeats.
     * If asyncRepetitions equals -1, sending is synchronous.
     * Otherwise the internal timer is used. In this case there is a delay of one second after each packet.
     *
     * Only synchronous hearthbeats contain
     *
     * @param asyncRepetitions # of async packets, -1 for 1 sync packet
     */
    void sendHeartbeat(final int asyncRepetitions) {
        if (asyncRepetitions == -1) {
            // synchronous
            multicast(packet);
            manualBroadcast();
        } else {
            // asynchronous (multiple times)
            TimerTask multicastTask = new TimerTask() {
                int counter = asyncRepetitions;

                @Override
                public void run() {
                    multicast(packet);
                    if (--counter <= 0) {
                        this.cancel();
                    }
                }
            };
            timer.schedule(multicastTask, 0, 1000);
        }
    }

    /**
     * Updates the list of NetworkInterfaces used for multicasting
     *
     * @param interf the new list containing all used interfaces
     */
    synchronized void updateInterfaces(List<NetworkInterface> interf) {
        // close all interfaces no longer used
        ArrayList<NetworkInterface> removeList = new ArrayList<NetworkInterface>();
        for (NetworkInterface inter : sockets.keySet()) {
            if (!interf.contains(inter)) {
                removeList.add(inter);
            }
        }
        // remove sockets now(mark+sweep-method avoids concurrentmodificationexception)
        for (NetworkInterface inter : removeList) {
            sockets.get(inter).close();
            sockets.remove(inter);
        }
        // create sockets for new interfaces
        for (NetworkInterface inter : interf) {
            if (!sockets.containsKey(inter)) {
                createSocket(inter);
            }
        }
    }

    /**
     * Creates a new MulticastSocket for the given NetworkInterface
     *
     * @param inter the new NetworkInterface
     */
    private void createSocket(NetworkInterface inter) {
        try {
            sockets.put(inter, new InterfaceHandler(inter));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Sends a packet via UDP-Multicast to all sockets
     *
     * @param data the packet
     */
    private synchronized void multicast(byte[] data) {
        for (InterfaceHandler handler : sockets.values()) {
            try {
                handler.multicast();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Send the given packet via UDP to all local network devices.
     * This is only required to reach IPv4-only hosts on Android (because multicast is often not supported)
     * Simulates a broadcast by sending UDP packets to all possible network devices (within a /24 IPv4 subnet).
     */
    private synchronized void manualBroadcast() {
        for (NetworkInterface inter : sockets.keySet()) {
            // get local ipv4 address of this interface
            InetAddress localAddress = null;
            Enumeration<InetAddress> inetAddresses = inter.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress testAddress = inetAddresses.nextElement();
                if (!testAddress.isLoopbackAddress() && testAddress instanceof Inet4Address) {
                    // just pick this one
                    localAddress = testAddress;
                    break;
                }
            }
            // found one?
            if (localAddress != null) {
                InterfaceHandler handler = sockets.get(inter);
                // leave IPv6-only hosts alone
                if (handler.pack4 != null) {
                    byte[] localAddressBytes = localAddress.getAddress();
                    int localSuffix = localAddressBytes[3];
                    try {
                        for (int i = 1; i < 255; i++) {
                            if (i == localSuffix) {
                                // don't ping self
                                continue;
                            }
                            localAddressBytes[3] = (byte) i;
                            DatagramPacket pack = new DatagramPacket(packet, packet.length, InetAddress.getByAddress(localAddressBytes), 27716);
                            handler.socket.send(pack);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Send the given signal as a heartbeat to all known instances.
     *
     * @param data the data to send
     */
    private synchronized void direct(byte[] data, InetAddress address) {
        DatagramPacket pack = new DatagramPacket(data, data.length, address, 27716);
        for (InterfaceHandler handler : sockets.values()) {
            try {
                handler.direct(pack);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Handles one NetworkInterface.
     * Sends heartbeats to the multicast groups for IPv4, IPv6 or both.
     */
    private class InterfaceHandler {

        /**
         * The MulticastSocket.
         */
        private final MulticastSocket socket;
        /**
         * The NetworkInterface to work with.
         */
        private final NetworkInterface networkInterface;
        /**
         * IPv4 heartbeat packet.
         */
        private DatagramPacket pack4;
        /**
         * IPv6 heartbeat packet.
         */
        private DatagramPacket pack6;
        /**
         * Packets must be created after constructor because android does not allow network access on main thread.
         */
        private boolean packetsCreated = false;

        /**
         * Create a new Handler and set up the MulticastSocket.
         *
         * @param interf the target interface.
         * @throws IOException when there is trouble creating the socket
         */
        private InterfaceHandler(final NetworkInterface interf) throws IOException {
            socket = new MulticastSocket(27716);
            socket.setTimeToLive(254);
            socket.setLoopbackMode(true);
            networkInterface = interf;
        }

        /**
         * Multicast now.
         *
         * @throws IOException in case anything goes wrong
         */
        private void multicast() throws IOException {
            if (!packetsCreated) {
                createPackets();
            }
            if (pack4 != null) {
                socket.send(pack4);
            }
            if (pack6 != null) {
                socket.send(pack6);
            }
        }

        /**
         * Just send the given packet.
         *
         * @param packet the packet
         */
        private void direct(DatagramPacket packet) throws IOException {
            if (!packetsCreated) {
                createPackets();
            }
            // protocol type must be supported
            if (packet.getAddress() instanceof Inet4Address && pack4 != null) {
                socket.send(packet);
            }
            if (packet.getAddress() instanceof Inet6Address && pack6 != null) {
                socket.send(packet);
            }
        }

        /**
         * Closes the socket.
         */
        private void close() {
            socket.close();
        }

        private void createPackets() throws IOException {
            packetsCreated = true;
            // figure out if this interface supports IPv4, IPv6 or both
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            boolean v4 = false, v6 = false;
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address) {
                    v4 = true;
                } else if (address instanceof Inet6Address) {
                    v6 = true;
                }
            }
            // configure sockets
            if (v4) {
                socket.setNetworkInterface(networkInterface);
            }
            pack4 = v4 ? new DatagramPacket(packet, packet.length, Inet4Address.getByAddress(new byte[]{(byte) 225, 4, 5, 6}), 27716) : null;
            pack6 = v6 ? new DatagramPacket(packet, packet.length, Inet6Address.getByAddress("ff15::4c61:6e58:6368:616e:6765", new byte[]{(byte) 0xff, (byte) 0x15, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x4c, (byte) 0x61, (byte) 0x6e, (byte) 0x58, (byte) 0x63, (byte) 0x68, (byte) 0x61, (byte) 0x6e, (byte) 0x67, (byte) 0x65}, networkInterface), 27716) : null;
        }

    }
}
