/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.plaf.impl.android.activity;

import android.app.Activity;
import de.tobifleig.lxc.plaf.impl.android.service.AndroidSingleton;

public class KeepServiceRunningActivity extends Activity {

    @Override
    public void onPause() {
        super.onPause();
        AndroidSingleton.onMainActivityHidden(1);
    }

    @Override
    public void onResume() {
        super.onResume();
        AndroidSingleton.onMainActivityVisible(1);
    }

}
