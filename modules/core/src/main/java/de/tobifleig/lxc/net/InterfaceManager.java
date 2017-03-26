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

import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;
import de.tobifleig.lxc.net.serv.PingServer;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Manages all NetworkInterfaces.
 * Detects, when interfaces come up/go down.
 *
 * Manages a list of NetworkInterfaces that LXC should listen to / use to send heartbeats.
 * Automatically informs the assigned servers about changes to this list.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class InterfaceManager {

    private final LXCLogger logger;
    /**
     * Server listening for pings/heartbeats.
     */
    private final PingServer pingListener;
    /**
     * Multicast-Sender.
     */
    private final HeartbeatSender pingMulticaster;
    /**
     * Interfaces that are actively used.
     */
    private ArrayList<NetworkInterface> usedInterfaces;
    /**
     * Timer to poll for interface changes.
     */
    private Timer timer;

    /**
     * Creates a new LXCInterfaceManager with the given parameters.
     *
     * @param serv the ping/heartbeat server
     * @param multi the multicaster
     */
    InterfaceManager(PingServer serv, HeartbeatSender multi) {
        logger = LXCLogBackend.getLogger("interface-manager");
        pingListener = serv;
        pingMulticaster = multi;
    }

    /**
     * Iterates over all known interfaces and decides, which should be used.
     */
    private void updateInterfaces(Enumeration<NetworkInterface> interf) {
        try {
            ArrayList<NetworkInterface> nextList = new ArrayList<NetworkInterface>();
            while (interf.hasMoreElements()) {
                NetworkInterface inter = interf.nextElement();
                if (inter.isUp() && !inter.isLoopback() && !inter.isPointToPoint()) {
                    nextList.add(inter);
                }
            }
            // any changes?
            if (!nextList.equals(usedInterfaces)) {
                usedInterfaces = nextList;
                pingListener.updateInterfaces(usedInterfaces);
                pingMulticaster.updateInterfaces(usedInterfaces);
            }
        } catch (SocketException ex) {
            logger.warn("cannot enumerate network interfaces:", ex);
        }
    }

    /**
     * Starts the InterfaceManager.
     * From now on, new NetworkInterfaces will be detected and the assigned servers will be informed about any changes.
     */
    void start() {
        // first execution is synchronous
        try {
            updateInterfaces(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException ex) {
            // ignore
        }
        // repeat async
        timer = new Timer(true);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                try {
                    updateInterfaces(NetworkInterface.getNetworkInterfaces());
                } catch (SocketException ex) {
                    // ignore
                }
            }
        }, 5000, 5000);
    }

    /**
     * Stops the IntefaceManager.
     * Stops polling for network changes immediately.
     * Also simulates removal of all active NetworkInterfaces.
     */
    void stop() {
        timer.cancel();
        updateInterfaces(Collections.<NetworkInterface>emptyEnumeration());
    }
}
