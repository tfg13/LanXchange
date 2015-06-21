/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.plaf.android.activity;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.VirtualFile;
import de.tobifleig.lxc.data.impl.InMemoryFile;
import de.tobifleig.lxc.data.impl.RealFile;
import de.tobifleig.lxc.plaf.android.AndroidGuiListener;
import de.tobifleig.lxc.plaf.android.ConnectivityChangeListener;
import de.tobifleig.lxc.plaf.android.ConnectivityChangeReceiver;
import de.tobifleig.lxc.plaf.android.GuiInterfaceBridge;
import de.tobifleig.lxc.plaf.android.NonFileContent;
import de.tobifleig.lxc.plaf.android.service.AndroidSingleton;
import de.tobifleig.lxc.plaf.android.ui.FileListView;

/**
 * Platform for Android / Default Activity
 * 
 * no automated updates (managed by Google Play)
 * 
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class AndroidPlatform extends AppCompatActivity {

    private static final int RETURNCODE_FILEINTENT = 12345;
    private AndroidGuiListener guiListener;
    private GuiInterfaceBridge guiBridge;

    /**
     * The view that displays all shared and available files
     */
    private FileListView fileListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check intent first
        List<VirtualFile> quickShare = null;
        Intent launchIntent = getIntent();
        if (launchIntent.getAction() != null && !launchIntent.getAction().equals(Intent.ACTION_MAIN)) {
            quickShare = computeInputIntent(launchIntent);
            if (quickShare == null) {
                // unable to access file, inform user
                handleShareError(launchIntent);
            }
        }

        // load layout
        setContentView(R.layout.main);
        // layout is loaded, setup main view
        fileListView = (FileListView) findViewById(R.id.fileList);
        // set up the text displayed when there are no files
        TextView emptyText = (TextView) ((LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.empty_list, null);
        fileListView.setEmptyView(emptyText);
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        root.addView(emptyText);


        ConnectivityChangeReceiver.setConnectivityListener(new ConnectivityChangeListener() {

            @Override
            public void setWifiState(boolean isWifi) {
                setWifiWarning(!isWifi);
            }
        });
        // trigger connectivity listener once to get the current status
        new ConnectivityChangeReceiver().onReceive(getBaseContext(), null);

        guiBridge = new GuiInterfaceBridge() {

            @Override
            public void update() {
                fileListView.updateGui();
            }

            @Override
            public boolean confirmCloseWithTransfersRunning() {
                AlertDialog.Builder builder = new AlertDialog.Builder(AndroidPlatform.this);
                builder.setMessage(R.string.dialog_closewithrunning_text);
                builder.setTitle(R.string.dialog_closewithrunning_title);
                builder.setPositiveButton(R.string.dialog_closewithrunning_yes, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        guiListener.shutdown(true, true);
                        AndroidSingleton.onRealDestroy(AndroidPlatform.this);
                        finish();
                    }
                });
                builder.setNegativeButton(R.string.dialog_closewithrunning_no, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                // always return false
                // if the user decides to kill lanxchange anyway, shutdown is called again
                return false;
            }
        };

        // setup floating action button
        findViewById(R.id.fab_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent testIntent = new Intent();
                testIntent.setAction(Intent.ACTION_GET_CONTENT);
                testIntent.addCategory(Intent.CATEGORY_OPENABLE);
                if (android.os.Build.VERSION.SDK_INT >= 18) {
                    testIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                testIntent.setType("*/*");
                startActivityForResult(testIntent, RETURNCODE_FILEINTENT);
            }
        });

        AndroidSingleton.onCreateMainActivity(this, guiBridge, quickShare);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Re-Check that Service is running. In some rare cases, this may not be the case.
        AndroidSingleton.onCreateMainActivity(this, guiBridge, null);
        AndroidSingleton.onMainActivityVisible(0);
    }

    @Override
    public void onStop() {
        super.onStop();
        // notify service about the gui becoming invisible.
        // service will stop itself after a while to preserve resources
        AndroidSingleton.onMainActivityHidden(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.lxc_layout, menu);
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.quit:
            if (guiListener.shutdown(false, true)) {
                AndroidSingleton.onRealDestroy(this);
                finish();
            }
            return true;
        case R.id.help:
            // display help
            Intent showHelp = new Intent();
            showHelp.setClass(getBaseContext(), HelpActivity.class);
            startActivity(showHelp);
            return true;
        case R.id.about:
            // display about
            Intent showAbout = new Intent();
            showAbout.setClass(getBaseContext(), AboutActivity.class);
            startActivity(showAbout);
            return true;
        case R.id.pcversion:
            // display info about pc version
            Intent showPCVersion = new Intent();
            showPCVersion.setClass(getBaseContext(), PCVersionActivity.class);
            startActivity(showPCVersion);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            // User pressed "back"/"cancel" etc
            return;
        }

        ArrayList<VirtualFile> files = new ArrayList<VirtualFile>();
        // multiple files
        if (android.os.Build.VERSION.SDK_INT >= 18 && data.getClipData() != null) {
            List<VirtualFile> virtualFiles = virtualFilesFromClipData(data.getClipData());
            if (virtualFiles != null && !virtualFiles.isEmpty()) {
                files.addAll(virtualFiles);
            } else {
                handleShareError(data);
            }
        } else if (data.getData() != null) {
            VirtualFile virtualFile = uriToVirtualFile(data.getData());
            if (virtualFile != null) {
                files.add(virtualFile);
            } else {
                handleShareError(data);
            }
        }

        if (!files.isEmpty()) {
            offerFiles(files);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // only act if there is an action and it is not MAIN
        if (intent.getAction() != null && !intent.getAction().equals(Intent.ACTION_MAIN)) {
            List<VirtualFile> files = computeInputIntent(intent);
            if (files != null && !files.isEmpty()) {
                offerFiles(files);
            } else {
                handleShareError(intent);
            }
        }
    }

    /**
     * Called when importing content to share failed.
     * Logs the Intent for debug purposes and displays a Toast.
     *
     */
    private void handleShareError(Intent failedIntent) {
        //noinspection ResourceType
        Snackbar.make(findViewById(R.id.main_layout), R.string.error_cantoffer, Snackbar.LENGTH_LONG)
                .setDuration(5000).show();
        System.err.println("Sharing failed. Intent details:");
        System.err.println("Intent object: " + failedIntent);
        System.err.println("Intent action: " + failedIntent.getAction());
        System.err.println("Intent dataString: " + failedIntent.getDataString());
        System.err.println("Intent data: " + failedIntent.getData());
        System.err.println("Intent type: " + failedIntent.getType());
        System.err.println("Intent scheme: " + failedIntent.getScheme());
        System.err.println("Intent package: " + failedIntent.getPackage());
        System.err.println("Intent extras: " + failedIntent.getExtras());
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            System.err.println("Intent clipData: " + failedIntent.getClipData());
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private List<VirtualFile> virtualFilesFromClipData(ClipData clipdata) {
        ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();
        for (int i = 0; i < clipdata.getItemCount(); i++) {
            ClipData.Item item = clipdata.getItemAt(i);
            // may contain Uri or String
            if (item.getUri() != null) {
                VirtualFile file = uriToVirtualFile(item.getUri());
                if (file != null) {
                    result.add(file);
                }
            } else if (item.getText() != null) {
                // plain text
                try {
                    ByteArrayOutputStream arrayOutput = new ByteArrayOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(arrayOutput);
                    writer.write(item.getText().toString());
                    writer.close();
                    result.add(new InMemoryFile("test.txt", arrayOutput.toByteArray()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private List<VirtualFile> computeInputIntent(Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SEND)) {
            // use ClipData if available (newer api)
            ClipData clip = intent.getClipData();
            if (clip != null) {
                return virtualFilesFromClipData(clip);
            } else {
                // no clip, try extra stream
                Object data = intent.getExtras().get(Intent.EXTRA_STREAM);
                if (data != null && (data.toString().startsWith("file://") || data.toString().startsWith("content:"))) {
                    // Make file available asap:
                    ArrayList<Uri> uris = new ArrayList<Uri>();
                    uris.add(Uri.parse(intent.getExtras().get(Intent.EXTRA_STREAM).toString()));
                    return urisToVirtualFiles(uris);
                }
            }
        } else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            // there is a legacy and a new way to receive multiple files
            // try the new first
            if (android.os.Build.VERSION.SDK_INT >= 16 && intent.getClipData() != null) {
                return virtualFilesFromClipData(intent.getClipData());
            } else if (intent.getStringArrayListExtra(Intent.EXTRA_STREAM) != null) {
                ArrayList<Uri> uris = new ArrayList<Uri>();
                @SuppressWarnings("rawtypes")
                ArrayList uriStrings = intent.getStringArrayListExtra(Intent.EXTRA_STREAM);
                for (Object uriString : uriStrings) {
                    uris.add(Uri.parse(uriString.toString()));
                }
                return urisToVirtualFiles(uris);
            }
        }
        return null;
    }

    private void setWifiWarning(boolean displayWarning) {
        findViewById(R.id.noWifiWarning).setVisibility(displayWarning ? View.VISIBLE : View.GONE);
    }

    /**
     * Offers files.
     * 
     * @param files the files to offer
     */
    private void offerFiles(List<VirtualFile> files) {
        if (files.isEmpty()) {
            System.err.println("invalid input!");
            return;
        }

        LXCFile lxcfile = new LXCFile(files, files.get(0).getName());
        guiListener.offerFile(lxcfile);
    }

    private List<VirtualFile> urisToVirtualFiles(List<Uri> uris) {
        List<VirtualFile> list = new ArrayList<VirtualFile>();
        for (Uri uri : uris) {
            VirtualFile virtualFile = uriToVirtualFile(uri);
            if (virtualFile != null) {
                list.add(virtualFile);
            }
        }
        return list;
    }

    private VirtualFile uriToVirtualFile(Uri uri) {
        String uriString = uri.toString();
        VirtualFile file = null;
        // Handle kitkat files
        if (uriString.startsWith("content://")) {
            ContentResolver resolver = getBaseContext().getContentResolver();
            // get file name
            String[] projection = { MediaStore.Files.FileColumns.DISPLAY_NAME };
            Cursor cursor = resolver.query(uri, projection, null, null, null);
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                String name = cursor.getString(column_index);
                try {
                    ParcelFileDescriptor desc = resolver.openFileDescriptor(uri, "r");
                    file = new NonFileContent(name, desc, uri, resolver);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                cursor.close();
            }
        } else if (uriString.startsWith("file://")) {
            // seems to be useable right away
            file = new RealFile(new File(uri.getPath()));
        }

        // one last trick
        if (file == null) {
            File resolvedFile = new File(uri.getPath());
            if (resolvedFile.exists()) {
                file = new RealFile(resolvedFile);
            }
        }
        return file;
    }

    /**
     * Sets the GuiListener. Will be called by AndroidSingleton when LXC is
     * ready. If this Activity has been recreated and LXC is still running,
     * AndroidSingleton calls this within onCreateMainActivity
     * 
     * @param guiListener
     *            out future GuiListener
     */
    public void setGuiListener(AndroidGuiListener guiListener) {
        fileListView.setGuiListener(guiListener);
        this.guiListener = guiListener;
        fileListView.updateGui();
    }

    /**
     * When this activity is started with an ACTION_SEND Intent, the path of the
     * file to share will end up here.
     * 
     * @param uris a list of Uris to share
     */
    public void quickShare(List<VirtualFile> uris) {
        offerFiles(uris);
    }
}
