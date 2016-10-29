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

import de.tobifleig.lxc.Configuration;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.Platform;
import de.tobifleig.lxc.plaf.swing.LXCUpdater;
import de.tobifleig.lxc.plaf.swing.SwingGui;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;

/**
 * A generic Platform for all OSes providing Swing and direct write access to the LXC directory.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class GenericPCPlatform implements Platform {

    /**
     * path to cfg-file.
     */
    private static final String CONFIG_PATH = "lxc.cfg";
    /**
     * the swing-gui.
     */
    protected final SwingGui gui = new SwingGui(this);

    @Override
    public boolean hasAutoUpdates() {
        return true;
    }

    @Override
    public void checkAndPerformUpdates(String[] args) {
        // Find out if updating is allowed, requested, forced and/or if the
        // verification should be disabled.
        boolean checkForUpdates = false;
        boolean forceUpdate = false;
        boolean overrideVerification = false;
        boolean restartable = false;
        if (Configuration.containsKey("allowupdates")) {
            String result = Configuration.getStringSetting("allowupdates");
            if ("yes".equals(result.toLowerCase())
                    || "true".equals(result.toLowerCase())) {
                checkForUpdates = true;
            }
        } else {
            // not set yet, default to true (yes)
            checkForUpdates = true;
            Configuration.putStringSetting("allowupdates", "true");
        }
        // special settings
        for (String s : args) {
            if (s.equals("-update")) {
                checkForUpdates = true;
            } else if (s.equals("-forceupdate")) {
                forceUpdate = true;
            } else if (s.equals("-overrideVerification")) {
                overrideVerification = true;
            } else if (s.equals("-managed")) {
                // whoever launched LXC tells us he is able to restart us in
                // case of an update
                restartable = true;
            }
        }
        if (checkForUpdates || forceUpdate) {
            System.out.println("Checking for Updates...");
            // Ugly workaround, anonymous inner classes require (local)
            // variables to be final
            final boolean force = forceUpdate;
            final boolean noVerification = overrideVerification;
            final boolean managed = restartable;
            // check in separate thread
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        LXCUpdater.checkAndPerformUpdate(gui, force,
                                noVerification, managed);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };

            Thread t = new Thread(r);
            t.setName("update checker");
            t.setDaemon(true);
            t.start();
        } else {
            System.out.println("Not checking for updates. (disabled via lxc.cfg)");
        }
    }

    @Override
    public GuiInterface getGui(String[] args) {
        return gui;
    }

    @Override
    public void readConfiguration(String[] args) {
        BufferedReader reader = null;
        try {
            File cfgFile = new File(CONFIG_PATH);
            // create on first start
            if (!cfgFile.exists()) {
                cfgFile.createNewFile();
            }
            reader = new BufferedReader(new FileReader(cfgFile));
            String line;
            int i = 0; // line number
            while ((line = reader.readLine()) != null) {
                // read line after line, add content to Configuration
                int equalSignIndex = line.indexOf('='); // search for "="
                if (equalSignIndex == -1) {
                } else {
                    String v1 = line.substring(0, equalSignIndex); // prefix (before "=")
                    String v2 = line.substring(equalSignIndex + 1); // suffix (after "=")
                    Configuration.putStringSetting(v1, v2);
                }
            }
        } catch (FileNotFoundException e1) {
            // this is serious. exit
            System.out.println("Cannot read/write configfile (cfg.txt)");
            // display warning (gui not available yet)
            JOptionPane.showMessageDialog(new JFrame(), "Cannot write to own folder. Please move to your home directory or start as administrator.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (IOException e2) {
            e2.printStackTrace();
            System.exit(1);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    @Override
    public void writeConfiguration() {
        FileWriter writer = null;
        try {
            writer = new FileWriter(CONFIG_PATH);
            Iterator<String> iter = Configuration.getKeyIterator();
            while (iter.hasNext()) {
                String s = iter.next();
                writer.append(s.toString() + "="
                        + Configuration.getStringSetting(s) + '\n');
            }
        } catch (IOException ex) {
            System.out.println("CRITICAL: ERROR WRITING TO LOGFILE!");
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    @Override
    public boolean askForDownloadTargetSupported() {
        return true;
    }

    @Override
    public String getDefaultDownloadTarget() {
        // depends on confituration
        if (!Configuration.containsKey("defaulttarget")) {
            return null; // default = always ask
        } else {
            // check if valid
            File file = new File(
                    Configuration.getStringSetting("defaulttarget"));
            if (file.isDirectory() && file.canWrite()) {
                return file.getAbsolutePath();
            }
        }
        return null; // invalid path = ask
    }

    @Override
    public void downloadComplete(LXCFile file, File targetFolder) {
        // not required for generic pcs
    }

    @Override
    public String[] getRequiredMulticastHelpers() {
        return new String[0];
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
                System.out.println("Canceled, cannot write (permission denied)");
                return null;
            }
        } else {
            // cancel
            System.out.println("Canceled by user.");
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
