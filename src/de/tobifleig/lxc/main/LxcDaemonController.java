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
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", LxcDaemon.LXC_SERVER_PORT));
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
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
            default:
                // Its a command to the deamon, send it and print the answer:
                String command = "";
                command += args[1];
                for (int i = 2; i < args.length; i++) {
                    command += " " + args[i];
                }
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
    }
}
