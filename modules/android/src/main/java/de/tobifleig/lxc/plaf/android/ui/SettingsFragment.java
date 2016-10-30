package de.tobifleig.lxc.plaf.android.ui;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import de.tobifleig.lxc.R;

/**
 * Fragment that shows the Settings
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}