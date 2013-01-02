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
package de.tobifleig.lxc.plaf;

import de.tobifleig.lxc.data.FileManager;
import de.tobifleig.lxc.data.LXCFile;
import java.io.File;

/**
 * Defines a LXC-GUI.
 *
 * Enables the same codebase to use Swing or the Android-UI
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public interface GuiInterface {
    
    /**
     * Initialize the user-inferface.
     * @param args LXC's arguments
     */
    public void init(String[] args);

    /**
     * Displays the gui.
     * Must be called after init
     */
    public void display();
    
    /**
     * Displays an error.
     * @param error the message to show
     */
    public void showError(String error);

    /**
     * Sets the listener.
     * Must be called before display().
     * @param guiListener 
     */
    public void setGuiListener(GuiListener guiListener);

    /**
     * updates the gui.
     * must be called after changes (modification of own/av/alllist, file-transfers etc)
     * may use interal scheduling to limit framerate/cpu usage
     */
    public void update();

    /**
     * Finds out where to save the given file.
     * optional, only required if the corresponding guiListener ever calls downloadFile with chooseTarget=true
     * @param file the file that is to be downloaded
     * @return the folder to save this file in
     */
    public File getFileTarget(LXCFile file);

    /**
     * Asks "are you sure" if a user tries to quit LXC while transfers are running.
     * @return true, if user really wants to quit and cancel all running transfers
     */
    public boolean confirmCloseWithTransfersRunning();
}
