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
 * Used by the frontend to find out if the backend is running.
 */
public class StartCommand extends BackendCommand {

    /**
     * Whether the backend was started for this command.
     * (If not, the backend prints a "already running" message.
     */
    private final boolean backendLaunched;

    public StartCommand(boolean backendLaunched) {
        super(BackendCommandType.START);
        this.backendLaunched = backendLaunched;
    }

    public boolean isBackendLaunched() {
        return backendLaunched;
    }
}
