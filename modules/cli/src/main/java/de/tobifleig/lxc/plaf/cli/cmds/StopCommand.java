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
