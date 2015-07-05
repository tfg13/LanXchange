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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Handles known/unknown remote/local instances.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
class InstanceManager {

    /**
     * Instance was detected by the other side, we learned about it when we received a file list.
     */
    public static final String SOURCE_RECEIVED_LIST = "incoming list";
    /**
     * Instance was detected by regular heartbeats.
     */
    public static final String SOURCE_MULTICAST_HEARTBEAT = "multicast heartbeat";
    /**
     * Instance was detected by direct heartbeat.
     * A direct heartbeat is a UDP unicast packet sent to known hosts on the local network.
     */
    public static final String SOURCE_UNICAST_HEARTBEAT = "unicast heartbeat";
    /**
     * Contains all known instances.
     */
    private HashMap<Integer, LXCInstance> instances;
    /**
     * Seems to contain all remote instances, without any duplicates.
     * Backed by the instances HashMap
     */
    private final Iterable<LXCInstance> remoteView;
    /**
     * The listener.
     */
    private InstanceManagerListener listener;
    /**
     * Timer, used to check for time since last heartbeat
     */
    private Timer timer;

    /**
     * Creates a new InstanceManger with the given parameters.
     *
     * @param listener the listener to pass events to
     */
    InstanceManager(InstanceManagerListener listener) {
        this.instances = new HashMap<Integer, LXCInstance>();
        this.listener = listener;
        this.timer = new Timer("lxc_heartbeat_helper", true);
        remoteView = new Iterable<LXCInstance>() {

            @Override
            public Iterator<LXCInstance> iterator() {
                return new Iterator<LXCInstance>() {
                    private final Iterator<LXCInstance> baseIter = instances.values().iterator();
                    private LXCInstance next = computeNext();

                    @Override
                    public boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public LXCInstance next() {
                        try {
                            return next;
                        } finally {
                            next = computeNext();
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported.");
                    }

                    private LXCInstance computeNext() {
                        while (baseIter.hasNext()) {
                            LXCInstance candidate = baseIter.next();
                            if (!candidate.isLocal()) {
                                return candidate;
                            }
                        }
                        return null;
                    }
                };
            }
        };
    }

    /**
     * Starts the InstanceManager.
     * Periodically checks all known instances for timeouts.
     */
    void start() {
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                synchronized (InstanceManager.this) {
                    // check instances
                    ArrayList<LXCInstance> timedOut = new ArrayList<LXCInstance>();
                    for (LXCInstance inst : instances.values()) {
                        if (inst.isLocal()) {
                            continue;
                        }
                        if (System.currentTimeMillis() - inst.getHeartbeatTime() > 60000) {
                            // timeout
                            System.out.println("Instance " + inst.id + " timed out, removing");
                            timedOut.add(inst);
                        }
                    }
                    // delete timeouts
                    for (LXCInstance del : timedOut) {
                        removeInstance(del.id);
                    }
                }
            }
        }, 30000, 30000);
    }

    /**
     * Stops the InstanceManager.
     * Disables periodic checks for timeouts.
     */
    void stop() {
        timer.cancel();
    }

    /**
     * Compute incoming ping-packets.
     * Computing may result in creation of new LXCInstanes.
     *
     * @param data the data
     * @param origin the origin of this ping
     */
    void computePing(byte[] data, InetAddress origin) {
        // unpack id:
        int id = (data[3] & 0xFF)
                + ((data[2] & 0xFF) << 8)
                + ((data[1] & 0xFF) << 16)
                + ((data[0]) << 24);
        // mode:
        if (data[4] == 'h' || data[4] == 'H') {
            // regular heartbeat
            gotHeartbeat(origin, id, data[4] == 'h');
        } else if (data[4] == 'o') {
            // offline
            removeInstance(id);
        }
    }

    /**
     * Returns a Iterable over all known remote LXCInstances.
     * The returned Iterable does not contain a real remote LXCInstance more than once.
     *
     * @return all known distinct remote LXCInstances
     */
    Iterable<LXCInstance> getRemotes() {
        return remoteView;
    }

    /**
     * Gets a LXCInstance for the given id and address
     * If the id is known, the corresponding LXCInstance is returned.
     * Otherwise, a new LXCInstance will be created and returned.
     *
     * @param id the id of the LXCInstance
     * @return the LXCInstance for this id
     */
    synchronized LXCInstance getOrCreateInstance(InetAddress address, int id) {
        if (instances.containsKey(id)) {
            LXCInstance instance = instances.get(id);
            instance.heartBeat(address, SOURCE_RECEIVED_LIST);//saves address if unknown
            return instances.get(id);
        }
        // create new (list detection)
        return addInstance(address, id, SOURCE_RECEIVED_LIST);
    }

    /**
     * Sets the heartbeat-timer if this instance is known.
     * If not, adds it.
     *
     * @param address the address from which the signal was received
     * @param id the id of the remote instance
     */
    synchronized private void gotHeartbeat(InetAddress address, int id, boolean receivedViaMulticast) {
        if (id == LXCInstance.local.id) {
            // ping from self, ignore
            return;
        }
        String source = receivedViaMulticast ? SOURCE_MULTICAST_HEARTBEAT : SOURCE_UNICAST_HEARTBEAT;
        if (instances.containsKey(id)) {
            instances.get(id).heartBeat(address, source);//also adds the address if unknown
        } else {
            addInstance(address, id, source);
        }
    }

    synchronized private LXCInstance addInstance(final InetAddress address, final int id, String source) {
        if (instances.containsKey(id)) {
            LXCInstance instance = instances.get(id);
            instance.heartBeat(address, source);
            return instance;
        }
        // create new:
        LXCInstance newRemote = new LXCInstance(address, id);
        instances.put(id, newRemote);
        System.out.println("New Instance at " + address + " id: " + id + " (detected via: " + source + ")");
        // Send a file list to this new instance
        listener.instanceAdded(newRemote);
        return newRemote;
    }

    /**
     * Removes the instance with the given id.
     */
    synchronized private void removeInstance(int id) {
        Iterator<LXCInstance> iter = instances.values().iterator();
        while (iter.hasNext()) {
            LXCInstance inst = iter.next();
            if (inst.id == id) {
                System.out.println("Instance " + id + " removed");
                iter.remove();
                listener.instanceRemoved(inst);
                break;
            }
        }
    }
}
