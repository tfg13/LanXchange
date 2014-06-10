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
                if (args.length < 3) {
                    System.out.println("what file should i upload?");
                    break;
                }
                String filename = args[2];
                try {
                    socket.getOutputStream().write(("upload-file " + filename + "\n").getBytes());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    if (!reader.readLine().equals("OK")) {
                        System.out.println("Error uploading file!");
                        System.out.println(reader.readLine());
                    } else {
                        System.out.println("File uploaded successfully!");
                    }
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Error uploading file.");
                }
                break;
            case "stop-uploading-file":
                if (args.length < 3) {
                    System.out.println("Stop uploading wich file?");
                }
                try {
                    int number = Integer.parseInt(args[2]);
                    socket.getOutputStream().write(("stop-uploading-file " + number + "\n").getBytes());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    if (!reader.readLine().equals("OK")) {
                        System.out.println("Error stopping file upload!");
                        System.out.println(reader.readLine());
                    } else {
                        System.out.println("Success");
                        System.out.println(reader.readLine());
                    }
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Error uploading file.");
                }catch(NumberFormatException ex2){
                    System.out.println("Usage: stop-uploading-file X\nWhere X is the number given to the file. Use -nogui list to see the numbers.");
                }
                break;
            case "download-file":
                System.out.println("not yet implemented");
                break;
            case "list":
                try {
                    socket.getOutputStream().write("list\n".getBytes());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String answer;
                    do {
                        answer = reader.readLine();
                        System.out.println(answer);
                    } while (!answer.equals(""));
                    socket.close();
                } catch (IOException ex) {
                    System.out.println("Error requesting list.");
                }
                break;
            default:
                System.out.print("unknown command: " + args[1]);
        }

//        GenericPCPlatform.startLXC(new LXCDaemonUserInterface(), args);
    }
}
