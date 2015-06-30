/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc;

import de.tobifleig.lxc.data.FileListChangeSet;
import de.tobifleig.lxc.data.FileManager;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.net.LXCInstance;
import de.tobifleig.lxc.net.NetworkManager;
import de.tobifleig.lxc.net.NetworkManagerListener;
import de.tobifleig.lxc.net.TransFileList;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.Platform;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * The main class of LanXchange.
 * Contains the main-method.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class LXC {

    /**
     * The internal version id.
     * For automatic updates.
     */
    public static final int versionId = 159;
    /**
     * The external version id.
     */
    public static final String versionString = "v1.31";
    /**
     * The Platform we are running on.
     */
    private final Platform platform;
    /**
     * Our GUI.
     */
    private final GuiInterface gui;
    /**
     * The networkmanager.
     */
    private final NetworkManager network;
    /**
     * The filemanager.
     */
    private final FileManager files;
    /**
     * The default target for downloads.
     */
    private File defaultDownloadTarget;
    /**
     * Use default target or ask.
     */
    private boolean askForDownloadTargetSupported;

    /**
     * Create a new instance of LXC with the given command-line options.
     * Should be called by the main-method only.
     *
     * @param platform the platform LXC is running on
     * @param args command-line args
     */
    public LXC(Platform platform, final String[] args) {
        this.platform = platform;

        initLogging(args);

        System.out.println("This is LanXchange " + versionString + " (" + versionId + ") - Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015 Tobias Fleig - License GPLv3 or later");

        platform.readConfiguration(args);

        if (platform.hasAutoUpdates()) {
            platform.checkAndPerformUpdates(args);
        }

        gui = platform.getGui(args);

        // set up listeners
        initListeners();

        gui.init(args);

        askForDownloadTargetSupported = platform.askForDownloadTargetSupported();
        if (platform.getDefaultDownloadTarget() != null) {
            defaultDownloadTarget = new File(platform.getDefaultDownloadTarget());
        }

        // create components
        files = new FileManager();

        // init networking
        network = new NetworkManager(new NetworkManagerListener() {
            @Override
            public void listReceived(TransFileList list, LXCInstance sender) {
                FileListChangeSet changes = files.computeFileList(list, sender);
                if (!changes.getRemovedFiles().isEmpty()) {
                    // no way of picking multiple files in larger set, must serialize
                    for (FileListChangeSet.FileModification modification : changes.getRemovedFiles()) {
                        gui.notifyFileChange(GuiInterface.UPDATE_ORIGIN_REMOTE, GuiInterface.UPDATE_OPERATION_REMOVE, modification.index, 1);
                    }
                }
                if (!changes.getAddedFiles().isEmpty()) {
                    gui.notifyFileChange(GuiInterface.UPDATE_ORIGIN_REMOTE, GuiInterface.UPDATE_OPERATION_ADD, changes.getAddedFiles().get(0).index, changes.getAddedFiles().size());
                }
            }

            @Override
            public void refreshGui() {
                gui.update();
            }

            @Override
            public void instanceRemoved(LXCInstance removedInstance) {
                FileListChangeSet removals = files.instanceRemoved(removedInstance);
                if (!removals.getRemovedFiles().isEmpty()) {
                    // no way of picking multiple files in larger set, must serialize
                    for (FileListChangeSet.FileModification modification : removals.getRemovedFiles()) {
                        gui.notifyFileChange(GuiInterface.UPDATE_ORIGIN_REMOTE, GuiInterface.UPDATE_OPERATION_REMOVE, modification.index, 1);
                    }
                }
            }

            @Override
            public void downloadComplete(LXCFile file, File targetFolder) {
                LXC.this.platform.downloadComplete(file, targetFolder);
            }

            @Override
            public void downloadFailedFileMissing() {
                gui.showError("At least one file could not be downloaded because it is no longer offered.");
            }

            @Override
            public void uploadFailedFileMissing(LXCFile file) {
                if (file.getFiles().size() == 1) {
                    gui.showError("Uploading \"" + file.getShownName() + "\" failed, LXC cannot locate this file anymore (did you move/delete it?)\n To avoid future errors, this file is no longer offered.");
                } else {
                    gui.showError("Uploading \"" + file.getShownName() + "\" failed, at least one file cannot be located anymore (did you move/delete it?)\n To avoid future errors, these files are no longer offered.");
                }
            }
        }, files, platform);

        // start networking
        if (!network.checkSingletonAndStart()) {
            // It is not possible to run multiple instances at the same time.
            // Warn user and exit.
            gui.showError("LXC is already running!");
            System.exit(1);
        }
        System.out.println("My instance-id is " + LXCInstance.local.id);

        // startup completed, display gui
        gui.display();
    }

    /**
     * Manages logging.
     *
     * @param args the start-params
     */
    private void initLogging(final String[] args) {
        // logging disabled?
        boolean logging = true;
        for (String s : args) {
            if (s.equals("-nolog")) {
                logging = false;
                break;
            }
        }
        // write to logfile
        if (logging) {
            try {
                File logfile = new File("lxc.log");
                if (!logfile.exists()) {
                    logfile.createNewFile();
                }
                PrintStream logger = new PrintStream(logfile);
                System.setOut(logger);
                System.setErr(logger);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("Logging to file disabled.");
        }
    }

    /**
     * Init listeners for all components.
     */
    private void initListeners() {
        gui.setGuiListener(new GuiListener() {
            @Override
            public void offerFile(LXCFile newFile) {
                int newIndex = files.addLocal(newFile);
                if (newIndex != -1) {
                    network.broadcastList();
                    gui.notifyFileChange(GuiInterface.UPDATE_ORIGIN_LOCAL, GuiInterface.UPDATE_OPERATION_ADD, newIndex, 1);
                }
            }

            @Override
            public boolean shutdown(boolean force, boolean askUserOnTransfer, boolean block) {
                if (!force && files.transferRunning()) {
                    // always abort when askUser is false
                    if (!askUserOnTransfer || !gui.confirmCloseWithTransfersRunning()) {
                        return false;
                    }
                }
                if (block) {
                    // synchronous
                    LXC.this.shutdown();
                } else {
                    // asynchronous
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            LXC.this.shutdown();
                        }
                    }, "t_shutdown_helper").start();
                }
                return true;
            }

            @Override
            public void resetFile(LXCFile file) {
                files.resetAvailableFile(file);
            }

            @Override
            public void removeFile(LXCFile oldFile) {
                files.removeLocal(oldFile);
                network.broadcastList();
            }

            @Override
            public void downloadFile(LXCFile file, boolean chooseTarget) {
                File targetFolder = defaultDownloadTarget;
                if (askForDownloadTargetSupported && chooseTarget || defaultDownloadTarget == null) {
                    // let user choose a (different) target
                    targetFolder = gui.getFileTarget(file);
                    if (targetFolder == null) {
                        // abort
                        file.setLocked(false);
                        return;
                    }
                }
                if (!network.connectAndDownload(file, targetFolder)) {
                    gui.showError("Download failed, host unreachable.");
                }
            }

            @Override
            public void reloadConfiguration() {
                askForDownloadTargetSupported = platform.askForDownloadTargetSupported();
                if (platform.getDefaultDownloadTarget() != null) {
                    defaultDownloadTarget = new File(platform.getDefaultDownloadTarget());
                } else {
                    defaultDownloadTarget = null;
                }
            }

            @Override
            public String generateUniqueFileName(String base, String extension) {
                return files.generateUniqueFileName(base, extension);
            }

            @Override
            public List<LXCFile> getFileList() {
                return files.getList();
            }
        });
    }

    /**
     * Stops LanXchange.
     */
    private void shutdown() {
        network.stop();

        // write configuration
        platform.writeConfiguration();

        // done, exit
        System.out.println("LXC done. Thank you.");
    }
}
