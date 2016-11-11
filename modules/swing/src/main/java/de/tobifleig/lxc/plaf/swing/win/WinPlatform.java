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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.PointerByReference;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.swing.GenericPCPlatform;
import de.tobifleig.lxc.plaf.swing.OverallProgressManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Windows-specific behaviors/settings.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class WinPlatform extends GenericPCPlatform {

    private boolean nativeSupportEnabled = true;
    private boolean taskbarProgressSupported = true;
    private boolean nativeFileDialogsSupported = true;
    private Pointer taskbar;
    private WinDef.HWND hwnd;

    private Thread initThread;


    public WinPlatform(String[] args) {
        // native support can be suppressed with a launch option
        if (Arrays.asList(args).contains("-nonative")) {
            nativeSupportEnabled = false;
        }
        // load dll async
        initThread = new Thread(new Runnable() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void run() {
                if (nativeSupportEnabled) {
                    // extract dll from jar
                    File dllFile = new File("lxcwin.dll");
                    try (InputStream dllStream = ClassLoader.getSystemResourceAsStream("lxcwin.dll")) {
                        if (dllStream == null) {
                            // not found
                            System.out.println("Unable to extract lxcwin.dll, not found");
                        } else {
                            try (FileOutputStream fileOut = new FileOutputStream(new File("lxcwin.dll"))) {
                                int bytes;
                                byte[] buf = new byte[4096];
                                while ((bytes = dllStream.read(buf)) > 0) {
                                    fileOut.write(buf, 0, bytes);
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Unable to extract lxcwin.dll, disabling native support");
                        e.printStackTrace();
                    }
                    if (dllFile.exists() && dllFile.length() == 0) {
                        // extract failure
                        dllFile.delete();
                    }
                    if (!dllFile.exists()) {
                        nativeSupportEnabled = false;
                        return;
                    }

                    try {
                        Lxcwin.INSTANCE.nop(); // does nothing, but loads dll
                        System.out.println("Detected win, enabled advanced windows features");
                    } catch (Throwable ex) {// alloc in native code may throw all sorts of interesting errors
                        ex.printStackTrace();
                        nativeSupportEnabled = false;
                    }
                }
            }
        });
        initThread.setName("lxcwin async load helper");
        initThread.setDaemon(true);
        initThread.start();
        // forward progress changes to taskbar
        gui.setOverallProgressManager(new OverallProgressManager() {
            @Override
            public void notifyOverallProgressChanged(int percentage) {
                try {
                    // prevent race
                    initThread.join();
                } catch (InterruptedException e) {
                    // ignore
                }
                if (nativeSupportEnabled && taskbarProgressSupported) {
                    try {
                        // init on first use
                        if (taskbar == null) {
                            try {
                                taskbar = Lxcwin.INSTANCE.allocTaskbarObject();
                                hwnd = new WinDef.HWND(Native.getWindowPointer(gui));
                            } catch (Error ex) {
                                ex.printStackTrace();
                                taskbarProgressSupported = false;
                                System.out.println("Unable to get window handle, disabling taskbar support.");
                                return;
                            }
                        }
                        // check init worked
                        if (taskbar == null) {
                            System.out.println("Failed to init taskbar");
                            taskbarProgressSupported = false;
                        }
                        if (percentage > 0 && percentage < 100) {
                            Lxcwin.INSTANCE.setProgressValue(taskbar, hwnd, percentage);
                        } else {
                            // disable progress
                            Lxcwin.INSTANCE.setProgressState(taskbar, hwnd, Lxcwin.TBPF_NOPROGRESS);
                        }
                    } catch (UnsatisfiedLinkError | Exception | NoClassDefFoundError ex) {
                        ex.printStackTrace();
                        taskbarProgressSupported = false;
                    }
                }
            }

            @Override
            public void notifySingleProgressChanged() {
                gui.update();
            }
        });
    }

    @Override
    public File getFileTarget(LXCFile file) {
        try {
            // prevent race
            initThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
        if (!nativeSupportEnabled || !nativeFileDialogsSupported) {
            return super.getFileTarget(file);
        }
        String path = null;
        WinNT.HRESULT hr = W32Errors.S_OK;
        try {
            WinDef.HWND hwnd = new WinDef.HWND(Native.getWindowPointer(gui));
            PointerByReference result = new PointerByReference();
            hr = Lxcwin.INSTANCE.fileSaveDialog(hwnd, new WString("Target directory for \"" + file.getShownName() + "\""), result);

            if (W32Errors.SUCCEEDED(hr) && result.getValue() != null) {
                path = result.getValue().getWideString(0);
                Lxcwin.INSTANCE.cleanupSaveDialogResult(result.getValue());
                // result is now invalid
                result = null;
            }
        } catch (Error ex) {
            ex.printStackTrace();
            System.out.println("Error communicating with native file dialog, hr: " + hr.toString());
            nativeFileDialogsSupported = false;
        }

        if (path != null) {
            // windows dialog returned something
            File resultFile = new File(path);
            if (resultFile.canWrite()) {
                return resultFile;
            } else {
                // inform user
                gui.showError("Cannot write there, please selected another target or start LXC as Administrator");
                // cancel
                System.out.println("Canceled, cannot write (permission denied)");
                return null;
            }
        } else {
            // cancel
            System.out.println("Canceled by user.");
            return null;
        }
    }

    @Override
    public File[] openFileForSharing() {
        try {
            // prevent race
            initThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
        if (!nativeSupportEnabled || !nativeFileDialogsSupported) {
            return super.openFileForSharing();
        }
        String[] paths = null;
        WinNT.HRESULT hr = W32Errors.S_OK;
        try {
            WinDef.HWND hwnd = new WinDef.HWND(Native.getWindowPointer(gui));
            PointerByReference result = new PointerByReference();
            WinDef.DWORDByReference countRef = new WinDef.DWORDByReference();
            hr = Lxcwin.INSTANCE.fileOpenDialog(hwnd, countRef, result);

            if (W32Errors.SUCCEEDED(hr) && result.getValue()!= null) {
                paths = result.getValue().getWideStringArray(0);
                Lxcwin.INSTANCE.cleanupOpenDialogResults(countRef.getValue(), result.getValue());
                // result is now invalid
                result = null;
            }
        } catch (Error ex) {
            ex.printStackTrace();
            System.out.println("Error communicating with native file dialog, hr: " + hr.toString());
            nativeFileDialogsSupported = false;
        }

        if (paths != null) {
            // windows dialog returned something
            File[] files = new File[paths.length];
            for (int i = 0; i < paths.length; i++) {
                files[i] = new File(paths[i]);
            }
            return files;
        } else {
            // cancel
            return null;
        }
    }
}
