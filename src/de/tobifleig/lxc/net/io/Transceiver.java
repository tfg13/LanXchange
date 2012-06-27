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
package de.tobifleig.lxc.net.io;

import de.tobifleig.lxc.data.LXCFile;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Superclass for Leecher and Seeder
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public abstract class Transceiver implements Runnable {

    /**
     * The LXCFile that is to be transfered.
     */
    protected LXCFile file;
    /**
     * The total number of bytes to be transfered.
     */
    protected long totalBytes;
    /**
     * The current number of transfered bytes.
     */
    protected long transferedBytes = 0;
    /**
     * Which protocol version to use.
     */
    protected int transVersion;
    /**
     * Buffer for five speed-measurements.
     */
    private int[] lastSpeeds = new int[5];
    /**
     * Time of last speed-measurement.
     */
    private long lastMeasure = System.currentTimeMillis();
    /**
     * The number of transfered bytes at the time stored in lastMeasure.
     */
    private long lastTransferedBytes = 0;
    /**
     * A mod-5-counter.
     * Used for the rolling-average speed-calculations
     */
    private int nextArrayPos = 0;
    /**
     * The OutputStream.
     * Implementations must create and connect this stream.
     */
    protected ObjectOutputStream out;
    /**
     * The InputStream.
     * Implementations must create and connect this stream.
     */
    protected ObjectInputStream in;
    /**
     * Used to trigger gui-updates on progress.
     */
    protected TransceiverListener listener;
    
    /**
     * Sets the Listener.
     * Must be called before any other methods!
     * @param listener the new listener
     */
    public void setListener(TransceiverListener listener) {
        this.listener = listener;
    }

    /**
     * Returns the current progress of this filetransfer.
     * @return the current progress of this filetransfer. 
     */
    public float getProgress() {
        return (1.0f * transferedBytes / totalBytes);
    }

    /**
     * Returns the current transfer speed.
     * @return the current transfer speed
     */
    public int getCurrentSpeed() {
        updateSpeeds();
        int avg = lastSpeeds[0] + lastSpeeds[1] + lastSpeeds[2] + lastSpeeds[3] + lastSpeeds[4];
        avg /= 5;
        return avg;
    }

    /**
     * Recalculates the current speed.
     */
    private void updateSpeeds() {
        // bytes since last frame
        long newBytes = transferedBytes - lastTransferedBytes;
        // time since last frame
        long within = System.currentTimeMillis() - lastMeasure;
        // calc bytes/sec
        if (within / 1000 != 0) {
            lastSpeeds[nextArrayPos] = (int) (newBytes / (within / 1000));
        } else {
            lastSpeeds[nextArrayPos] = 0;
        }
        // increment rolling counter
        if (++nextArrayPos >= 5) {
            nextArrayPos = 0;
        }
    }
    
    /**
     * Aborts the transfer.
     * Has no effect if the transfer is already finished.
     */
    public abstract void abort();
    
    /**
     * Starts this Transceiver.
     * Creates a new thread, returns immediately.
     */
    public abstract void start();
}
