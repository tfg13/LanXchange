package de.tobifleig.lxc.plaf.impl;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import de.tobifleig.lxc.Configuration;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.R.layout;
import de.tobifleig.lxc.R.menu;
import de.tobifleig.lxc.data.FileManager;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.Platform;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
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
		setContentView(R.layout.loading);
		LXC lxc = new LXC(this, new String[]{"-nolog"});
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
		return new GuiInterface() {
			
			@Override
			public void update() {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void showError(String error) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setGuiListener(GuiListener guiListener) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void setFileManager(FileManager fileManager) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void init(String[] args) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public File getFileTarget(LXCFile file) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public void display() {
				setContentView(R.layout.lxc_layout);
				
			}
			
			@Override
			public boolean confirmCloseWithTransfersRunning() {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}

	@Override
	public void readConfiguration(String[] args) {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		Map<String, ?> stored = prefs.getAll();
		for (String key : stored.keySet()) {
			Object value = stored.get(key);
			Configuration.putStringSetting(key, value.toString());
		}
	}

	@Override
	public void writeConfiguration() {
		SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE).edit();
		prefs.clear();
		Iterator<String> keyIter = Configuration.getKeyIterator();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			prefs.putString(key, Configuration.getStringSetting(key));
		}
		prefs.commit();
	}

	@Override
	public boolean askForDownloadTargetSupported() {
		return false;
	}

	@Override
	public String getDefaultDownloadTarget() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
			return Environment.getExternalStorageDirectory().getAbsolutePath();
		} else {
			// Bad. Display error message and exit
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
			finish();
			return null; // should not be executed
		}
	}

}
