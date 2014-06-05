package de.tobifleig.lxc.main;

import de.tobifleig.lxc.plaf.impl.GenericPCPlatform;
import de.tobifleig.lxc.plaf.impl.textbased.LXCDaemonUserInterface;
import java.net.Socket;

/**
 * Control the Lxc-Daemon. Sends start, stop, file-upload etc. commands to the daemon
 *
 * @author Michael
 */
public class LxcDaemonController {

    /**
     * The socket used to communicate to the Lxc-Daemon
     */
    private Socket socket;

    public LxcDaemonController(String args[]) {
        switch (args[1]) {
            case "status":
                System.out.println("not yet implemented");
                break;
            case "start":
                System.out.println("not yet implemented");
                break;
            case "stop":
                System.out.println("not yet implemented");
                break;
            case "upload-file":
                System.out.println("not yet implemented");
                break;
            case "download-file":
                System.out.println("not yet implemented");
                break;
            default:
                System.out.print("unknown command: " + args[1]);
        }

//        GenericPCPlatform.startLXC(new LXCDaemonUserInterface(), args);
    }
}
