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
package de.tobifleig.lxc.plaf;

import de.tobifleig.lxc.data.LXCFile;
import java.io.File;

/**
 * Provides platform-specific settings and functions such as automatic updates.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public interface Platform {

    /**
     * Weather this platform supports automatic updates.
     *
     * @return true, if supported
     */
    public boolean hasAutoUpdates();

    /**
     * Triggers the platform-specific update system, if any.
     *
     * @throws UnsupportedOperationException if hasAutoUpdate() == false
     * @param args LXC's arguments
     */
    public void checkAndPerformUpdates(String[] args);

    /**
     * Returns the platform-specific user interface.
     *
     * @param args the start-parameters
     * @return the platform-specific user interface
     */
    public GuiInterface getGui(String[] args);

    /**
     * Reads and returns the configuration.
     *
     * @param args
     */
    public void readConfiguration(String[] args);

    /**
     * Writes the configuration.
     */
    public void writeConfiguration();

    /**
     * Weather this platform supports individually set download targets.
     * True implies that the optional method GuiInterface.getFileTarget() is implemented.
     * There may be a default behaviour weather LanXchange should ask for every download or use a default target most of the time.
     * getDefaultDownloadTarget should return a valid path regardless if asking is supported or not.
     * If getDefaultDownloadTarget does not return a valid path, LXC will ask for a target on each download.
     *
     * @return true, if supported.
     */
    public boolean askForDownloadTargetSupported();

    /**
     * Returns the default download target for new downloads.
     * If only one download target is supported, this path is returned.
     * If multiple paths are supported, but there is a default, the default must be returned.
     * If this returns null, "ask-always" is assumed.
     * Returning null when askForDownloadTargetSupported returned false is forbidden.
     *
     * @return see above
     */
    public String getDefaultDownloadTarget();

    /**
     * Informs about finished downloads.
     * Some platforms (like android) need to be informed about this so they can index the new files.
     */
    public void downloadComplete(LXCFile file, File targetFolder);
}
