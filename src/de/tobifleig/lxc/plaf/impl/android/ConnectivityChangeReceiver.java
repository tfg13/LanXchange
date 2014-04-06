package de.tobifleig.lxc.plaf.impl.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    private static ConnectivityChangeListener listener;

    public static void setConnectivityListener(ConnectivityChangeListener listener) {
        ConnectivityChangeReceiver.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent receivedIntent) {
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info != null) {
            boolean isWifi = info.getType() == ConnectivityManager.TYPE_WIFI;
            // Send to Activity
            if (listener != null) {
                listener.setWifiState(isWifi);
            }
        }
    }

}
