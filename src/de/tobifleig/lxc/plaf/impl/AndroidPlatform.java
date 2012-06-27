/*
 * Copyright 2009, 2010, 2011, 2012 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.plaf.impl;

import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.Platform;

/**
 * Platform for Android.
 * Unfinshed.
 * No automated updates (managed by Google Play/Market)
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class AndroidPlatform implements Platform {

    @Override
    public boolean hasAutoUpdates() {
        return false;
    }

    @Override
    public void checkAndPerformUpdates(String[] args) {
        throw new UnsupportedOperationException("this platform does not support automatic updates!");
    }

    @Override
    public GuiInterface getGui(String[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void readConfiguration(String[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeConfiguration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean askForDownloadTargetSupported() {
	return false;
    }

    @Override
    public String getDefaultDownloadTarget() {
	throw new UnsupportedOperationException("Not supported yet.");
    }

}
