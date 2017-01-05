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

/**
 * Command to stop the backend and shut down LanXchange.
 */
public class StopCommand extends BackendCommand {

    private final boolean force;

    public StopCommand(boolean force) {
        super(BackendCommandType.STOP);
        this.force = force;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public boolean startBackendOnSendError() {
        return false;
    }
}
