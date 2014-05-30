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
package de.tobifleig.lxc.plaf.impl.textbased;

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.impl.ui.UpdateDialog;
import de.tobifleig.lxc.plaf.impl.ui.UserInterface;
import java.io.File;

/**
 * The UnserInterface that lets the daemon communicate with lxc.
 *
 * @author Michael
 */
public class LXCDaemonUserInterface implements UserInterface {

    @Override
    public void init(String[] args) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void display() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void showError(String error) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setGuiListener(GuiListener guiListener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public File getFileTarget(LXCFile file) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean confirmCloseWithTransfersRunning() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public UpdateDialog getUpdateDialog() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
