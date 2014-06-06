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
package de.tobifleig.lxc.plaf.impl.textbased;

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.LXCJob;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.impl.ui.UpdateDialog;
import de.tobifleig.lxc.plaf.impl.ui.UserInterface;
import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * The UnserInterface that lets the daemon communicate with lxc.
 *
 * @author Michael
 */
public class LXCDaemonUserInterface implements UserInterface {

    private GuiListener guiListener;
    private List<LXCFile> files;
    FileNumberTranslator translator = new FileNumberTranslator();

    public String getStatus() {
        String status = "ID NAME                           DESCRIPTION\n";
        for (LXCFile file : files) {
            status += translator.getNumberForFile(file) + "  ";
            status += file.getFormattedName() + " ";
            if (file.isLocal()) {
                if (file.getJobs().size() == 0) {
                    status += " Offered by this machine\n";
                } else {
                    Iterator<LXCJob> iter = file.getJobs().iterator();
                    while (iter.hasNext()) {
                        LXCJob job = iter.next();
                        status += "Sending to " + job.getRemote().getName() + " at " + job.getTrans().getCurrentSpeed() + ", " + job.getTrans().getProgress() + "% complete.\n";
                        if (iter.hasNext()) {
                            status += "                              ";
                        }
                    }
                }

                // TODO show downloaders and their progress
            } else {
                if (file.getJobs().size() == 0) {
                    status += "Offered from " + file.getInstance().getName();
                } else {
                    LXCJob job = file.getJobs().get(0);
                    status += "Downloading from " + file.getInstance().getName() + " at " + job.getTrans().getCurrentSpeed() + ", " + job.getTrans().getProgress() + "% complete.\n";
                }

            }
            status += "\n";
        }
        return status;
    }

    @Override
    public void init(String[] args) {
    }

    @Override
    public void display() {
        files = guiListener.getFileList();
    }

    @Override
    public void showError(String error) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setGuiListener(GuiListener guiListener) {
        this.guiListener = guiListener;
    }

    @Override
    public void update() {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public File getFileTarget(LXCFile file) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean confirmCloseWithTransfersRunning() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UpdateDialog getUpdateDialog() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
