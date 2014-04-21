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
package de.tobifleig.lxc.plaf.impl.android;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.IBinder;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.VirtualFile;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.Platform;
import de.tobifleig.lxc.plaf.impl.AndroidPlatform;

public class LXCService extends Service implements Platform {

    /**
     * After how many ms without activity the service should stop itself.
     */
    private final long STOP_SERVICE_MS = 1000 * 60 * 15;// 15 mins
    /**
     * Flat to prevent multiple service instances running at a time.
     */
    private boolean running = false;
    //private LXC lxc;
    /**
     * The listener, used by the user interface to send events to the core implementation.
     */
    private AndroidGuiListener listener;
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
            public void showError(String error) {
                System.err.println(error);
                System.out.println("FIXME: Error printed to stderr");
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
                    public boolean shutdown(boolean force, boolean askUserOnTransfer) {
                        boolean superResult = super.shutdown(force, askUserOnTransfer);
                        // need to catch this and stop our threads
                        if (superResult) {
                            stopTimer();
                            timer.cancel();
                        }
                        return superResult;
                    }
                };
            }

            @Override
            public void init(String[] args) {
                // not required for android
            }

            @Override
            public File getFileTarget(LXCFile file) { // optional, not supported
                throw new UnsupportedOperationException("not supported");
            }

            @Override
            public void display() {
                AndroidSingleton.serviceReady(listener);
            }

            @Override
            public boolean confirmCloseWithTransfersRunning() { // TODO
                return AndroidSingleton.getInterfaceBridge().confirmCloseWithTransfersRunning();
            }
        };
    }

    /**
     * Call this after componentsVisible has changed
     */
    private void triggerTimer() {
        if (componentsVisible[0] == false && componentsVisible[1] == false) {
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
                if (listener.shutdown(false, false)) {
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
        System.out.println("FixMe: Implement readConfiguration");
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
        System.out.println("FixMe: Implement writeConfiguration");
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Cannot access data ");
            builder.setCancelable(false);
            builder.setNeutralButton("OK", new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            builder.create();
            throw new RuntimeException("Cannot write to storage!");
        }
    }

    @Override
    public void downloadComplete(LXCFile file, File targetFolder) {
        // TODO Auto-generated method stub
        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        List<VirtualFile> baseFiles = file.getFiles();
        String[] paths = new String[baseFiles.size()];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = new File(targetFolder, baseFiles.get(i).getTransferPath()).getAbsolutePath();
        }

        MediaScannerConnection.scanFile(this, paths, null, null);
    }
}
