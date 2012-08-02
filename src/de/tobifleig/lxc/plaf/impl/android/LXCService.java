package de.tobifleig.lxc.plaf.impl.android;

import de.tobifleig.lxc.R;
import de.tobifleig.lxc.plaf.impl.AndroidPlatform;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class LXCService extends Service {

	private boolean running = false;

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
			
			String ns = Context.NOTIFICATION_SERVICE;
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
			int icon = R.drawable.ic_lxc_running;
			CharSequence tickerText = "LXC Service started!";
			long when = System.currentTimeMillis();

			Notification notification = new Notification(icon, tickerText, when);
			
			Context context = getApplicationContext();
			CharSequence contentTitle = "LanXchange Service Indicator";
			CharSequence contentText = "Service running!";
			Intent notificationIntent = new Intent(this, AndroidPlatform.class);
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);

			startForeground(1, notification);

		}
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		System.out.println("LXC_SERVICE_STOP");
	}

}
