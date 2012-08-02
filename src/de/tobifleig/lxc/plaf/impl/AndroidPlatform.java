package de.tobifleig.lxc.plaf.impl;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.plaf.impl.android.AndroidSingleton;

/**
 * Platform for Android / Default Activity
 * 
 * no automated updates (managed by Google Play)
 * 
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class AndroidPlatform extends ListActivity {

	private LayoutInflater infl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView emptyText = new TextView(this);
		emptyText.setText("loading...");
		emptyText.setGravity(Gravity.CENTER);

		getListView().setEmptyView(emptyText);

		ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
		root.addView(emptyText);

		infl = (LayoutInflater) getBaseContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);

		getListView().setAdapter(new ListAdapter() {

			@Override
			public void unregisterDataSetObserver(DataSetObserver arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void registerDataSetObserver(DataSetObserver arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isEmpty() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean hasStableIds() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public int getViewTypeCount() {
				// TODO Auto-generated method stub
				return 1;
			}

			@Override
			public View getView(int arg0, View arg1, ViewGroup arg2) {
				View item = infl.inflate(R.layout.file_item, arg2, false);
				((TextView) item.findViewById(R.id.filename)).setText("File "
						+ arg0);
				return item;
			}

			@Override
			public int getItemViewType(int arg0) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public long getItemId(int arg0) {
				// TODO Auto-generated method stub
				return arg0;
			}

			@Override
			public Object getItem(int arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return 100;
			}

			@Override
			public boolean isEnabled(int position) {
				return true;
			}

			@Override
			public boolean areAllItemsEnabled() {
				return true;
			}
		});

		AndroidSingleton.onCreateMainActivity(this);

		// LXC lxc = new LXC(this, new String[]{"-nolog"});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.lxc_layout, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item1:
			AndroidSingleton.onRealDestroy(this);
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	/*
	 * @Override public boolean hasAutoUpdates() { return false; }
	 * 
	 * @Override public void checkAndPerformUpdates(String[] args) { throw new
	 * UnsupportedOperationException(
	 * "this platform does not support automatic updates!"); }
	 * 
	 * @Override public GuiInterface getGui(String[] args) { return new
	 * GuiInterface() {
	 * 
	 * @Override public void update() { // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * @Override public void showError(String error) {
	 * System.err.println(error);
	 * System.out.println("FIXME: Error printed to stderr"); }
	 * 
	 * @Override public void setGuiListener(GuiListener guiListener) { listener
	 * = guiListener; }
	 * 
	 * @Override public void setFileManager(FileManager fileManager) { files =
	 * fileManager; }
	 * 
	 * @Override public void init(String[] args) { // not required for android }
	 * 
	 * @Override public File getFileTarget(LXCFile file) { // optional, not
	 * supported throw new UnsupportedOperationException("not supported"); }
	 * 
	 * @Override public void display() { setContentView(R.layout.lxc_layout); }
	 * 
	 * @Override public boolean confirmCloseWithTransfersRunning() { //TODO
	 * System.out.println("FIXME: Check for running downloads before exiting");
	 * return true; } }; }
	 * 
	 * @Override public void readConfiguration(String[] args) {
	 * SharedPreferences prefs = getPreferences(MODE_PRIVATE); Map<String, ?>
	 * stored = prefs.getAll(); for (String key : stored.keySet()) { Object
	 * value = stored.get(key); Configuration.putStringSetting(key,
	 * value.toString()); } }
	 * 
	 * @Override public void writeConfiguration() { SharedPreferences.Editor
	 * prefs = getPreferences(MODE_PRIVATE).edit(); prefs.clear();
	 * Iterator<String> keyIter = Configuration.getKeyIterator(); while
	 * (keyIter.hasNext()) { String key = keyIter.next(); prefs.putString(key,
	 * Configuration.getStringSetting(key)); } prefs.commit(); }
	 * 
	 * @Override public boolean askForDownloadTargetSupported() { return false;
	 * }
	 * 
	 * @Override public String getDefaultDownloadTarget() { String state =
	 * Environment.getExternalStorageState();
	 * 
	 * if (Environment.MEDIA_MOUNTED.equals(state)) { // We can read and write
	 * the media return
	 * Environment.getExternalStorageDirectory().getAbsolutePath(); } else { //
	 * Bad. Display error message and exit AlertDialog.Builder builder = new
	 * AlertDialog.Builder(this); builder.setMessage("Cannot access data ");
	 * builder.setCancelable(false); builder.setNeutralButton("OK", new
	 * OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface dialog, int id) {
	 * dialog.cancel(); } }); builder.create(); finish(); return null; // should
	 * not be executed } }
	 */
}
