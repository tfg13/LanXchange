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
package de.tobifleig.lxc.plaf.impl.swing;

import de.tobifleig.lxc.data.LXCFile;

/**
 * A Listener for Drop-File-onto-LXC-Events
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public interface FileDropListener {

    /**
     * Called, when a drop has been registered and calculation starts.
     */
    public void displayCalcing();

    /**
     * Called, when a file is fully calced and may now be offered to remote clients.
     *
     * @param file the file which size has been calced
     */
    public void newCalcedFile(LXCFile file);
}
