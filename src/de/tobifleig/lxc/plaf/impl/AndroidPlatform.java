package de.tobifleig.lxc.plaf.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
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

    private static final int RETURNCODE_FILEINTENT = 12345;
    private static final int RETURNCODE_MEDIAINTENT = 12346;
    private LayoutInflater infl;
    private GuiListener guiListener;
    private List<LXCFile> files;
    private DataSetObserver observer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView emptyText = new TextView(this);
        emptyText.setText(R.string.nofiles);
        emptyText.setGravity(Gravity.CENTER);

        getListView().setEmptyView(emptyText);

        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        root.addView(emptyText);

        infl = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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

                ((TextView) item.findViewById(R.id.filename)).setText(files.get(arg0).getShownName());
                ((TextView) item.findViewById(R.id.filesize)).setText(LXCFile.getFormattedSize(files.get(arg0).getFileSize()));

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
            case R.id.quit:
                AndroidSingleton.onRealDestroy(this);
                finish();
                return true;
            case R.id.addFile:
                // There are several methods to select a file
                // Built-in (always offered) are Music, Video and Images
                // Optional are generic files. This option is available if the
                // user has a file-browser installed:
                // Best way: User has a file-browser installed:
                final Intent fileIntent = new Intent();
                fileIntent.setAction(Intent.ACTION_GET_CONTENT);
                fileIntent.setType("file/*");
                CharSequence[] items = { "Video", "Music", "Image" };
                if (this.getPackageManager().resolveActivity(fileIntent, 0) != null) {
                    // file-browser available:
                    items = new CharSequence[] { "Video", "Music", "Image", "Other files" };
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Pick what to share:");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        Intent pickIntent = new Intent();
                        pickIntent.setAction(Intent.ACTION_PICK);
                        switch (item) {
                            case 0: // Video
                                pickIntent.setData(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(pickIntent, RETURNCODE_MEDIAINTENT);
                                break;
                            case 1: // Audio
                                pickIntent.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(pickIntent, RETURNCODE_MEDIAINTENT);
                                break;
                            case 2: // Images
                                pickIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(pickIntent, RETURNCODE_MEDIAINTENT);
                                break;
                            case 3: // Other files
                                startActivityForResult(fileIntent, RETURNCODE_FILEINTENT);
                                break;
                        }
                        
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            // User pressed "back"/"cancel" etc
            return;
        }
        switch (requestCode) {
            case RETURNCODE_MEDIAINTENT:
                String[] proj = { MediaStore.Images.Media.DATA };
                Cursor cursor = managedQuery(data.getData(), proj, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(column_index);
                File mediaFile = new File(path);
                List<File> list = new ArrayList<File>();
                list.add(mediaFile);
                LXCFile lxcfile = new LXCFile(list, mediaFile.getName());
                guiListener.offerFile(lxcfile);
                break;
            case RETURNCODE_FILEINTENT:
                String filePath = data.getData().toString();
                File file = new File(filePath.substring(filePath.indexOf('/')));
                List<File> fileList = new ArrayList<File>();
                fileList.add(file);
                guiListener.offerFile(new LXCFile(fileList, file.getName()));
                break;
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
