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

import de.tobifleig.lxc.data.LXCFile;

import java.io.File;

/**
 * A Listener for NetworkManager.
 * Used by LXC to listen for events created by NetworkManager.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public interface NetworkManagerListener {

    /**
     * A new file list was received.
     *
     * @param list the list
     * @param sender the origin of this list
     */
    public void listReceived(TransFileList list, LXCInstance sender);

    /**
     * Files in the Lists were changed, gui-update required.
     */
    public void refreshGui();

    /**
     * Called after a new Job was added to the given file, important for gui updates.
     * @param file the file the job was added to
     * @param jobIndex index of the new job in the job list
     */
    public void notifyJobAdded(LXCFile file, int jobIndex);

    /**
     * Called before a Job is removed from the given file, important for gui updates.
     * @param file the transferred file
     * @param jobIndex index of the job
     */
    public void notifyRemoveJob(LXCFile file, int jobIndex);

    /**
     * Gets called when the networksystem removes an LXCInstance.
     *
     * @param removedInstance the removed instance
     */
    public void instanceRemoved(LXCInstance removedInstance);

    /**
     * Called when a download was completed successfully.
     *
     * @param file the file
     */
    public void downloadComplete(LXCFile file, File targetFolder);

    /**
     * Called when a upload was aborted because a file no longer exists.
     * Show a message an tell the user whats going on.
     *
     * @param file The file that was removed.
     */
    public void uploadFailedFileMissing(LXCFile file);

    /**
     * Called when a download was aborted because remote could no longer find a file.
     * Show a message an tell the user whats going on.
     */
    public void downloadFailedFileMissing();
}
