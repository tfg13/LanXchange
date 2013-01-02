/*
 * Copyright 2009, 2010, 2011, 2012, 2013 Tobias Fleig (tobifleig gmail com)
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

/**
 * A Listener for NetworkManager.
 * Used by LXC to listen for events created by NetworkManager.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public interface NetworkManagerListener {
    
    /**
     * A new file list was received.
     * @param list the list
     * @param sender the origin of this list
     */
    public void listReceived(TransFileList list, LXCInstance sender);
    
    /**
     * Files in the Lists were changed, gui-update required.
     */
    public void triggerGui();
    
    /**
     * Gets called when the networksystem removes an LXCInstance.
     * @param removedInstance the removed instance
     */
    public void instanceRemoved(LXCInstance removedInstance);

    /**
     * Called when a download was completed successfully.
     * @param file the file
     */
    public void downloadComplete(LXCFile file);
}
