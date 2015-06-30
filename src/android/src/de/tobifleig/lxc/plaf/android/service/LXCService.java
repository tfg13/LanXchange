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
package de.tobifleig.lxc.plaf.android.service;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.VirtualFile;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.Platform;
import de.tobifleig.lxc.plaf.android.activity.AndroidPlatform;
import de.tobifleig.lxc.plaf.android.AndroidGuiListener;

public class LXCService extends Service implements Platform {

    /**
     * After how many ms without activity the service should stop itself.
     */
    private final long STOP_SERVICE_MS = 1000 * 60 * 15;// 15 mins
    /**
     * Flat to prevent multiple service instances running at a time.
     */
    private boolean running = false;

    /**
     * Set on some internal errors that need to be communicated to the user.
     * This field is copied to the Activity by AndroidSingleton.
     * The activity then displays a nice error message.
     * Zero means no error.
     */
    private int errorCode = 0;
    /**
     * The listener, used by the user interface to send events to the core implementation.
     */
    private AndroidGuiListener listener;
    /**
     * The MulticastLock. Required to allow LanXchange to receive multicasts over wifi.
     * This drains the battery, so multicasting must be disabled on exit.
     */
    private MulticastLock multicastLock;
    private WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    private Timer timer;
    private TimerTask killTask;
    private boolean[] componentsVisible = new boolean[2];

    public LXCService() {
        timer = new Timer("SERVICE_SHUTDOWN_TIMER", false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;

            /*
             * Create permanent notifiaction that stays active until this Service is stopped.
             * Tapping the notification brings the user to the main activity.
             */

            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.drawable.ic_lxc_running);
            builder.setContentTitle(getResources().getString(R.string.notification_running_title));
            builder.setContentText(getResources().getString(R.string.notification_running_text));

            // configure intent
            Intent notificationIntent = new Intent(this, AndroidPlatform.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            builder.setContentIntent(contentIntent);

            // launch notification
            startForeground(1, builder.getNotification());

            // launch LXC
            startLXC();
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void startLXC() {
        new LXC(this, new String[]{"-nolog"});
    }

    @Override
    public boolean hasAutoUpdates() {
        return false;
    }

    @Override
    public void checkAndPerformUpdates(String[] args) {
        throw new UnsupportedOperationException(
                "this platform does not support automatic updates!");
    }

    @Override
    public GuiInterface getGui(String[] args) {
        return new GuiInterface() {

            @Override
            public void update() {
                AndroidSingleton.getInterfaceBridge().update();
            }

            @Override
            public void notifyFileChange(int fileOrigin, int operation, int firstIndex, int numberOfFiles) {
                AndroidSingleton.getInterfaceBridge().notifyFileChange(fileOrigin, operation, firstIndex, numberOfFiles);
            }

            @Override
            public void showError(String error) {
                System.err.println(error);
            }

            @Override
            public void setGuiListener(final GuiListener guiListener) {
                // delegate everything to the original listener except the new Android Funct
                listener = new AndroidGuiListener(guiListener) {
                    @Override
                    public void guiHidden(int depth) {
                        componentsVisible[depth] = false;
                        triggerTimer();
                    }

                    @Override
                    public void guiVisible(int depth) {
                        componentsVisible[depth] = true;
                        triggerTimer();
                    }

                    @Override
                    public boolean shutdown(boolean force, boolean askUserOnTransfer, boolean block) {
                        boolean superResult = super.shutdown(force, askUserOnTransfer, block);
                        // need to catch this and stop our threads
                        if (superResult) {
                            stopTimer();
                            timer.cancel();
                            stopSelf();
                        }
                        return superResult;
                    }
                };
            }

            @Override
            public void init(String[] args) {
                // acquire multicast lock
                WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                multicastLock = wifi.createMulticastLock("Lanxchange multicastLock");
                multicastLock.setReferenceCounted(false);
                multicastLock.acquire();
                // acquire wifi lock
                wifiLock = wifi.createWifiLock("LanXchange wifilock");
                wifiLock.setReferenceCounted(false);
                wifiLock.acquire();
                // acquire wake lock
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LanXchange wakelock");
                wakeLock.setReferenceCounted(false);
                wakeLock.acquire();
            }

            @Override
            public File getFileTarget(LXCFile file) { // optional, not supported
                throw new UnsupportedOperationException("not supported");
            }

            @Override
            public void display() {
                AndroidSingleton.serviceReady(listener, errorCode);
            }

            @Override
            public boolean confirmCloseWithTransfersRunning() {
                return AndroidSingleton.getInterfaceBridge().confirmCloseWithTransfersRunning();
            }
        };
    }

    /**
     * Call this after componentsVisible has changed
     */
    private void triggerTimer() {
        if (!componentsVisible[0] && !componentsVisible[1]) {
            reScheduleTimer();
        } else {
            stopTimer();
        }
    }

    private void stopTimer() {
        if (killTask != null) {
            killTask.cancel();
            killTask = null;
        }
    }
    private void reScheduleTimer() {
        stopTimer();
        killTask = new TimerTask() {

            @Override
            public void run() {
                if (listener.shutdown(false, false, true)) {
                    System.out.println("LanXchange auto-quits now to preserve your battery");
                    AndroidSingleton.serviceStopping();
                    timer.cancel();
                    stopSelf();
                }
            }
        };

        try {
            timer.schedule(killTask, STOP_SERVICE_MS);
        } catch (IllegalStateException ex) {
            // ignore, app is shutting down
        }
    }

    @Override
    public void readConfiguration(String[] args) {
        /*SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		Map<String, ?> stored = prefs.getAll();
		for (String key : stored.keySet()) {
			Object value = stored.get(key);
			Configuration.putStringSetting(key, value.toString());
		}*/
    }

    @Override
    public void writeConfiguration() {
        /*SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE).edit();
		prefs.clear();
		Iterator<String> keyIter = Configuration.getKeyIterator();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			prefs.putString(key, Configuration.getStringSetting(key));
		}
		prefs.commit();*/
    }

    @Override
    public boolean askForDownloadTargetSupported() {
        return false;
    }

    @Override
    public String getDefaultDownloadTarget() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) { // We can read and write
            // the media
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        } else { // Bad. Display error message and exit
            errorCode = 1;
            return ".";
        }
    }

    @Override
    public void downloadComplete(LXCFile file, File targetFolder) {
        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        List<VirtualFile> baseFiles = file.getFiles();
        String[] paths = new String[baseFiles.size()];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = new File(targetFolder, baseFiles.get(i).getTransferPath()).getAbsolutePath();
        }

        MediaScannerConnection.scanFile(this, paths, null, null);
    }

    @Override
    public void onDestroy() {
        if (multicastLock != null) {
            multicastLock.release();
        }
        if (wifiLock != null) {
            wifiLock.release();
        }
        if (wakeLock != null) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public String[] getRequiredMulticastHelpers() {
	return new String[]{"v4_manual_broadcast", "v6_allnodes_broadcast"};
    }
}
