/*
 * Copyright 2009, 2010, 2011, 2012, 2013 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.plaf.impl.android.service;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import de.tobifleig.lxc.plaf.impl.AndroidPlatform;
import de.tobifleig.lxc.plaf.impl.android.AndroidGuiListener;
import de.tobifleig.lxc.plaf.impl.android.GuiInterfaceBridge;

public class AndroidSingleton {

    private static boolean running = false;
    private static AndroidPlatform activity;
    private static AndroidSingleton singleton;
    private static AndroidGuiListener guiListener;
    private static GuiInterfaceBridge genericBridge = new GuiInterfaceBridge() {

        @Override
        public void update() {
            // do nothing
        }

        @Override
        public boolean confirmCloseWithTransfersRunning() {
            // no gui = no one to ask = no not close
            return false;
        }
    };
    private static GuiInterfaceBridge currentBridge = genericBridge;
    private static List<Uri> quickShare;

    private AndroidSingleton() {
    }

    public AndroidSingleton getSingleton() {
        if (singleton == null) {
            singleton = new AndroidSingleton();
        }
        return singleton;
    }

    /**
     * Call this when the main activity is created. Handles initialization in a
     * way that it is only run if needed.
     */
    public static void onCreateMainActivity(AndroidPlatform activity, GuiInterfaceBridge bridge, List<Uri> quickShare) {
        AndroidSingleton.quickShare = quickShare;
        AndroidSingleton.activity = activity;
        AndroidSingleton.currentBridge = bridge;
        if (!running) {
            running = true;
            activity.startService(new Intent(activity, de.tobifleig.lxc.plaf.impl.android.service.LXCService.class));
        } else if (guiListener != null) {
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
     * @param activity
     *            the main activity
     */
    public static void onRealDestroy(Activity activity) {
        if (running) {
            running = false;
            activity.stopService(new Intent(activity, de.tobifleig.lxc.plaf.impl.android.service.LXCService.class));
        }
    }

    /**
     * Called by the Service on self-stop.
     */
    public static void serviceStopping() {
        running = false;
    }

    /**
     * Called by the Service when LXC is up and running.
     * 
     * @param fileManagern
     *            the filemanager, required for the gui
     */
    public static void serviceReady(AndroidGuiListener guiListener) {
        AndroidSingleton.guiListener = guiListener;
        activity.setGuiListener(guiListener);
        sendQuickShare();
    }

    public static GuiInterfaceBridge getInterfaceBridge() {
        return currentBridge;
    }

    private static void sendQuickShare() {
        if (quickShare != null) {
            activity.quickShare(quickShare);
            quickShare = null;
        }
    }
}
