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
package de.tobifleig.lxc.data;

import de.tobifleig.lxc.net.LXCInstance;
import de.tobifleig.lxc.net.io.Seeder;
import de.tobifleig.lxc.net.io.Transceiver;

/**
 * Represents a running transfer.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class LXCJob {

    /**
     * If this job represents a running upload (true) or download (false).
     */
    private boolean isSeeder = false;
    /**
     * The remote LXCInstance.
     */
    private final LXCInstance remote;
    /**
     * The corresponding Transceiver.
     */
    private final Transceiver trans;

    /**
     * Creates a new LXCJob with the given parameters.
     *
     * @param trans the running transceiver that transfers the data
     * @param remote the connected remote
     */
    public LXCJob(Transceiver trans, LXCInstance remote) {
        this.trans = trans;
        this.remote = remote;
        isSeeder = trans instanceof Seeder;
    }

    /**
     * Returns true, if this job represents a running upload.
     *
     * @return true if upload, false if download
     */
    public boolean isIsSeeder() {
        return isSeeder;
    }

    /**
     * Returns the remote LXCInstance.
     *
     * @return the remote LXCInstance
     */
    public LXCInstance getRemote() {
        return remote;
    }

    /**
     * Returns the running Transceiver.
     *
     * @return the Transceiver
     */
    public Transceiver getTrans() {
        return trans;
    }

    /**
     * Immediately aborts the transfer.
     */
    public void abortTransfer() {
        trans.abort();
    }
}
