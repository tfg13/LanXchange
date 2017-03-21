/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015 Tobias Fleig (tobifleig gmail com)
 *
 * All rights reserved.
 *
 * This file is part of LanXchange.
 *
 * LanXchange is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LanXchange is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LanXchange. If not, see <http://www.gnu.org/licenses/>.
 */
package de.tobifleig.lxc.plaf.android.activity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.plaf.android.PermissionTools;
import de.tobifleig.lxc.plaf.android.ui.DownloadPathPreference;
import de.tobifleig.lxc.plaf.android.ui.SettingsFragment;

/**
 * Shows settings
 */
public class SettingsActivity extends KeepServiceRunningActivity implements CancelablePermissionPromptActivity {

    private DownloadPathPreference permissionDownloadPathPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // load layout
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public void cancelPermissionPromptAction() {
        permissionDownloadPathPref = null;
        Snackbar.make(findViewById(android.R.id.content), R.string.snackbar_action_cancelled_permission_storage_missing, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == MainActivity.RETURNCODE_PERMISSION_PROMPT_STORAGE) {
            if (grantResults.length == 0) {
                // cancelled, try again
                PermissionTools.verifyStoragePermission(this, this);
                return;
            }
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // continue what the user tried to do when the permission dialog fired
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (permissionDownloadPathPref != null) {
                            permissionDownloadPathPref.prompt();
                            permissionDownloadPathPref = null;
                        }
                    }
                });
                t.setName("lxc_helper_useraction");
                t.setDaemon(true);
                t.start();
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                cancelPermissionPromptAction();
            }
        }
    }

    public boolean verifyStoragePermission(DownloadPathPreference pref) {
        permissionDownloadPathPref = pref;
        boolean result = PermissionTools.verifyStoragePermission(this, this);
        if (result) {
            // delete action cache on success
            permissionDownloadPathPref = null;
        }
        return result;
    }
}
