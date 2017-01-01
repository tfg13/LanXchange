/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.plaf.pc;

/**
 * Interface for everything required by the updater.
 */
public interface UpdaterGui {

    void setVersionTitle(String title);
    void setStatusToVerify();
    void setStatusToInstall();
    void setStatusToRestart();
    void setStatusToError();
    void setRestartTime(int i, boolean manual);
    boolean isUpdate();
    void toProgressView();
    void finish();
}
