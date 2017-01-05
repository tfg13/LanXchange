package de.tobifleig.lxc.plaf.cli.cmds;

import de.tobifleig.lxc.plaf.cli.BackendCommand;
import de.tobifleig.lxc.plaf.cli.BackendCommandType;

/**
 * Lists all files and running transfers.
 */
public class ListCommand extends BackendCommand {

    public ListCommand() {
        super(BackendCommandType.LIST);
    }
}
