package de.tobifleig.lxc.plaf.impl.android;

import de.tobifleig.lxc.data.FileManager;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.impl.AndroidPlatform;
import android.app.Activity;
import android.content.Intent;

public class AndroidSingleton {

	private static boolean running = false;
	private static AndroidPlatform activity;
	private static AndroidSingleton singleton;
	private static GuiListener guiListener;
	private static GuiInterfaceBridge genericBridge = new GuiInterfaceBridge() {
		
		@Override
		public void update() {
			// do nothing
		}
	};
	private static GuiInterfaceBridge currentBridge = genericBridge;
	
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
	public static void onCreateMainActivity(AndroidPlatform activity, GuiInterfaceBridge bridge) {
		AndroidSingleton.activity = activity;
		AndroidSingleton.currentBridge = bridge;
		if (!running) {
			running = true;
			activity.startService(new Intent(activity, de.tobifleig.lxc.plaf.impl.android.LXCService.class));
		} else {
			activity.setGuiListener(guiListener);
		}
	}
	
	/**
	 * Call this when the main activity is being destroyed.
	 * If the main activity is not re-created soon, the service will be stopped, if there are no running jobs left.
	 * @param activity the main activity
	 */
	public static void onDestroy(AndroidPlatform activity) {
		AndroidSingleton.activity = null;
		currentBridge = genericBridge;
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
	
	/**
	 * Called by the Service when LXC is up and running.
	 * @param fileManagern the filemanager, required for the gui
	 */
	public static void serviceReady(GuiListener guiListener) {
		AndroidSingleton.guiListener = guiListener;
		activity.setGuiListener(guiListener);
	}
	
	public static GuiInterfaceBridge getInterfaceBridge() {
		return currentBridge;
	}
}
