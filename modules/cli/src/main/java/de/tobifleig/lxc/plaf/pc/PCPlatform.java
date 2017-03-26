/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.plaf.pc;

import de.tobifleig.lxc.Configuration;
import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.plaf.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

/**
 * Generic PC platform for all platforms with direct write access to the main directory.
 * Shared by CLI and Swing platforms.
 * Provides automatic updates and manages the config file.
 */
public abstract class PCPlatform implements Platform {

    private static final int MAX_LOG_SIZE_CHARS = 134217728; // ~128MiB
    private static final int LOG_ROTATION_SIZE = 3;

    /**
     * path to cfg-file.
     */
    private static final String CONFIG_PATH = "lxc.cfg";

    public PCPlatform(String[] args) {
        // check permission for own folder
        try {
            File.createTempFile("testacl", null, new File(".")).delete();
            // Can write
        } catch (IOException ex) {
            // Cannot write
            System.err.println("ERROR: Cannot write to my directory ("
                    + new File(".").getAbsolutePath()
                    + "). Try running LXC in your home directory.");
            getGui(args).showError("LXC is not allowed to create/modify files in the folder it is located. Please move to your home directory or start as administrator.");
            System.exit(1);
        }
        // init logging
        boolean debug = false;
        for (String s : args) {
            if (s.equals("-nolog")) {
                debug = true;
                break;
            }
        }
        LXCLogBackend.init(new File("."), MAX_LOG_SIZE_CHARS, LOG_ROTATION_SIZE, debug);
    }

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
        boolean allowDowngrade = false;
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
            }  else if (s.equals("-allowDowngrade")) {
                allowDowngrade = true;
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
            final boolean allowDowngradeF = allowDowngrade;
            // check in separate thread
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        LXCUpdater.checkAndPerformUpdate(getUpdaterGui(), force,
                                noVerification, allowDowngradeF, managed);
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
            showEarlyError("Cannot write to own folder. Please move to your home directory or start as administrator.");
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
        // depends on configuration
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
    public String[] getRequiredMulticastHelpers() {
        return new String[0];
    }

    /**
     * Display critical errors that occurred during startup.
     * @param error the message
     */
    public abstract void showEarlyError(String error);

    /**
     * Returns the updater gui.
     * @return the updater gui
     */
    public abstract UpdaterGui getUpdaterGui();
}
