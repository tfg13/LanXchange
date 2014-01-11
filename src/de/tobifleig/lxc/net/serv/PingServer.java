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
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Ping-Server, listens for Pings/Heartbeats
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class PingServer {

    /**
     * The sockets currently in use.
     */
    private HashMap<NetworkInterface, MulticastSocket> listenSockets;
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
        listenSockets = new HashMap<NetworkInterface, MulticastSocket>();
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
                    MulticastSocket socket = new MulticastSocket(27716);
                    socket.setNetworkInterface(inter);
                    socket.joinGroup(InetAddress.getByName("225.4.5.6"));
                    byte[] buffer = new byte[5];
                    DatagramPacket packet = new DatagramPacket(buffer, 5);
                    while (true) {
                        socket.receive(packet); // Blocks
                        // got ping/heartbeat
                        listener.pingReceived(packet.getData(), packet.getAddress());
                    }
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
}
