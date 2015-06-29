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
import java.util.Random;

/**
 * Represents a running instance of LXC somewhere in the local network.
 * May also represent the local instance.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class LXCInstance {

    /**
     * The id of this instance.
     * There must not be any duplicates throughout the local network
     */
    public final int id;
    /**
     * Contains all the addresses under which this instance is known.
     * Must contains at least one address.
     */
    private final ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();
    /**
     * A human-readable name.
     */
    private String name;
    /**
     * Only true, if this LXCInstance represents the local instance.
     */
    private final boolean isLocal;
    /**
     * The time when the last heartbeat was received.
     * Unused for local instances
     */
    long heartbeatTime;
    /**
     * The static accessible, local instance.
     */
    public static final LXCInstance local = new LXCInstance();

    /**
     * Creates a new remote LXCInstance with the given parameters.
     *
     * @param initialAddress the first known address of this instance
     * @param id the unique id, as transfered by the instance itself
     */
    LXCInstance(final InetAddress initialAddress, final int id) {
        this.id = id;
        addresses.add(initialAddress);
        heartbeatTime = System.currentTimeMillis();
        isLocal = false;
        // set a basic name and start a lookup
        name = String.valueOf(id);
        lookupName(initialAddress);
    }

    /**
     * Creates a local LXCInstance.
     */
    private LXCInstance() {
        Random r = new Random();
        r.setSeed(System.currentTimeMillis());
        this.id = r.nextInt();
        name = "localhost";
        isLocal = true;
    }

    /**
     * Returns an address of this LXCInstance useable for downloads.
     *
     * @return an address of this LXCInstance
     */
    InetAddress getDownloadAddress() {
        /*
         * It may be a GoodIdea(tm) to add some code here that picks the best address, not just the first.
         * Example: If a remote instance is reachable by WLAN and LAN, you want the download to happen over LAN.
         */
        return addresses.get(0);
    }

    /**
     * Adds the given address to the list of known addresses.
     *
     * @param address the new address
     */
    void addAddress(InetAddress address) {
        if (!addresses.contains(address)) {
            addresses.add(address);
        }
    }

    /**
     * Returns true, if this LXCInstance represents the local instance.
     *
     * @return true, if local
     */
    public boolean isLocal() {
        return isLocal;
    }

    /**
     * Returns the time when the last heartbeat was received.
     *
     * @return the time of the last heartbeat
     */
    long getHeartbeatTime() {
        return heartbeatTime;
    }

    /**
     * Call this to indicate a heartbeat was received.
     */
    void heartBeat(InetAddress address) {
        if (!addresses.contains(address)) {
            addresses.add(address);
        }
        heartbeatTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return id + " " + name;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LXCInstance) {
            LXCInstance i = (LXCInstance) o;
            return i.id == this.id;
        }
        return false;
    }

    /**
     * Returns the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets a human-readable name for this remote instance.
     * Performs a asynchronous lookup in a seperate thread.
     */
    private void lookupName(final InetAddress initialAddress) {
        Thread lookupThread = new Thread(new Runnable() {

            @Override
            public void run() {
                name = initialAddress.getHostName();
            }
        }, "lxc_helper_lookup" + this.id);
        lookupThread.setDaemon(true);
        lookupThread.start();
    }
}
