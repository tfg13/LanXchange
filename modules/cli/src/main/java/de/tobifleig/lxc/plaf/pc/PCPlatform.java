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
import de.tobifleig.lxc.log.LXCLogger;
import de.tobifleig.lxc.plaf.Platform;

import java.io.*;
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

    protected final LXCLogger logger;

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
            getGui(args).showError("LXC is not allowed to create/modify files in the folder it is located. Please move to your home directory or start as administrator.", "");
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
        logger = LXCLogBackend.getLogger("platform");
    }

    @Override
    public boolean hasAutoUpdates() {
        return true;
    }

    @Override
    public void postUpdateStep(String[] args) {
        // default is just file cleanup
        LXCUpdater.cleanup();
    }

    @Override
    public void checkAndPerformUpdates(String[] args) {
        LXCUpdater.Options options = new LXCUpdater.Options();
        // Find out if updating is allowed, requested, forced and/or if the
        // verification should be disabled.
        boolean checkForUpdates = false;
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
        if (Configuration.containsKey("beta")) {
            String result = Configuration.getStringSetting("beta");
            if ("yes".equals(result.toLowerCase())
                    || "true".equals(result.toLowerCase())) {
                options.beta = true;
            }
        }
        // special settings
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s.equals("-update")) {
                checkForUpdates = true;
            } else if (s.equals("-forceupdate")) {
                options.forceUpdate = true;
            } else if (s.equals("-overrideVerification")) {
                options.overrideVerification = true;
            }  else if (s.equals("-allowDowngrade")) {
                options.allowDowngrade = true;
            } else if (s.equals("-managed")) {
                // whoever launched LXC tells us he is able to restart us in
                // case of an update
                options.restartable = true;
            } else if (s.equals("-beta")) {
                options.beta = true;
            } else if (s.equals("-unsafe_updates")) {
                options.unsafe = true;
            } else if (s.equals("-unsafe_url_override") && i + 1 < args.length) {
                options.unsafeUrlOverride = args[i + 1];
                i++;
            } else if (s.equals("-unsafe_disable_tls")) {
                options.unsafeDisableTLS = true;
            } else if (s.equals("-unsafe_internal_version_override") && i + 1 < args.length) {
                options.unsafeOverrideInternalVersion = Integer.parseInt(args[i + 1]);
                i++;
            } else if (s.equals("-unsafe_update_sig_pubkey") && i + 1 < args.length) {
                options.unsafePublicKey = args[i + 1];
                i++;
            }
        }
        if (checkForUpdates || options.forceUpdate) {
            logger.info("Checking for Updates...");
            // check in separate thread
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        LXCUpdater.checkAndPerformUpdate(getUpdaterGui(), options);
                    } catch (Exception ex) {
                        logger.error("Updater crashed", ex);
                    }
                }
            };

            Thread t = new Thread(r);
            t.setName("update checker");
            t.setDaemon(true);
            t.start();
        } else {
            logger.info("Not checking for updates. (disabled via lxc.cfg)");
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
        } catch (IOException ex) {
            // this is serious. exit
            logger.error("Cannot read/write configfile (cfg.txt)", ex);
            // display warning (gui not available yet)
            showEarlyError("Cannot write to own folder. Please move to your home directory or start as administrator.");
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
                writer.append(s);
                writer.append("=");
                writer.append(Configuration.getStringSetting(s));
                writer.append('\n');
            }
        } catch (IOException ex) {
            logger.error("Cannot write config file.", ex);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                // ignore
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
