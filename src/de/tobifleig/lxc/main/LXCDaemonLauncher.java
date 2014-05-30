package de.tobifleig.lxc.main;

import de.tobifleig.lxc.plaf.impl.GenericPCPlatform;
import de.tobifleig.lxc.plaf.impl.textbased.LXCDaemonUserInterface;

/**
 * Start LXC in headless mode.
 *
 * @author Michael
 */
public class LXCDaemonLauncher {

    public LXCDaemonLauncher(String args[]) {
        GenericPCPlatform.startLXC(new LXCDaemonUserInterface(), args);
    }
}
