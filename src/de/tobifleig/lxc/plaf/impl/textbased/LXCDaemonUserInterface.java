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
import java.util.LinkedList;
import java.util.List;

/**
 * The UnserInterface that lets the daemon communicate with lxc.
 *
 * @author Michael
 */
public class LXCDaemonUserInterface implements UserInterface {

    private GuiListener guiListener;
    private List<LXCFile> lxcFileList;
    FileNumberTranslator translator = new FileNumberTranslator();
    private File downloadFolder;

    public String getStatus() {
        String status = "ID NAME                           DESCRIPTION\n";
        for (LXCFile file : lxcFileList) {
            status += translator.getNumberForFile(file) + "  ";
            status += file.getFormattedName() + " ";
            if (file.isLocal()) {
                if (file.getJobs().isEmpty()) {
                    status += "Offered by this machine\n";
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
                if (file.getJobs().isEmpty()) {
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
        lxcFileList = guiListener.getFileList();
    }

    @Override
    public void showError(String error) {
        LxcDaemon.errorBuffer.add(error);
    }

    @Override
    public void setGuiListener(GuiListener guiListener) {
        this.guiListener = guiListener;
    }

    @Override
    public void update() {
        // lxcd is not interactive, so we ignore updates
    }

    @Override
    public File getFileTarget(LXCFile file) {
        return downloadFolder;
    }

    @Override
    public boolean confirmCloseWithTransfersRunning() {
        return true;
    }

    @Override
    public UpdateDialog getUpdateDialog() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String uploadFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return "File not found";
        }
        List<File> files = new LinkedList<File>();
        files.add(file);
        LXCFile lxcFile = new LXCFile(LXCFile.convertToVirtual(files), file.getName());
        guiListener.offerFile(lxcFile);
        return "File uploaded as " + translator.getNumberForFile(lxcFile);
    }

    public String stopUploadFile(int number) {
        LXCFile file = translator.getFileForNumber(number);
        if (file == null || !lxcFileList.contains(file)) {
            return "No such file.";
        } else if (!file.isLocal()) {
            return "File is uploaded from another system.";
        } else {
            guiListener.removeFile(file);
            return "Stopped uploading " + file.getShownName();
        }
    }

    String downloadFile(int number, String target) {
        LXCFile file = translator.getFileForNumber(number);
        if (file == null || !lxcFileList.contains(file)) {
            return "No such file.";
        } else if (!file.isLocal()) {
            if (!target.isEmpty()) {
                downloadFolder = new File(target);
            }
            if (downloadFolder == null) {
                return "Set defaulttarget in lxc.cfg or supply the download target as argument.";
            } else if (!downloadFolder.canWrite()) {
                return "Cant write to " + downloadFolder.getAbsolutePath();
            } else {
                guiListener.downloadFile(file, true);
                return "Downloading " + file.getFormattedName();
            }
        } else {
            return "Cant download files offered from this machine." + file.getShownName();
        }
    }

    public void setDefaultDownloadTarget(String defaultDownloadTarget) {
        if (defaultDownloadTarget != null) {
            downloadFolder = new File(defaultDownloadTarget);
        }
    }
}
