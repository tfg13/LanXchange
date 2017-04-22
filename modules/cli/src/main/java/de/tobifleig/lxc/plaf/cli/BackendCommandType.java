/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
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
    NOP, // used for frontend-handled commands like STATUS

}
