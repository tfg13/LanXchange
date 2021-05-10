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
import java.util.List;

/**
 * Defines a LXC-GUI.
 *
 * Enables the same codebase to use Swing or the Android-UI
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public interface GuiInterface {

    int UPDATE_ORIGIN_LOCAL = 1;
    int UPDATE_ORIGIN_REMOTE = 2;
    int UPDATE_OPERATION_ADD = 3;
    int UPDATE_OPERATION_REMOVE = 4;

    /**
     * Initialize the user-inferface.
     *
     */
    public void init();

    /**
     * Displays the gui.
     * Must be called after init
     */
    public void display();

    /**
     * Displays an error.
     *
     * @param error the message to show
     * @param cancelText text on "cancel" button. Will only be shown if non-empty
     *
     * @return true if normal action was taken, false if cancel was chosen
     */
    public boolean showError(String error, String cancelText);

    /**
     * Sets the listener.
     * Must be called before display().
     *
     * @param guiListener
     */
    public void setGuiListener(GuiListener guiListener);

    /**
     * Updates the gui.
     * must be called after changes (modification of own/av/alllist, file-transfers etc)
     * may use interal scheduling to limit framerate/cpu usage
     *
     * If more specific info is available, one can also call notifyFileChange().
     */
    public void update();

    /**
     * More specific way of updating the gui.
     * Gives the gui the opportunity to selectively update the view and/or animate the changes.
     * This can be called instead of update()
     *
     * @param fileOrigin one of UPDATE_ORIGIN_LOCAL, UPDATE_ORIGIN_REMOTE
     * @param operation one of UPDATE_OPERATION_ADD, UPDATE_OPERATION_REMOVE
     * @param firstIndex index of first modified file (index of first removed or future index of first new)
     * @param numberOfFiles number of files that were added/removed in this batch
     * @param affectedFiles the changed files
     */
    public void notifyFileChange(int fileOrigin, int operation, int firstIndex, int numberOfFiles, List<LXCFile> affectedFiles);

    /**
     * Fired to notify the gui of new/removed jobs.
     * Gives the gui the opportunity ot selectively update the view and/or animate the changes.
     * This can be called instead of update()
     *
     * @param operation one of UPDATE_OPERATION_ADD, UPDATE_OPERATION_REMOVE
     * @param file the transferred LXCFile
     * @param index the index of the modified job (withing the file) - (index of removed or index of new)
     */
    public void notifyJobChange(int operation, LXCFile file, int index);

    /**
     * Finds out where to save the given file.
     * optional, only required if the corresponding guiListener ever calls downloadFile with chooseTarget=true
     *
     * @param file the file that is to be downloaded
     * @return the folder to save this file in
     */
    public File getFileTarget(LXCFile file);

    /**
     * Asks "are you sure" if a user tries to quit LXC while transfers are running.
     *
     * @return true, if user really wants to quit and cancel all running transfers
     */
    public boolean confirmCloseWithTransfersRunning();
}
