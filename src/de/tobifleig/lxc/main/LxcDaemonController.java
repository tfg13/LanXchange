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
package de.tobifleig.lxc.main;

import de.tobifleig.lxc.plaf.impl.textbased.LxcDaemon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

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
    /**
     * The reader to read from the sockets inputstream.
     */
    private BufferedReader reader;

    public LxcDaemonController(String args[]) {

        if (Arrays.asList(args).contains("status")) {
            if (tryConnectSocket()) {
                System.out.println("LxcDaemon is running.");
            } else {
                System.out.println("LxcDaemon is NOT running.");
            }
        } else if (Arrays.asList(args).contains("start")) {
            if (startDaemon()) {
                System.out.println("LxcDaemon started successfully.");
            } else {
                System.out.println("Error starting LxcDaemon.");
            }
        } else if (Arrays.asList(args).contains("daemonize")) {
            LxcDaemon daemon = new LxcDaemon();
        } else if (Arrays.asList(args).contains("stop")) {
            if (tryConnectSocket()) {
                try {
                    socket.getOutputStream().write("stop\n".getBytes());
                    String answer = reader.readLine();
                    if (answer.equals("stopping...")) {
                        System.out.println("LxcDaemon shutting down.");
                    } else {
                        System.out.println("Error stopping LxcDaemon.");
                    }
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Error stopping LxcDaemon.");
                }
            } else {
                System.out.println("LxcDaemon is not running.");
            }
        } else if (Arrays.asList(args).contains("help") || Arrays.asList(args).contains("halp") || Arrays.asList(args).contains("hlep")) {
            System.out.println("Usage:");
            System.out.println("Show status:                lxc -nogui status");
            System.out.println("Start daemon:               lxc -nogui start");
            System.out.println("Stop daemon:                lxc -nogui stop");
            System.out.println("List available files:       lxc -nogui list");
            System.out.println("Download file:              lxc -nogui download [number]");
            System.out.println("Upload file:                lxc -nogui upload-file [file]");
            System.out.println("Stop Uploading file:        lxc -nogui stop-uploading-file [file]");
        } else {
            // Its a command to the deamon, send it and print the answer:
            if (!tryConnectSocket()) {
                if (startDaemon()) {
                    System.out.println("LxcDaemon started.");
                } else {
                    System.out.println("Cant start LxcDaemon!");
                    tryCloseSocket();
                    return;
                }
            }
            String command = "";
            boolean daemonargs = false;
            for (int i = 0; i < args.length; i++) {
                if (daemonargs) {
                    command += " " + args[i];
                } else if (args[i].equals("-nogui")) {
                    daemonargs = true;
                }
            }
            command = command.substring(1);
            try {
                socket.getOutputStream().write(command.getBytes());
                socket.getOutputStream().write("\n".getBytes());
                String answer;
                do {
                    answer = reader.readLine();
                    System.out.println(answer);
                } while (!answer.isEmpty());
                socket.close();
            } catch (IOException ex) {
                System.out.println("Cant connect to LXCDaemon.");
            }
        }
        tryCloseSocket();
    }

    private boolean startDaemon() {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.environment().putAll(System.getenv());
//                builder.command("java", "-jar", "LXC.jar", "-nogui", "daemonize");
            builder.command("lxc", "-nogui", "daemonize");
            builder.start();
            return tryConnectSocket();
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean tryConnectSocket() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", LxcDaemon.LXC_DAEMON_PORT));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private void tryCloseSocket() {
        try {
            reader.close();
            socket.close();
        } catch (Exception ex) {

        }
    }
}
