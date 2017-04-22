/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.plaf.cli.ui;

import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;
import de.tobifleig.lxc.plaf.pc.UpdaterGui;

/**
 * Implements a non-interactive GUI for the updater.
 */
public class CLIUpdaterGui implements UpdaterGui {

    private final LXCLogger logger;

    /**
     * Cached version title
     */
    private String versionTitle = "";

    public CLIUpdaterGui() {
        logger = LXCLogBackend.getLogger("cli-updater-gui");
    }

    @Override
    public void setVersionTitle(String title) {
        this.versionTitle = title;
    }

    @Override
    public void setStatusToVerify() {
        CLITools.out.println("[UPDATER]: Verifying update package...");
    }

    @Override
    public void setStatusToInstall() {
        CLITools.out.println("[UPDATER]: Installing update...");
    }

    @Override
    public void setStatusToRestart() {
        CLITools.out.println("[UPDATER]: Update complete, restart required.");
    }

    @Override
    public void setStatusToError() {
        CLITools.out.println("[UPDATER]: Error! Update failed.");
    }

    @Override
    public void setRestartTime(int i, boolean manual) {
        if (manual) {
            // user fiddled with startup script or bypassed it if this happens
            logger.error("In CLI mode, but not restartable/managed ?!?");
        } else {
            CLITools.out.println("[UPDATER]: Restarting in " + i + "...");
        }
    }

    @Override
    public boolean prompt() {
        return false;
    }

    @Override
    public void toProgressView() {
        CLITools.out.println("[UPDATER]: Downloading update...");
    }

    @Override
    public void finish() {
        // nop
    }
}
