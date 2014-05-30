package de.tobifleig.lxc.main;

import de.tobifleig.lxc.plaf.impl.GenericPCPlatform;
import de.tobifleig.lxc.plaf.impl.swing.SwingGui;

/**
 * Start LXC with a swing GUI.
 *
 * @author Michael
 */
public class SwingGUILauncher {

    public SwingGUILauncher(String args[]) {
        GenericPCPlatform.startLXC(new SwingGui(), args);
    }

}
