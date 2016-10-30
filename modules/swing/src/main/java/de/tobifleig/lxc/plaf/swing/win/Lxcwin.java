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
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

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
     * Does nothing, is called in order to verify the dll was loaded and JNA works.
     */
    void nop();

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
     * Allocates a native Taskbar handling object (ITaskbarList3) and returns a pointer to it.
     *
     * @return pointer to native ITaskbarList3
     */
    Pointer allocTaskbarObject();

    /**
     * Creates a native "Open file" dialog which allows the user to select files for sharing.
     * Due to limitations of the Win32 API, this does not allow selecting files *and* folders,
     * only multiple files.
     *
     * Note: The caller must free the returned native strings by calling cleanupOpenDialogResults.
     *
     * @param hwnd handle of the parent window (this is a modal dialog that blocks its parent)
     * @param count return value: how many files were selected
     * @param result return value: wstring array with file names
     * @return Win32-Style HRESULT with S_OK or the last error code
     */
    WinNT.HRESULT fileOpenDialog(WinDef.HWND hwnd, WinDef.DWORDByReference count, PointerByReference result);

    /**
     * Creates a native "Save file" dialog which allows the user to select the target folder for a transfer.
     * (This is actually an "open" dialog with changed text, because the user must select an existing folder.
     *
     * Note: The caller must free the returned native string by calling cleanupSaveDialogResult.
     *
     * @param hwnd handle of the parent window (this is a modal dialog that blocks its parent)
     * @param dialogTitle window title of the dialog
     * @param result return value: wstring array with file names
     * @return Win32-Style HRESULT with S_OK or the last error code
     */
    WinNT.HRESULT fileSaveDialog(WinDef.HWND hwnd, WString dialogTitle, PointerByReference result);

    /**
     * Frees the WStrings returned by fileOpenDialog.
     * @param count number of strings, as returned by fileOpenDialog
     * @param paths string array, as returned by fileOpenDialog
     */
    void cleanupOpenDialogResults(WinDef.DWORD count, Pointer paths);

    /**
     * Frees the WString returned by fileSaveDialog.
     * @param path string, as returned by fileSaveDialog
     */
    void cleanupSaveDialogResult(Pointer path);
}