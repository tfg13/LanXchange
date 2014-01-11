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
import java.util.List;

/**
 * The Listener for LXC GUIs.
 * Used by LXC to listen for events created by the GUI.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public interface GuiListener {

    /**
     * Called within gui.display().
     * Must return a (non-modifiable) list of all managed files.
     * This method is only called once, so the list must be backed be the real, internal state of FileManager.
     *
     * @return a List containing all managed files.
     */
    public List<LXCFile> getFileList();

    /**
     * Called when the user wants to offer a new LXCFile.
     * Must not be called before the size of the file is fully calculated.
     * GUI-Implementations may want to provide some sort of feedback during calculation,
     * as this may require quite some time for large folders.
     *
     * @param newFile the new LXCFile
     */
    public void offerFile(LXCFile newFile);

    /**
     * Called when the user no longer wants to offer a certain LXCFile.
     *
     * @param oldFile the old LXCFile
     */
    public void removeFile(LXCFile oldFile);

    /**
     * Called when the user wants to download a certain file.
     *
     * @param file the file the user wants to download
     * @param chooseTarget true, if the user wants to choose a file-target even if a default is set
     */
    public void downloadFile(LXCFile file, boolean chooseTarget);

    /**
     * Unsets the "downloaded" (available) flag for a given file.
     * If the file is still available, it can be downloaded again.
     * Otherwise it will disappear.
     *
     * @param file the downloaded, non-local file to reset
     */
    public void resetFile(LXCFile file);

    /**
     * The user wants to quit LXC.
     */
    public void shutdown();

    /**
     * The user changed important settings.
     */
    public void reloadConfiguration();
}
