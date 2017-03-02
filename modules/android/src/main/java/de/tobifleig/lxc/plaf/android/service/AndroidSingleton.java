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
package de.tobifleig.lxc.plaf.android.service;

import java.util.List;

import android.content.Intent;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.VirtualFile;
import de.tobifleig.lxc.plaf.android.activity.MainActivity;
import de.tobifleig.lxc.plaf.android.AndroidGuiListener;
import de.tobifleig.lxc.plaf.android.GuiInterfaceBridge;

public class AndroidSingleton {

    private static boolean running = false;
    private static MainActivity activity;
    private static AndroidGuiListener guiListener;
    private static GuiInterfaceBridge genericBridge = new GuiInterfaceBridge() {

        @Override
        public void update() {
            // do nothing
        }

        @Override
        public void notifyFileChange(int fileOrigin, int operation, int firstIndex, int numberOfFiles, List<LXCFile> affectedFiles) {
            // do nothing
        }

        @Override
        public boolean confirmCloseWithTransfersRunning() {
            // no gui = no one to ask = no not close
            return false;
        }
    };
    private static GuiInterfaceBridge currentBridge = genericBridge;
    private static List<VirtualFile> quickShare;

    private AndroidSingleton() {
    }

    /**
     * Call this when the main activity is created. Handles initialization in a
     * way that it is only run if needed.
     */
    public static void onCreateMainActivity(MainActivity activity, GuiInterfaceBridge bridge, List<VirtualFile> quickShare) {
        if (quickShare != null) {
            AndroidSingleton.quickShare = quickShare;
        }
        AndroidSingleton.activity = activity;
        AndroidSingleton.currentBridge = bridge;
        if (!running) {
            running = true;
            activity.startService(new Intent(activity, de.tobifleig.lxc.plaf.android.service.LXCService.class));
        }
        if (guiListener != null) {
            activity.setGuiListener(guiListener);
            sendQuickShare();
        }
    }

    /**
     * Call this when the main activity becomes visible (again).
     */
    public static void onMainActivityVisible(int depth) {
        if (guiListener != null) {
            guiListener.guiVisible(depth);
        }
    }

    /**
     * Call this when the main activity is being destroyed. If the main activity
     * is not re-created soon, the service will be stopped, if there are no
     * running jobs left.
     */
    public static void onMainActivityHidden(int depth) {
        currentBridge = genericBridge;
        if (guiListener != null) {
            guiListener.guiHidden(depth);
        }
        AndroidSingleton.activity = null;
    }

    /**
     * Call this to stop the LXC service.
     *
     */
    public static void onRealDestroy() {
        if (running) {
            running = false;
            guiListener = null;
        }
    }

    /**
     * Called by the main activity if a share-intent was received,
     * but the service is not ready yet (no guilistener available).
     * Effectively a setter for quickShare
     * @param quickShare the files to share once the service becomes available
     */
    public static void onEarlyShareIntent(List<VirtualFile> quickShare) {
        AndroidSingleton.quickShare = quickShare;
    }

    /**
     * Called by the Service on self-stop.
     */
    public static void serviceStopping() {
        running = false;
    }

    /**
     * Called by the Service when LXC is up and running.
     */
    public static void serviceReady(AndroidGuiListener guiListener, int errorCode) {
        AndroidSingleton.guiListener = guiListener;
        if (activity != null) {
            activity.setGuiListener(guiListener);
            sendQuickShare();
            if (errorCode != 0) {
                activity.onErrorCode(errorCode);
            }
        }
    }

    public static GuiInterfaceBridge getInterfaceBridge() {
        return currentBridge;
    }

    private static void sendQuickShare() {
        if (activity != null && quickShare != null) {
            activity.quickShare(quickShare);
            quickShare = null;
        }
    }
}
