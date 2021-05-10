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
import de.tobifleig.lxc.plaf.swing.DesktopInteractionHelper;
import de.tobifleig.lxc.plaf.swing.GenericSwingPlatform;
import de.tobifleig.lxc.plaf.swing.OverallProgressManager;

import java.io.*;
import java.nio.Buffer;
import java.util.Arrays;

/**
 * Windows-specific behaviors/settings.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class WinPlatform extends GenericSwingPlatform {

    private boolean nativeSupportEnabled = true;
    private boolean taskbarProgressSupported = true;
    private boolean nativeFileDialogsSupported = true;
    private Pointer taskbar;
    private WinDef.HWND hwnd;

    private Thread initThread;


    public WinPlatform(String[] args) {
        super(args);
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
                    try {
                        long startTime = System.currentTimeMillis();
                        Lxcwin.INSTANCE.nop(); // does nothing, but loads dll
                        logger.info("Loading advanced windows lib took " + (System.currentTimeMillis() - startTime) + "ms");
                        logger.info("Detected win, enabled advanced windows features");
                    } catch (Throwable ex) {// alloc in native code may throw all sorts of interesting errors
                        logger.error("Unexpected error in native code", ex);
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
                                taskbarProgressSupported = false;
                                logger.error("Unable to get window handle, disabling taskbar support.", ex);
                                return;
                            }
                        }
                        // check init worked
                        if (taskbar == null) {
                            logger.error("Failed to init taskbar");
                            taskbarProgressSupported = false;
                        }
                        if (percentage > 0 && percentage < 100) {
                            Lxcwin.INSTANCE.setProgressValue(taskbar, hwnd, percentage);
                        } else {
                            // disable progress
                            Lxcwin.INSTANCE.setProgressState(taskbar, hwnd, Lxcwin.TBPF_NOPROGRESS);
                        }
                    } catch (UnsatisfiedLinkError | Exception | NoClassDefFoundError ex) {
                        logger.error("Unable to load lxcwin lib", ex);
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
    public void postUpdateStep(String[] args) {
        // first do the exe jump. if it works run super
        boolean postUpdateFailed = new File("UPDATE_FAILED_MARKER").isFile();
        if (postUpdateFailed) {
            logger.error("update helper signals fatal error");
        }
        // run update-helper if there is data to update (exe is always required)
        if (!postUpdateFailed && new File("tmp_update").isDirectory() && new File("tmp_update/lxc.exe").isFile() && new File("update_helper.exe").isFile())  {
            logger.info("detected previous two-step update, running helper");
            try {
                // start helper and wait for return. The helper immediately forks again to escape the JVM
                ProcessBuilder pb = new ProcessBuilder("update_helper.exe", "-detach");
                Process helper = pb.start();
                int result = helper.waitFor();
                if (result == 0) {
                    logger.info("updater helper start success, exiting...");
                    System.exit(0);
                }
                logger.error("running update helper failed with error code: " + result);
                BufferedReader stdout = new BufferedReader(new InputStreamReader(helper.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(helper.getErrorStream()));
                while (stderr.ready()) {
                    logger.error(stderr.readLine());
                }
                while (stdout.ready()) {
                    logger.error(stdout.readLine());
                }
                postUpdateFailed = true;
            } catch (Exception ex) {
                logger.error("running update helper failed:", ex);
                postUpdateFailed = true;
            }
        }
        if (postUpdateFailed) {
            logger.error("installation looks broken, prompting for redownload");
            // there is a good chance something is super broken now
            // at least display a message with some tips
            if (getGui(args).showError("Automatic update failed :( Sorry, this should not happen.\nIf you want to help, send the content all the logfiles to mail@lanxchange.com.\nLanXchange is probably broken now, press OK to open the website and get a clean copy", "Try running anyway")) {
                DesktopInteractionHelper.openURL("https://lanxchange.com");
                System.exit(1);
            }
            logger.warn("continuing with potentially broken installation by user choice");
        }
        if (!postUpdateFailed) {
            // everything worked, run normal cleanup
            super.postUpdateStep(args);
        }
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
            logger.error("Error communicating with native file dialog, hr: " + hr.toString(), ex);
            nativeFileDialogsSupported = false;
        }

        if (path != null) {
            // windows dialog returned something
            File resultFile = new File(path);
            if (resultFile.canWrite()) {
                return resultFile;
            } else {
                // inform user
                gui.showError("Cannot write there, please selected another target or start LXC as Administrator", "");
                // cancel
                logger.info("Canceled, cannot write (permission denied)");
                return null;
            }
        } else {
            // cancel
            logger.info("Canceled by user.");
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
            logger.error("Error communicating with native file dialog, hr: " + hr.toString(), ex);
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
