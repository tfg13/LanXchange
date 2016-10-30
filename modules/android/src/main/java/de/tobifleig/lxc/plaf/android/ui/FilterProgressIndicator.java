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
package de.tobifleig.lxc.plaf.android.ui;

import de.tobifleig.lxc.plaf.ProgressIndicator;

/**
 * A ProgressIndicator, that keeps the GUI-Updates to a minimum.
 *
 * @author Tobias Fleig <tobifleig@googlemail.com>
 */
public abstract class FilterProgressIndicator implements ProgressIndicator {

    /**
     * After how much ms the view is updated.
     */
    public static final int UPDATE_MS = 1000;

    /**
     * Holds the last progress value received via update().
     */
    protected int lastProgress = -1;

    /**
     * The last time the view was updated.
     */
    protected long lastUpdateTime;

    /**
     * Creates a new FilterProgressIndicator with the given initial value.
     */
    public FilterProgressIndicator(int lastProgress) {
        this.lastProgress = lastProgress;
        lastUpdateTime = System.currentTimeMillis();
    }


    @Override
    public void update(int percentage) {
        if (lastProgress != percentage) {
            lastProgress = percentage;
            updateGui();
            lastUpdateTime = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - lastUpdateTime > UPDATE_MS) {
                updateGui();
                lastUpdateTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Must be implemented by subclasses, submits the new value to the GUI.
     */
    protected abstract void updateGui();
}
