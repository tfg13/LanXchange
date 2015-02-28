/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.plaf.android.ui;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.DataSetObserver;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.LXCJob;
import de.tobifleig.lxc.data.impl.RealFile;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.ProgressIndicator;
import de.tobifleig.lxc.plaf.android.FileListWrapper;


/**
 * The main GUI element, the list that displays all available & shared files.
 *
 */
public class FileListView extends ListView {

    private LayoutInflater inflator;
    private DataSetObserver observer;
    private FileListWrapper files;
    private GuiListener guiListener;

    private final ProgressIndicator noopIndicator = new ProgressIndicator() {

        @Override
        public void update(int percentage) {
            // do nothing
        }
    };

    /*
     * Constructors of super class.
     */
    public FileListView(Context context) {
        super(context);
        setup();
    }

    public FileListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    public FileListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    /**
     * Sets up this view.
     */
    private void setup() {
        // get layout inflator
        inflator = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // setup listener
        setAdapter(new ListAdapter() {

            @Override
            public void unregisterDataSetObserver(DataSetObserver arg0) {
                observer = null;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver arg0) {
                observer = arg0;
            }

            @Override
            public boolean isEmpty() {
                return guiListener == null || (files.getLocalList().isEmpty() && files.getRemoteList().isEmpty());
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public View getView(int n, View view, ViewGroup group) {
                // insert header
                if (n == 0) {
                    TextView yourfiles = (TextView) inflator.inflate(R.layout.listheader, group, false);
                    yourfiles.setText(R.string.ui_yourfiles);
                    return yourfiles;
                }
                // element own files?
                if (n <= files.getLocalList().size()) {
                    return createLocalListItem(files.getLocalList().get(n - 1), group);
                }
                // second header
                if (n == files.getLocalList().size() + 1) {
                    TextView sharedwithyou = (TextView) inflator.inflate(R.layout.listheader, group, false);
                    sharedwithyou.setText(R.string.ui_sharedwithyou);
                    return sharedwithyou;
                }
                // network files
                return createRemoteListItem(files.getRemoteList().get(n - (2 + files.getLocalList().size())), group);
            }

            @Override
            public int getItemViewType(int arg0) {
                return 0;
            }

            @Override
            public long getItemId(int arg0) {
                return arg0;
            }

            @Override
            public Object getItem(int arg0) {
                return null;
            }

            @Override
            public int getCount() {
                return guiListener == null ? 0 : (files.getLocalList().size() + files.getRemoteList().size()) + 2;
            }

            @Override
            public boolean isEnabled(int position) {
                // cannot click on category headers
                if (position == 0 || position == files.getLocalList().size() + 1) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }
        });

        setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onListItemClick(position);
            }
        });

        setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return onLongListItemClick(position);
            }
        });
    }

    /**
     * Injects the gui listener.
     * @param guiListener used send events to the LXC core
     */
    public void setGuiListener(GuiListener guiListener) {
        this.files = new FileListWrapper(guiListener.getFileList());
        this.guiListener = guiListener;
    }

    /**
     * Handler for taps on list items.
     * 
     * @param position index of tapped element
     */
    public void onListItemClick(int position) {
        // only clicks to second list for now
        if (position >= files.getLocalList().size() + 2) {
            final LXCFile file = files.getRemoteList().get(position - files.getLocalList().size() - 2);
            if (!file.isLocked() && !file.isLocal() && !file.isAvailable()) {
                file.setLocked(true);
                updateGui();
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        guiListener.downloadFile(file, false);
                    }
                });
                t.setName("lxc_helper_initdl_" + file.getShownName());
                t.setDaemon(true);
                t.start();
            } else if (!file.isLocal() && file.isAvailable()) {
                // open file
                Intent openIntent = new Intent();
                openIntent.setAction(Intent.ACTION_VIEW);
                // Hack: Local files are RealFiles
                RealFile realFile = (RealFile) file.getFiles().get(0);
                Uri fileUri = Uri.fromFile(realFile.getBackingFile());
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(realFile.getBackingFile().getAbsolutePath()));
                openIntent.setDataAndType(fileUri, mimeType);
                // check if intent can be processed
                List<ResolveInfo> list = getContext().getPackageManager().queryIntentActivities(openIntent, 0);
                if (list.isEmpty()) {
                    // cannot be opened
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.error_cantopen_title);
                    builder.setMessage(R.string.error_cantopen_text);
                    builder.setPositiveButton(R.string.error_cantopen_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    });
                    builder.show();
                } else {
                    getContext().startActivity(openIntent);
                }
            }
        }
    }

    private boolean onLongListItemClick(int position) {
        // clicks for first list
        if (position != 0 && position <= files.getLocalList().size()) {
            final LXCFile file = files.getLocalList().get(position - 1);
            guiListener.removeFile(file);
            updateGui();
            return true;
        } else if (position >= files.getLocalList().size() + 2) {
            // clicks for second list, only valid for downloaded files
            final LXCFile file = files.getRemoteList().get(position - files.getLocalList().size() - 2);
            if (file.isAvailable()) {
                guiListener.resetFile(file);
                updateGui();
                return true;
            }
        }
        return false;
    }

    private View createLocalListItem(LXCFile file, ViewGroup group) {
        View item = inflator.inflate(R.layout.file_item, group, false);
        ((TextView) item.findViewById(R.id.filename)).setText(file.getShownName());
        ((TextView) item.findViewById(R.id.filesize)).setText(LXCFile.getFormattedSize(file.getFileSize()));
        // set image
        if (file.getType() == LXCFile.TYPE_FILE) {
            ((ImageView) item.findViewById(R.id.typeView)).setImageDrawable(getResources().getDrawable(R.drawable.singlefile));
        } else if (file.getType() == LXCFile.TYPE_FOLDER) {
            ((ImageView) item.findViewById(R.id.typeView)).setImageResource(R.drawable.folder);
        } else { // multi
            ((ImageView) item.findViewById(R.id.typeView)).setImageResource(R.drawable.multifile);
        }
        // Status text is different for own files
        TextView statusText = (TextView) item.findViewById(R.id.instruction);
        statusText.setText(R.string.ui_holdtoremove);
        // Override all default ProgressIndicators
        for (LXCJob job : file.getJobs()) {
            job.getTrans().setProgressIndicator(noopIndicator);
        }
        return item;
    }

    private View createRemoteListItem(LXCFile file, ViewGroup group) {
        View item = inflator.inflate(R.layout.file_item, group, false);
        ((TextView) item.findViewById(R.id.filename)).setText(file.getShownName());
        ((TextView) item.findViewById(R.id.filesize)).setText(LXCFile.getFormattedSize(file.getFileSize()));
        // set image
        if (!file.isAvailable() && file.getType() == LXCFile.TYPE_FILE) {
            ((ImageView) item.findViewById(R.id.typeView)).setImageDrawable(getResources().getDrawable(R.drawable.singlefile));
        } else if (!file.isAvailable() && file.getType() == LXCFile.TYPE_FOLDER) {
            ((ImageView) item.findViewById(R.id.typeView)).setImageResource(R.drawable.folder);
        } else if (!file.isAvailable()) { // multi
            ((ImageView) item.findViewById(R.id.typeView)).setImageResource(R.drawable.multifile);
        } else {
            ((ImageView) item.findViewById(R.id.typeView)).setImageDrawable(getResources().getDrawable(R.drawable.done));
        }
        // Show status
        final ProgressBar progressBar = (ProgressBar) item.findViewById(R.id.progressBar1);
        final TextView statusText = (TextView) item.findViewById(R.id.instruction);
        // download starting?
        if (file.isLocked() && file.getJobs().size() == 0) {
            progressBar.setVisibility(View.VISIBLE);
            statusText.setText(R.string.ui_connecting);
        } else if (!file.isAvailable() && file.getJobs().size() == 1) {
            // downloading
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(false);
            int progress = (file.getJobs().get(0).getTrans().getProgress());
            progressBar.setProgress(progress);
            statusText.setText(getResources().getString(R.string.ui_downloading) + " " + progress + "%");
            // override default ProgressIndicator
            file.getJobs().get(0).getTrans().setProgressIndicator(new FilterProgressIndicator(progress) {
                @Override
                protected void updateGui() {
                    post(new Runnable() {
                        @Override
                        public void run() {
                            observer.onChanged();
                        }
                    });
                }
            });
        } else if (file.isAvailable()) {
            // done
            statusText.setText(R.string.ui_available);
        }
        return item;
    }

    public void updateGui() {
        post(new Runnable() {

            @Override
            public void run() {
                if (observer != null) {
                    files.listChanged();
                    observer.onChanged();
                }
            }
        });
    }

}
