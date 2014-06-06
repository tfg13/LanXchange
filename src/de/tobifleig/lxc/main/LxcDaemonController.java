package de.tobifleig.lxc.main;

import de.tobifleig.lxc.plaf.impl.GenericPCPlatform;
import de.tobifleig.lxc.plaf.impl.textbased.LXCDaemonUserInterface;
import de.tobifleig.lxc.plaf.impl.textbased.LxcDaemon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
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
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", LxcDaemon.LXC_SERVER_PORT));
        } catch (IOException ex) {
            // cant connect, lxcd propably isnt running
        }
        switch (args[1]) {
            case "status":
                if (socket.isConnected()) {

                    System.out.println("LxcDaemon is running.");

                } else {
                    System.out.println("LxcDaemon is NOT running.");
                }
                break;
            case "start":
                try {
                    ProcessBuilder builder = new ProcessBuilder();
                    builder.environment().putAll(System.getenv());
                    builder.command("java", "-jar", "LXC.jar", "-nogui", "daemonize");
                    builder.start();
                } catch (IOException ex) {
                    System.out.println("Error starting lxc daemon");
                    ex.printStackTrace();
                }
                break;
            case "daemonize":
                LxcDaemon daemon = new LxcDaemon();
                break;
            case "stop":
                if (socket.isConnected()) {
                    try {
                        socket.getOutputStream().write("stop\n".getBytes());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String answer = reader.readLine();
                        if (answer.equals("stopping...")) {
                            System.out.println("LXC Daemon shutting down.");
                        } else {
                            System.out.println("Error stopping daemon.");
                        }
                        socket.close();
                    } catch (IOException ex) {
                        System.out.println("Error stopping daemon.");
                    }
                } else {
                    System.out.println("LXC Daemon is not running.");
                }
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
