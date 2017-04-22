/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.plaf.cli;

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.cli.ui.CLITools;

import java.util.HashMap;

/**
 * Assigns a human-readable id to each file.
 * IDs are only valid and unique during the runtime of LanXchange (on one client).
 */
public class LocalFileIDManager {

    /**
     * Counter for file ids.
     */
    private int idCounter = 1;

    /**
     * Contains all known file ids.
     */
    private HashMap<Long, Integer> usedIds = new HashMap<>();

    public void addFile(LXCFile file) {
        CLITools.out.println("DEBUG: ADD ID " + file.id +", internal " + idCounter);
        usedIds.put(file.id, idCounter);
        idCounter++;
    }

    public void removeFile(LXCFile file) {
        CLITools.out.println("DEBUG: REM ID " + file.id +", internal " + usedIds.get(file.id));
        usedIds.remove(file.id);
    }

    public int getId(LXCFile file) {
        if (!usedIds.containsKey(file.id)) {
            return -1;
        }
        return usedIds.get(file.id);
    }
}
