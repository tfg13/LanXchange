package de.tobifleig.lxc.plaf.impl;

import de.tobifleig.lxc.R;
import de.tobifleig.lxc.R.layout;
import de.tobifleig.lxc.R.menu;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.Platform;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.support.v4.app.NavUtils;

/**
 * Platform for Android / Default Activity
 *
 * no automated updates (managed by Google Play)
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class AndroidPlatform extends Activity implements Platform {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.lxc_layout);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.lxc_layout, menu);
		return true;
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
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void readConfiguration(String[] args) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void writeConfiguration() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean askForDownloadTargetSupported() {
		return false;
	}

	@Override
	public String getDefaultDownloadTarget() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
