package de.tobifleig.lxc.plaf.impl.android;

import android.app.Activity;
import android.content.Intent;

public class AndroidSingleton {

	private static boolean running = false;
	private static AndroidSingleton singleton;
	
	private AndroidSingleton() {
	}
	
	public AndroidSingleton getSingleton() {
		if (singleton == null) {
			singleton = new AndroidSingleton();
		}
		return singleton;
	}
	
	/**
	 * Call this when the main activity is created.
	 * Handles initialization in a way that it is only run if needed.
	 */
	public static void onCreateMainActivity(Activity activity) {
		if (!running) {
			running = true;
			activity.startService(new Intent(activity, de.tobifleig.lxc.plaf.impl.android.LXCService.class));
		}
	}
	
	/**
	 * Call this when the main activity is being destroyed.
	 * If the main activity is not re-created soon, the service will be stopped, if there are no running jobs left.
	 * @param activity the main activity
	 */
	public static void onDestroy(Activity activity) {
		//TODO: Add some clever code here...
	}
	
	/**
	 * Call this to stop the LXC service.
	 * @param activity the main activity
	 */
	public static void onRealDestroy(Activity activity) {
		if (running) {
			running = false;
			activity.stopService(new Intent(activity, de.tobifleig.lxc.plaf.impl.android.LXCService.class));
		}
	}
}
