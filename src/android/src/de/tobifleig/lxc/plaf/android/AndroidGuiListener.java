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
package de.tobifleig.lxc.plaf.android;

import java.util.List;

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiListener;

/**
 * GuiListener for android with some extensions.
 *
 */
public abstract class AndroidGuiListener implements GuiListener {

    private GuiListener basicGuiListener;

    public AndroidGuiListener(GuiListener basicGuiListener) {
        this.basicGuiListener = basicGuiListener;
    }

    /**
     * Called everytime the gui is sent to the background.
     *
     * @param depth 0 for parent, >0 for child activities
     */
    public abstract void guiHidden(int depth);

    /**
     * Called everytime the gui becomes visible (again).
     */
    public abstract void guiVisible(int depth);


    @Override
    public List<LXCFile> getFileList() {
        return basicGuiListener.getFileList();
    }

    @Override
    public void offerFile(LXCFile newFile) {
        basicGuiListener.offerFile(newFile);
    }

    @Override
    public void removeFile(LXCFile oldFile) {
        basicGuiListener.removeFile(oldFile);
    }

    @Override
    public void downloadFile(LXCFile file, boolean chooseTarget) {
        basicGuiListener.downloadFile(file, chooseTarget);
    }

    @Override
    public void resetFile(LXCFile file) {
        basicGuiListener.resetFile(file);
    }

    @Override
    public boolean shutdown(boolean force, boolean askUserOnTransfer) {
        return basicGuiListener.shutdown(force, askUserOnTransfer);
    }

    @Override
    public void reloadConfiguration() {
        basicGuiListener.reloadConfiguration();
    }

}
