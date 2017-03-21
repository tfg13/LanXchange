/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.plaf.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import de.tobifleig.lxc.plaf.android.activity.SettingsActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

/**
 * Custom preference to select the download target directory.
 * Currently, this only supports the internal storage (no external sd cards).
 */
public class DownloadPathPreference extends Preference {

    private DirectoryChooserFragment dialog;

    private String downloadPath;

    public DownloadPathPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void prompt() {
        // super ugly cast
        SettingsActivity myActivity = (SettingsActivity) getContext();
        if (myActivity.verifyStoragePermission(this)) {
            // create dialog/fragment here
            final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                    .allowReadOnlyDirectory(false)
                    .newDirectoryName("LanXchange")
                    .allowNewDirectoryNameModification(true)
                    .initialDirectory(downloadPath)
                    .build();
            dialog = DirectoryChooserFragment.newInstance(config);
            dialog.show(myActivity.getFragmentManager(), null);
            dialog.setDirectoryChooserListener(new DirectoryChooserFragment.OnFragmentInteractionListener() {

                @Override
                public void onSelectDirectory(@NonNull String path) {
                    dialog.dismiss();
                    downloadPath = path;
                    persistString(downloadPath);
                    setSummary(downloadPath);
                }

                @Override
                public void onCancelChooser() {
                    dialog.dismiss();
                }
            });
        }
    }

    @Override
    protected void onClick() {
        prompt();
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        downloadPath = this.getPersistedString(getDefaultPath());
        if ("NONE".equals(downloadPath)) {
            downloadPath = getDefaultPath();
        }
        persistString(downloadPath);
        setSummary(downloadPath);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return getDefaultPath();
    }

    private String getDefaultPath() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent,
            // use superclass state
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current
        // setting value
        myState.value = downloadPath;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Set this Preference's widget to reflect the restored state
        setSummary(myState.value);
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        String value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeString(value);
        }

        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
