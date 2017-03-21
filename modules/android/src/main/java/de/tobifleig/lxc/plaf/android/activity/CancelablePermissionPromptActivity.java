package de.tobifleig.lxc.plaf.android.activity;

/**
 * Any activity that uses permissions and needs a callback to cancel them gets this
 */
public interface CancelablePermissionPromptActivity {

    public void cancelPermissionPromptAction();
}
