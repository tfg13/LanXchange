/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.plaf.swing;

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.pc.PCPlatform;
import de.tobifleig.lxc.plaf.pc.UpdaterGui;

import java.io.File;
import javax.swing.*;

/**
 * A generic Platform for all OSes providing Swing.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class GenericSwingPlatform extends PCPlatform {

    /**
     * the swing-gui.
     */
    protected final SwingGui gui = new SwingGui(this);

    public GenericSwingPlatform(String[] args) {
        super(args);
    }

    @Override
    public boolean hasAutoUpdates() {
        return true;
    }

    @Override
    public GuiInterface getGui(String[] args) {
        return gui;
    }

    @Override
    public void downloadComplete(LXCFile file, File targetFolder) {
        // not required for generic pcs
    }

    @Override
    public void showEarlyError(String error) {
        JOptionPane.showMessageDialog(new JFrame(), error,"Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public UpdaterGui getUpdaterGui() {
        return new UpdateDialog(gui);
    }

    /**
     * Prompts the user for a download target.
     * See same method GuiInterface.
     */
    public File getFileTarget(LXCFile file) {
        // default implementation with swing
        // subclasses may override this to use a native system dialog
        JFileChooser cf = new JFileChooser();
        cf.setApproveButtonText("Choose target");
        cf.setApproveButtonToolTipText("Download files into selected directory");
        cf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        cf.setMultiSelectionEnabled(false);
        cf.setDialogTitle("Target directory for \"" + file.getShownName() + "\"");
        int chooseResult = cf.showDialog(gui, null);
        if (chooseResult == JFileChooser.APPROVE_OPTION) {
            if (cf.getSelectedFile().canWrite()) {
                return cf.getSelectedFile();
            } else {
                // inform user
                gui.showError("Cannot write there, please selected another target or start LXC as Administrator");
                // cancel
                logger.info("Canceled download, cannot write to selected target (permission denied)");
                return null;
            }
        } else {
            // cancel
            logger.info("Download attempt canceled by user.");
            return null;
        }
    }

    /**
     * Prompts the user for files to share.
     */
    public File[] openFileForSharing() {
        // default implementation with swing
        // subclasses may override this to use a native system dialog
        JFileChooser cf = new JFileChooser();
        cf.setApproveButtonText("Share");
        cf.setApproveButtonToolTipText("Share the selected files with LanXchange");
        cf.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        cf.setMultiSelectionEnabled(true);
        cf.setDialogTitle("Select file(s) to share");
        int chooseResult = cf.showDialog(gui, null);
        if (chooseResult == JFileChooser.APPROVE_OPTION) {
            return cf.getSelectedFiles();
        } else {
            // cancel
            return null;
        }
    }
}
