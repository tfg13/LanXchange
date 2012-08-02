/*
 * Copyright 2009, 2010, 2011, 2012 Tobias Fleig (tobifleig gmail com)
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
import java.net.*;
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
    private HashMap<NetworkInterface, MulticastSocket> sockets;
    /**
     * A private timer for sending heartbeats.
     */
    private Timer timer;
    /**
     * The mutlicast-task.
     */
    private TimerTask multicastTask;
    /**
     * The heartbeat-packet. Always the same and therefore cached.
     */
    private byte[] packet;

    /**
     * Creates a new HeartbeatSender
     */
    HeartbeatSender() {
	sockets = new HashMap<NetworkInterface, MulticastSocket>();
	// create packet
	packet = new byte[5];
	int id = LXCInstance.local.id;
	packet[3] = (byte) (id);
	packet[2] = (byte) (id >>> 8);
	packet[1] = (byte) (id >>> 16);
	packet[0] = (byte) (id >>> 24);
	packet[4] = 'h';
    }
    
    /**
     * Starts the HeartbeatSender.
     * Starts sending heartbeats every 20 seconds.
     * Sends 5 heartbeats to get started.
     */
    void start() {
	timer = new Timer();
	// start sending heartbeats
	TimerTask task = new TimerTask() {
	    @Override
	    public void run() {
		sendHeartbeat(-1);
	    }
	};
	timer.schedule(task, 20000, 20000);
	// inital heartbeat
	sendHeartbeat(5);
    }
    
    /**
     * Stops sending heartbeats and sends a special, last offline-beat.
     */
    void stop() {
	// Set packet content to "offline-signal"
	packet[4] = 'o';
	// cancel repeated task:
	multicastTask.cancel();
	// Send one last signal ASAP
	timer.schedule(new TimerTask() {

	    @Override
	    public void run() {
		multicast(packet);
		// shutdown timer thread
		timer.cancel();
	    }
	}, 0);
    }
    
    /**
     * Sends heartbeats.
     * If asyncRepetitions equals -1, sending is synchronous.
     * Otherwise the internal timer is used. In this case there is a delay of one second after each packet.
     * @param asyncRepetitions # of async packets, -1 for 1 sync packet
     */
    void sendHeartbeat(final int asyncRepetitions) {
	if (asyncRepetitions == -1) {
	    // synchronous
	    multicast(packet);
	} else {
	    // asynchronous (multiple times)
	    multicastTask = new TimerTask() {

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
    void updateInterfaces(List<NetworkInterface> interf) {
	// close all interfaces no longer used
	for (NetworkInterface inter : sockets.keySet()) {
	    if (!interf.contains(inter)) {
		sockets.get(inter).close();
		sockets.remove(inter);
	    }
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
	    MulticastSocket sock = new MulticastSocket();
	    sock.setNetworkInterface(inter);
	    sock.setTimeToLive(254);
	    sock.setLoopbackMode(true);
	    sockets.put(inter, sock);
	} catch (SocketException ex) {
	    ex.printStackTrace();
	} catch (IOException ex) {
	    ex.printStackTrace();
	}
    }

    /**
     * Sends a packet via UDP-Multicast to all sockets
     * @param data the packet
     */
    private synchronized void multicast(byte[] data) {
	try {
	    DatagramPacket pack = new DatagramPacket(data, data.length, InetAddress.getByName("225.4.5.6"), 27716);
	    for (MulticastSocket sock : sockets.values()) {
		try {
		    sock.send(pack);
		} catch (IOException ex) {
		    ex.printStackTrace();
		}
	    }
	} catch (UnknownHostException ex) {
	    ex.printStackTrace();
	}

    }
}
