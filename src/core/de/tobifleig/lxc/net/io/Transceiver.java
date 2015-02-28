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
package de.tobifleig.lxc.net.io;

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.ProgressIndicator;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

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
    private final int[] lastSpeeds = new int[5];
    /**
     * Begin of speed-measurement.
     */
    private final long begin = System.currentTimeMillis();
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
     * The socket for the transfer.
     * Since the streams are created externally, this socket is not required for the transfer itself, but it has to be closed somewhere.
     */
    protected Socket socket;
    /**
     * Used to trigger gui-updates on progress.
     */
    protected TransceiverListener listener;
    /**
     * Used to display information about transfer progress to the user.
     */
    private ProgressIndicator progressIndicator;

    /**
     * Sets the Listener.
     * Must be called before any other methods!
     *
     * @param listener the new listener
     */
    public void setListener(TransceiverListener listener) {
        this.listener = listener;
    }

    /**
     * Returns the current progress of this filetransfer in percent
     *
     * @return the current progress of this filetransfer in percent
     */
    public int getProgress() {
        return (int) (100f * transferedBytes / totalBytes);
    }

    /**
     * Returns the current transfer speed.
     *
     * @return the current transfer speed
     */
    public int getCurrentSpeed() {
        updateSpeeds();
        int avg = lastSpeeds[0] + lastSpeeds[1] + lastSpeeds[2] + lastSpeeds[3] + lastSpeeds[4];
        avg /= 5;
        return avg;
    }

    /**
     * Sets the ProgressIndicator used to display progress of running transfers.
     * Overwrites the old ProgressIndicator, if any.
     *
     * @param progressIndicator the new ProgressIndicator
     */
    public void setProgressIndicator(ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

    /**
     * Called by the transfer routine to signal a progressing transfer.
     */
    protected void updateProgress() {
        if (progressIndicator != null) {
            progressIndicator.update(getProgress());
        }
    }

    /**
     * Recalculates the current speed.
     */
    private void updateSpeeds() {
        long since = System.currentTimeMillis() - begin;
        // calc bytes/sec
        if (since / 1000 != 0) {
            lastSpeeds[nextArrayPos] = (int) (transferedBytes / (since / 1000));
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
