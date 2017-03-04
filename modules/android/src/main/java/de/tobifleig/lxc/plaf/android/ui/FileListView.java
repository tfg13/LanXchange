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
package de.tobifleig.lxc.plaf.android.ui;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.LXCJob;
import de.tobifleig.lxc.data.impl.RealFile;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.android.FileListWrapper;
import de.tobifleig.lxc.plaf.android.MIMETypeGuesser;


/**
 * The main GUI element, the list that displays all available & shared files.
 *
 */
public class FileListView extends RecyclerView {

    private LayoutInflater inflater;
    private FileListWrapper files;
    private GuiListener guiListener;
    private View emptyView;

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

    private class LXCFileViewHolder extends RecyclerView.ViewHolder {

        private static final int TYPE_HEADER = 1;
        private static final int TYPE_LOCAL = 2;
        private static final int TYPE_REMOTE = 3;

        private final int type;
        private final ViewGroup parent;

        private View cachedView;
        private TextView cachedFileName;
        private TextView cachedFileInfo;
        // local files only
        private View cachedRemoveLocal;
        private ArrayList<View> cachedTransferProgress;
        private View cachedSizer;
        // remote files only
        private ImageView cachedDownloadStatus;
        private ProgressBar cachedProgressBar;
        private ImageView cachedDownloadCancel;

        /**
         * The file that is currently held/displayed by this ViewHolder.
         * Only for TYPE_LOCAL and TYPE_REMOTE!
         */
        private LXCFile currentFile;

