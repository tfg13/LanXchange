package de.tobifleig.lxc.plaf.impl.android.activity;

import de.tobifleig.lxc.plaf.impl.android.service.AndroidSingleton;
import android.app.Activity;

public class KeepServiceRunningActivity extends Activity {

    @Override
    public void onPause() {
        super.onPause();
        AndroidSingleton.onMainActivityHidden(1);
    }

    @Override
    public void onResume() {
        super.onResume();
        AndroidSingleton.onMainActivityVisible(1);
    }

}
