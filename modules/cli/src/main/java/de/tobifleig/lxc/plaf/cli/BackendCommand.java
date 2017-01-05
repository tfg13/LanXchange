/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.plaf.cli;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A (parsed) user command, sent from the front- to the backend.
 */
public abstract class BackendCommand implements Serializable {

    private final BackendCommandType type;

    private final List<String> data;

    public BackendCommand(BackendCommandType type) {
        this.type = type;
        data = new ArrayList<>();
    }

    public void addData(String d) {
        data.add(d);
    }

    public BackendCommandType getType() {
        return type;
    }

    /**
     * Tells the frontend if an attempt should be made to start the backend if sending the command initially fails.
     * With this, commands like stop can avoid re-starting already stopped backends.
     * @return true, if backend should be started
     */
    public boolean startBackendOnSendError() {
        return true;
    }
}
