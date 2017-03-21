package de.tobifleig.lxc.plaf.android;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import de.tobifleig.lxc.R;
import de.tobifleig.lxc.plaf.android.activity.CancelablePermissionPromptActivity;
import de.tobifleig.lxc.plaf.android.activity.MainActivity;

/**
 * Misc helpers for runtime permissions.
 */
public final class PermissionTools {

    private PermissionTools(){}

    public static boolean verifyStoragePermission(Activity activity, CancelablePermissionPromptActivity cancel) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // user denied this before, explain it
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setMessage(R.string.permissions_explain_storage_text);
                builder.setPositiveButton(R.string.permissions_explain_storage_permdeny, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cancel.cancelPermissionPromptAction();
                    }
                });
                builder.setNegativeButton(R.string.permissions_explain_storage_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // re-request the permission
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MainActivity.RETURNCODE_PERMISSION_PROMPT_STORAGE);
                    }
                });
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
            } else {
                // request it
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MainActivity.RETURNCODE_PERMISSION_PROMPT_STORAGE);
                return false;
            }
            // permission not available now, but may be in the future
            return false;
        }
    }
}
