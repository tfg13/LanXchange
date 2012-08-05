package de.tobifleig.lxc.plaf.impl.android;

import java.io.File;
import java.util.List;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.IBinder;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.Platform;
import de.tobifleig.lxc.plaf.impl.AndroidPlatform;

public class LXCService extends Service implements Platform {

	private boolean running = false;
	private LXC lxc;
	private GuiListener listener;

	public LXCService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (!running) {
			running = true;
			System.out.println("LXC_SERVICE START");
			
			int icon = R.drawable.ic_lxc_running;
			CharSequence tickerText = "LanXchange running";
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);
			
			Context context = getApplicationContext();
			CharSequence contentTitle = "LanXchange running";
			CharSequence contentText = "Tap to return to LanXchange";
			Intent notificationIntent = new Intent(this, AndroidPlatform.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			
			startForeground(1, notification);

			startLXC();
		}
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		System.out.println("LXC_SERVICE_STOP");
		listener.shutdown();
	}
	
	private void startLXC() {
		lxc = new LXC(this, new String[]{"-nolog"});
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
			public void setGuiListener(GuiListener guiListener) {
				listener = guiListener;
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
				System.out
						.println("FIXME: Check for running downloads before exiting");
				return true;
			}
		};
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
	public void downloadComplete(LXCFile file) {
		// TODO Auto-generated method stub
		// Tell the media scanner about the new file so that it is
        // immediately available to the user.
		List<File> baseFiles = file.getFiles();
		String[] paths = new String[baseFiles.size()];
		for (int i = 0; i < paths.length; i++) {
			paths[i] = baseFiles.get(i).getAbsolutePath();
		}

		MediaScannerConnection.scanFile(this, paths, null, null);
	}
}
