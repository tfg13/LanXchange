/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.plaf.swing;

import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.plaf.Platform;
import de.tobifleig.lxc.plaf.swing.win.WinPlatform;

import java.io.File;
import java.io.IOException;

/**
 * Launches the desktop (swing) version of LanXchange with the correct Platform.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class Main {

    public static void main(String[] args) {
        Platform platform;
        // figure out platform
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            platform = new WinPlatform(args);
        } else {
            platform = new GenericSwingPlatform(args);
        }

        LXC lxc = new LXC(platform, args);
    }
}
