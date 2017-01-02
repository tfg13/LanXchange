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
