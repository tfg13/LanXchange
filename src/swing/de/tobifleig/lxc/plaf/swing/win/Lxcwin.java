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
package de.tobifleig.lxc.plaf.swing.win;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;

/**
 * JNA mapping for lxcwin.dll
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
interface Lxcwin extends Library {
    int TBPF_NOPROGRESS	= 0;
    int TBPF_INDETERMINATE	= 1;
    int TBPF_NORMAL	= 2;
    int TBPF_ERROR	= 4;
    int TBPF_PAUSED	= 8;

    Lxcwin INSTANCE = (Lxcwin) Native.loadLibrary("lxcwin", Lxcwin.class);


    /**
     * Sets the progress value, uses the given taskbar object.
     *
     * @param taskbar taskbar object, must be Pointer returned by allocTaskbarObject()
     * @param hwnd    window handle
     * @param percent the progress to set in percent (0-100)
     */
    void setProgressValue(Pointer taskbar, WinDef.HWND hwnd, int percent);

    /**
     * Sets the progress state, uses the given taskbar object.
     *
     * @param taskbar taskbar object, must be Pointer returned by allocTaskbarObject()
     * @param hwnd    window handle
     * @param state   one of: TBFP_*
     */
    void setProgressState(Pointer taskbar, WinDef.HWND hwnd, int state);

    /**
     * Allocs a native Taskbar handling object (ITaskbarList3) and returns a pointer to it.
     *
     * @return pointer to native ITaskbarList3
     */
    Pointer allocTaskbarObject();
}