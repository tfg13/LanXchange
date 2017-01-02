package de.tobifleig.lxc.plaf.cli;

/**
 * All commands the backend supports.
 */
public enum BackendCommandType {

    START, // pseudo-command does nothing, but is used by the frontend to test whether the backend is already running
    STOP, // stops LanXchange
    LIST, // lists files and transfers
    GET,  // downloads a file
    SHARE, // offers a file
    ABORT, // aborts a transfer
    VERSION, // displays version info

}
