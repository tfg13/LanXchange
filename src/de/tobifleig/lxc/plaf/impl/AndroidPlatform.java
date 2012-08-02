package de.tobifleig.lxc.plaf.impl;

import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.impl.android.AndroidSingleton;
import de.tobifleig.lxc.plaf.impl.android.GuiInterfaceBridge;

/**
 * Platform for Android / Default Activity
 * 
 * no automated updates (managed by Google Play)
 * 
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class AndroidPlatform extends ListActivity {

	private LayoutInflater infl;
	private GuiListener guiListener;
	private List<LXCFile> files;
	private DataSetObserver observer;

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
				System.out.println("Observer deregistered");
				observer = null;
			}

			@Override
			public void registerDataSetObserver(DataSetObserver arg0) {
				System.out.println("Observer registered!");
				observer = arg0;
			}

			@Override
			public boolean isEmpty() {
				return guiListener == null || files.isEmpty();
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

				((TextView) item.findViewById(R.id.filename)).setText(files
						.get(arg0).getShownName());
				((TextView) item.findViewById(R.id.filesize)).setText(LXCFile
						.getFormattedSize(files.get(arg0).getFileSize()));

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
				return guiListener == null ? 0 : files.size();
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

		AndroidSingleton.onCreateMainActivity(this, new GuiInterfaceBridge() {

			@Override
			public void update() {
				updateGui();
			}
		});
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

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final LXCFile file = files.get(position);
		if (!file.isLocal() && !file.isAvailable()) {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					guiListener.downloadFile(file, false);
				}
			});
			t.setName("lxc_helper_initdl_" + file.getShownName());
			t.setDaemon(true);
			t.start();
		}
	}

	/**
	 * Sets the GuiListener. Will be called by AndroidSingleton when LXC is
	 * ready. If this Activity has been recreated and LXC is still running,
	 * AndroidSingleton calls this within onCreateMainActivity
	 * 
	 * @param guiListener
	 *            out future GuiListener
	 */
	public void setGuiListener(GuiListener guiListener) {
		files = guiListener.getFileList();
		this.guiListener = guiListener;
		updateGui();
	}
	
	private void updateGui() {
		getListView().post(new Runnable() {

			@Override
			public void run() {
				if (observer != null) {
					observer.onChanged();
				}
			}
		});
	}
}
