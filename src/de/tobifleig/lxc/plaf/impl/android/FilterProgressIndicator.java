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
package de.tobifleig.lxc.plaf.impl.android;

import de.tobifleig.lxc.plaf.ProgressIndicator;

/**
 * A ProgressIndicator, that keeps the GUI-Updates to a minimum.
 *
 * @author Tobias Fleig <tobifleig@googlemail.com>
 */
public abstract class FilterProgressIndicator implements ProgressIndicator {

    /**
     * Holds the last progress value received via update().
     */
    protected int lastProgress = -1;


    @Override
    public void update(int percentage) {
        if (lastProgress != percentage) {
            lastProgress = percentage;
            updateGui();
        }
    }

    /**
     * Must be implemented by subclasses, submits the new value to the GUI.
     */
    protected abstract void updateGui();
}
