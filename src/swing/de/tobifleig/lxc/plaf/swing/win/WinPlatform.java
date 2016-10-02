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
import com.sun.jna.platform.win32.*;
import de.tobifleig.lxc.plaf.swing.GenericPCPlatform;
import de.tobifleig.lxc.plaf.swing.OverallProgressManager;

/**
 * Windows-specific behaviors/settings.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class WinPlatform extends GenericPCPlatform {

    private Pointer taskbar;
    private WinDef.HWND hwnd;
    private boolean taskbarProgressSupported = false;
    private Thread initThread;


    public WinPlatform() {
        gui.setOverallProgressManager(new OverallProgressManager() {
            @Override
            public void notifyOverallProgressChanged(int percentage) {
                try {
                    // prevent race
                    initThread.join();
                } catch (InterruptedException e) {
                    // ignore
                }
                if (taskbarProgressSupported) {
                    try {
                        // init on first use
                        if (hwnd == null) {
                            try {
                                hwnd = new WinDef.HWND(Native.getWindowPointer(gui));
                            } catch (Error ex) {
                                ex.printStackTrace();
                                taskbarProgressSupported = false;
                                System.out.println("Unable to get window handle, disabling taskbar support.");
                                return;
                            }
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
        if (System.getProperty("os.arch").equals("amd64") || System.getProperty("os.arch").equals("x86_64")) {
            taskbarProgressSupported = true;
        }
        // load dll async
        initThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (taskbarProgressSupported) {
                    try {
                        taskbar = Lxcwin.INSTANCE.allocTaskbarObject();
                        System.out.println("Detected win64, enabled advanced windows features");
                    } catch (Throwable ex) {// alloc in native code may throw all sorts of interesting errors
                        ex.printStackTrace();
                    }
                    // check init worked
                    if (taskbar == null) {
                        System.out.println("Failed to init taskbar");
                        taskbarProgressSupported = false;
                    }
                }
            }
        });
        initThread.setName("lxcwin async load helper");
        initThread.setDaemon(true);
        initThread.start();
    }
}
