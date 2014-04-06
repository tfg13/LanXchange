package de.tobifleig.lxc.plaf.impl.android;

import java.util.List;

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiListener;

/**
 * GuiListener for android with some extensions.
 *
 */
public abstract class AndroidGuiListener implements GuiListener {

    private GuiListener basicGuiListener;

    public AndroidGuiListener(GuiListener basicGuiListener) {
        this.basicGuiListener = basicGuiListener;
    }

    /**
     * Called everytime the gui is sent to the background.
     */
    public abstract void guiHidden();

    /**
     * Called everytime the gui becomes visible (again).
     */
    public abstract void guiVisible();


    @Override
    public List<LXCFile> getFileList() {
        return basicGuiListener.getFileList();
    }

    @Override
    public void offerFile(LXCFile newFile) {
        basicGuiListener.offerFile(newFile);
    }

    @Override
    public void removeFile(LXCFile oldFile) {
        basicGuiListener.removeFile(oldFile);
    }

    @Override
    public void downloadFile(LXCFile file, boolean chooseTarget) {
        basicGuiListener.downloadFile(file, chooseTarget);
    }

    @Override
    public void resetFile(LXCFile file) {
        basicGuiListener.resetFile(file);
    }

    @Override
    public void shutdown() {
        basicGuiListener.shutdown();
    }

    @Override
    public void reloadConfiguration() {
        basicGuiListener.reloadConfiguration();
    }

}
