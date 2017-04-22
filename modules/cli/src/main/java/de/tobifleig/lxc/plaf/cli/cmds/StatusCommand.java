/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.plaf.cli.cmds;

import de.tobifleig.lxc.plaf.cli.BackendCommand;
import de.tobifleig.lxc.plaf.cli.BackendCommandType;
import de.tobifleig.lxc.plaf.cli.ui.CLITools;

/**
 * Queries if the backend is running without starting it automatically.
 */
public class StatusCommand extends BackendCommand {

    public StatusCommand() {
        super(BackendCommandType.NOP);
    }

    @Override
    public boolean startBackendOnSendError() {
        return false;
    }

    @Override
    public void onFrontendResult(boolean deliveredToBackend) {
        if (deliveredToBackend) {
            CLITools.out.println("LanXchange is running in the background.");
        } else {
            CLITools.out.println("LanXchange backend is NOT running.");
        }
    }
}