        /**
         * Creates a LXCFileViewHolder with the given type
         */
        private LXCFileViewHolder(ViewGroup parent, View view, int type) {
            super(view);
            this.type = type;
            this.parent = parent;

            this.cachedView = view;
            // cache subviews for file types
            if (type == TYPE_LOCAL || type == TYPE_REMOTE) {
                cachedFileName = (TextView) cachedView.findViewById(R.id.filename);
                cachedFileInfo = (TextView) cachedView.findViewById(R.id.fileInfo);
            }
            if (type == TYPE_LOCAL) {
                cachedRemoveLocal = cachedView.findViewById(R.id.removeLocal);
                cachedSizer = cachedView.findViewById(R.id.sizer);
                cachedTransferProgress = new ArrayList<>();
            }
            if (type == TYPE_REMOTE) {
                cachedDownloadStatus = (ImageView) cachedView.findViewById(R.id.downloadStatus);
                cachedProgressBar = (ProgressBar) cachedView.findViewById(R.id.progressBar1);
                cachedDownloadCancel = (ImageView) cachedView.findViewById(R.id.cancelTransfer);
            }

            // setup interaction
            if (type == TYPE_REMOTE) {
                cachedView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!currentFile.isLocked() && !currentFile.isLocal() && !currentFile.isAvailable()) {
                            currentFile.setLocked(true);
                            updateGui();
                            Thread t = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    guiListener.downloadFile(currentFile, false);
                                }
                            });
                            t.setName("lxc_helper_initdl_" + currentFile.getShownName());
                            t.setDaemon(true);
                            t.start();
                        } else if (!currentFile.isLocal() && currentFile.isAvailable()) {
                            // open currentFile
                            final Intent openIntent = new Intent();
                            openIntent.setAction(Intent.ACTION_VIEW);
                            // Hack: Local files are RealFiles
                            final RealFile realFile = (RealFile) currentFile.getFiles().get(0);
                            Uri fileUri = Uri.fromFile(realFile.getBackingFile());
                            openIntent.setDataAndType(fileUri, MIMETypeGuesser.guessMIMEType(realFile, getContext()));
                            // check if intent can be processed
                            List<ResolveInfo> list = getContext().getPackageManager().queryIntentActivities(openIntent, 0);

                            if (!list.isEmpty() && openIntent.getType().equals(MIMETypeGuesser.GENERIC_RESULT) && !realFile.isDirectory()) {
                                // mimetype could not be determined
                                // inform user, offer to choose an app anyway, cancel or complain about this
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setTitle(R.string.error_cantopen_title_unknowntype);
                                builder.setMessage(R.string.error_cantopen_text_unknowntype);
                                builder.setPositiveButton(R.string.error_cantopen_choose, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // still choose app to open this
                                        getContext().startActivity(openIntent);
                                    }
                                });
                                builder.setNeutralButton(R.string.error_cantopen_complain, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String uriText = "mailto:" + Uri.encode("mail@lanxchange.com") +
                                                "?subject=" + Uri.encode("Cannot determine filetype") +
                                                "&body=" + Uri.encode("This is an automatic mail to the developer of LanXchange.\n" +
                                                "Feel free to write a bit more and explain what you were doing, or just press send.\n" +
                                                "\n" +
                                                "\n" +
                                                "---------------------------------------\n" +
                                                "technical info (do not remove this!):\n" +
                                                "---------------------------------------\n" +
                                                "unknown mimetype: (" + MIMETypeGuesser.GENERIC_RESULT + ")\n" +
                                                "path: " + realFile.getBackingFile().getAbsolutePath() + "\n" +
                                                "name:" + realFile.getName());
                                        getContext().startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(uriText)));
                                    }
                                });
                                builder.setNegativeButton(R.string.error_cantopen_cancel, new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                });
                                builder.show();
                            } else if (list.isEmpty()) {
                                // mimetype was determined, but no suitable app was found
                                // inform user, offer to complain about this
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                builder.setTitle(R.string.error_cantopen_title_listempty);
                                builder.setMessage(R.string.error_cantopen_text_listempty);
                                builder.setPositiveButton(R.string.error_cantopen_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // do nothing
                                    }
                                });
                                builder.setNeutralButton(R.string.error_cantopen_mail, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String uriText = "mailto:" + Uri.encode("mail@lanxchange.com") +
                                                "?subject=" + Uri.encode("No suitable app found to open file") +
                                                "&body=" + Uri.encode("This is an automatic mail to the developer of LanXchange.\n" +
                                                "Feel free to write a bit more and explain what you were doing, or just press send.\n" +
                                                "\n" +
                                                "\n" +
                                                "---------------------------------------\n" +
                                                "technical info (do not remove this!):\n" +
                                                "---------------------------------------\n" +
                                                "no app for: (" + MIMETypeGuesser.GENERIC_RESULT + ")\n" +
                                                "path: " + realFile.getBackingFile().getAbsolutePath() + "\n" +
                                                "name:" + realFile.getName());
                                        getContext().startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(uriText)));
                                    }
                                });
                                builder.show();
                            } else {
                                // mime type was either known or this is a directory
                                getContext().startActivity(openIntent);
                            }
                        }
                    }
                });
                // long clicks (yes, I know one should no longer use this, but this is hidden "expert" functionality, anyway
                cachedView.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (currentFile.isAvailable()) {
                            guiListener.resetFile(currentFile);
                            updateGui();
                            return true;
                        }
                        return false;
                    }
                });
            }
        }

        private void setLocalHeader() {
            if (type != TYPE_HEADER) {
                throw new IllegalStateException("Wrong type of LXCFileViewHolder (called setLocalHeader, but is of type " + type + ")");
            }
            ((TextView) cachedView).setText(R.string.ui_yourfiles);
        }

        private void setRemoteHeader() {
            if (type != TYPE_HEADER) {
                throw new IllegalStateException("Wrong type of LXCFileViewHolder (called setRemoteHeader, but is of type " + type + ")");
            }
            ((TextView) cachedView).setText(R.string.ui_sharedwithyou);
        }

        private void setFile(final LXCFile file) {
            if (file.isLocal() && type != TYPE_LOCAL) {
                throw new IllegalStateException("Wrong type of LXCFileViewHolder (called setFile with a local file, but is of type " + type + ")");
            }
            if (!file.isLocal() && type != TYPE_REMOTE) {
                throw new IllegalStateException("Wrong type of LXCFileViewHolder (called setFile with a remote file, but is of type " + type + ")");
            }
            currentFile = file;
            // set values common for both file types
            cachedFileName.setText(file.getShownName());
            cachedFileInfo.setText(LXCFile.getFormattedSize(file.getFileSize()));
            // values for local files
            if (type == TYPE_LOCAL) {
                // handle spacing with progressbars
                if (file.getJobs().isEmpty()) {
                    ((LinearLayout.LayoutParams) cachedSizer.getLayoutParams()).height = (int) getResources().getDimension(R.dimen.file_sizer_standalone);
                } else {
                    ((LinearLayout.LayoutParams) cachedSizer.getLayoutParams()).height = (int) getResources().getDimension(R.dimen.file_sizer_withtransfers);
                }
                // add progressbars
                for (int i = 0; i < file.getJobs().size(); i++) {
                    LXCJob job = file.getJobs().get(i);
                    View transferProgress;
                    if (i >= cachedTransferProgress.size()) {
                        // create new progressbar and add it
                        transferProgress = inflater.inflate(R.layout.transfer_progress, parent, false);
                        cachedTransferProgress.add(transferProgress);
                        ((LinearLayout) cachedView.findViewById(R.id.container)).addView(transferProgress);
                    } else {
                        transferProgress = cachedTransferProgress.get(i);
                    }
                    final ProgressBar transferProgressBar = ((ProgressBar) transferProgress.findViewById(R.id.progressbar));
                    final TextView transferProgressLabel = ((TextView) transferProgress.findViewById(R.id.progresslabel));
                    file.getJobs().get(i).getTrans().setProgressIndicator(new FilterProgressIndicator(0) {


                        @Override
                        protected void updateGui() {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    int progress = job.getTrans().getProgress();
                                    transferProgressBar.setProgress(progress);
                                    transferProgressLabel.setText(getResources().getString(R.string.ui_uploading) + " " + progress + "%" + " - " + LXCFile.getFormattedSize(job.getTrans().getCurrentSpeed()) + "/s");
                                }
                            });
                        }
                    });
                    cachedView.findViewById(R.id.cancelTransfer).setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            job.abortTransfer();
                        }
                    });

                }
                // remove old progressbars
                for (int i = cachedTransferProgress.size() - 1; i >= file.getJobs().size(); i--) {
                    ((LinearLayout) cachedView.findViewById(R.id.container)).removeView(cachedTransferProgress.get(i));
                    cachedTransferProgress.remove(i);
                }
                cachedRemoveLocal.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int removedIndex = files.getLocalList().indexOf(file);
                        guiListener.removeFile(file);
                        notifyLocalFileRemoved(removedIndex);
                        // offer to undo the removal
                        Snackbar.make(parent, getResources().getString(R.string.ui_undo_removelocalfile)
                                + "\n\"" + file.getShownName() + "\"", Snackbar.LENGTH_LONG)
                                .setAction(R.string.snackbar_undoremovelocal_action, new OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // undo
                                        guiListener.offerFile(file);
                                    }
                                }).show();
                    }
                });
            }
            // values for remote files
            if (type == TYPE_REMOTE) {
                // set image
                if (!file.isAvailable()) {
                    cachedDownloadStatus.setImageResource(R.drawable.ic_file_download);
                } else {
                    cachedDownloadStatus.setImageResource(R.drawable.ic_done_black_48dp);
                }
                // download starting?
                if (file.isLocked() && file.getJobs().size() == 0) {
                    cachedProgressBar.setVisibility(View.VISIBLE);
                    cachedProgressBar.setIndeterminate(true);
                    cachedDownloadCancel.setVisibility(View.GONE);
                    cachedFileInfo.setText(R.string.ui_connecting);
                } else if (!file.isAvailable() && file.getJobs().size() == 1) {
                    // downloading
                    cachedProgressBar.setVisibility(View.VISIBLE);
                    cachedProgressBar.setIndeterminate(false);
                    cachedDownloadCancel.setVisibility(View.VISIBLE);
                    final LXCJob job = file.getJobs().get(0);
                    int progress = job.getTrans().getProgress();
                    cachedProgressBar.setProgress(progress);
                    cachedFileInfo.setText(getResources().getString(R.string.ui_downloading) + " " + progress + "%");
                    // override default ProgressIndicator
                    job.getTrans().setProgressIndicator(new FilterProgressIndicator(progress) {
                        @Override
                        protected void updateGui() {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    int progress = job.getTrans().getProgress();
                                    cachedProgressBar.setProgress(progress);
                                    cachedFileInfo.setText(getResources().getString(R.string.ui_downloading) + " " + progress + "%" + " - " + LXCFile.getFormattedSize(job.getTrans().getCurrentSpeed()) + "/s");
                                }
                            });
                        }
                    });
                    // config cancel button
                    cachedDownloadCancel.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            job.abortTransfer();
                        }
                    });
                } else if (file.isAvailable()) {
                    // done
                    cachedFileInfo.setText(R.string.ui_available);
                    cachedProgressBar.setVisibility(View.INVISIBLE);
                    cachedDownloadCancel.setVisibility(View.GONE);
                } else {
                    // file status normal
                    cachedProgressBar.setVisibility(View.INVISIBLE);
                    cachedDownloadCancel.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * Sets up this view.
     */
    private void setup() {
        // get layout inflater
        inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setLayoutManager(new LinearLayoutManager(getContext()));

        // setup listener
        setAdapter(new RecyclerView.Adapter<LXCFileViewHolder>() {

            @Override
            public LXCFileViewHolder onCreateViewHolder(ViewGroup parent, int type) {
                View createdView;
                switch (type) {
                    case LXCFileViewHolder.TYPE_HEADER:
                        createdView = inflater.inflate(R.layout.listheader, parent, false);
                        break;
                    case LXCFileViewHolder.TYPE_LOCAL:
                        createdView = inflater.inflate(R.layout.file_list_item_local, parent, false);
                        break;
                    case LXCFileViewHolder.TYPE_REMOTE:
                        createdView = inflater.inflate(R.layout.file_list_item_remote, parent, false);
                        break;
                    default:
                        throw new IllegalArgumentException("LXCFileViewHolder type out of range! (was " + type + ")");
                }
                return new LXCFileViewHolder(parent, createdView, type);
            }

            @Override
            public void onBindViewHolder(LXCFileViewHolder viewHolder, int position) {
                List<LXCFile> localFiles = files.getLocalList();
                List<LXCFile> remoteFiles = files.getRemoteList();
                // first element is always a header
                if (position == 0) {
                    if (!localFiles.isEmpty()) {
                        viewHolder.setLocalHeader();
                    } else {
                        viewHolder.setRemoteHeader();
                    }
                } else if (position <= localFiles.size()) {
                    // own files
                    viewHolder.setFile(localFiles.get(position - 1));
                } else if (position == localFiles.size() + (localFiles.isEmpty() ? 0 : 1)) {
                    // second header
                    viewHolder.setRemoteHeader();
                } else {
                    // network files
                    viewHolder.setFile(remoteFiles.get(position - ((localFiles.isEmpty() ? 1 : 2) + localFiles.size())));
                }
            }

            @Override
            public int getItemCount() {
                if (guiListener == null) {
                    return 0;
                }
                int numberOfLocalFiles = files.getLocalList().size();
                int numberOfRemoteFiles = files.getRemoteList().size();

                // increment to account for category headers
                if (numberOfLocalFiles > 0) {
                    numberOfLocalFiles++;
                }
                if (numberOfRemoteFiles > 0) {
                    numberOfRemoteFiles++;
                }

                return numberOfLocalFiles + numberOfRemoteFiles;
            }

            @Override
            public int getItemViewType(int position) {
                List<LXCFile> localFiles = files.getLocalList();
                // first element is always a header
                if (position == 0) {
                    return LXCFileViewHolder.TYPE_HEADER;
                }
                // own files
                if (position <= localFiles.size()) {
                    return LXCFileViewHolder.TYPE_LOCAL;
                }
                // second header
                if (position == localFiles.size() + (localFiles.isEmpty() ? 0 : 1)) {
                    return LXCFileViewHolder.TYPE_HEADER;
                }
                // network files
                return LXCFileViewHolder.TYPE_REMOTE;
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

    public void updateGui() {
        post(new Runnable() {

            @Override
            public void run() {
                // do *not* call listChanged here!!!
                // this allows the RecyclerView sometimes to see changed files before the actual add/remove
                // event arrives. When it later does arrive, RecyclerView crashes.
                //files.listChanged();
                manageEmptyView();
                getAdapter().notifyDataSetChanged();
            }
        });
    }

    public void notifyLocalFileAdded() {
        post(new Runnable() {
            @Override
            public void run() {
                files.listChanged();
                manageEmptyView();
                // calc change set for animation
                if (files.getLocalList().size() == 1) {
                    // insert header + file
                    getAdapter().notifyItemRangeInserted(0, 2);
                } else {
                    // insert file only
                    getAdapter().notifyItemInserted(files.getLocalList().size());
                }
            }
        });
    }

    public void notifyLocalFileRemoved(final int localListIndex) {
        post(new Runnable() {
            @Override
            public void run() {
                files.listChanged();
                manageEmptyView();
                // calc change set for animation
                if (files.getLocalList().isEmpty()) {
                    // remove header + file
                    getAdapter().notifyItemRangeRemoved(0, 2);
                } else {
                    // remove file only
                    getAdapter().notifyItemRemoved(localListIndex + 1);
                }
            }
        });
    }

    public void notifyRemoteFilesAdded(final int numberOfFiles) {
        post(new Runnable() {
            @Override
            public void run() {
                files.listChanged();
                manageEmptyView();
                List<LXCFile> localList = files.getLocalList();
                List<LXCFile> remoteList = files.getRemoteList();
                if (remoteList.size() == numberOfFiles) {
                    // insert header + files
                    getAdapter().notifyItemRangeInserted(localList.isEmpty() ? 0 : localList.size() + 1, numberOfFiles + 1);
                } else {
                    // insert files only
                    getAdapter().notifyItemRangeInserted((localList.isEmpty() ? 0 : localList.size() + 1) + remoteList.size() + 1, numberOfFiles);
                }
            }
        });

    }

    public void notifyRemoteFilesRemoved(final int firstIndex, final int numberOfFiles) {
        post(new Runnable() {
            @Override
            public void run() {
                files.listChanged();
                manageEmptyView();
                List<LXCFile> localList = files.getLocalList();
                List<LXCFile> remoteList = files.getRemoteList();
                if (remoteList.isEmpty()) {
                    // remove header + files
                    getAdapter().notifyItemRangeRemoved(localList.isEmpty() ? 0 : localList.size() + 1, numberOfFiles + 1);
                } else {
                    // remove files only
                    getAdapter().notifyItemRangeRemoved((localList.isEmpty() ? 0 : localList.size() + 1) + firstIndex + 1, numberOfFiles);
                }
            }
        });
    }

    public void notifyLocalJobAdded(LXCFile file, int jobIndex) {
        post(new Runnable() {
            @Override
            public void run() {
                getAdapter().notifyItemChanged(files.getIndexForFile(file), null);
            }
        });
    }

    public void notifyLocalJobRemoved(LXCFile file, int jobIndex) {
        post(new Runnable() {
            @Override
            public void run() {
                getAdapter().notifyItemChanged(files.getIndexForFile(file), null);
            }
        });
    }

    /**
     * Set what to display when the list is empty.
     * ListView was able to do this, but RecyclerView is lacking support for this,
     * so this is my own implementation.
     * @param emptyView the view to show while the list is empty
     */
    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
    }

    /**
     * Called whenever the list changes.
     */
    private void manageEmptyView() {
        if (files.getLocalList().isEmpty() && files.getRemoteList().isEmpty()) {
            if (emptyView.getVisibility() != View.VISIBLE) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        emptyView.setVisibility(View.VISIBLE);
                    }
                });
            }
        } else {
            if (emptyView.getVisibility() != View.GONE) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        emptyView.setVisibility(View.GONE);
                    }
                });
            }
        }
    }
}
