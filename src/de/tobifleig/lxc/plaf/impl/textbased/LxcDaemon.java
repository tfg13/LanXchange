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

import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.plaf.Platform;
import de.tobifleig.lxc.plaf.impl.GenericPCPlatform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * The LXC Daemon runs LXC in the backgroud and talks to the LxcDaemonController
 * over a socket.
 *
 * @author Michael
 */
public class LxcDaemon implements Runnable {

    public static int LXC_DAEMON_PORT = 27718;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private LXCDaemonUserInterface lxcInterface;

    public static ArrayList<String> errorBuffer = new ArrayList<String>();

    public LxcDaemon() {
        try {
            lxcInterface = new LXCDaemonUserInterface();
            Platform plattform = new GenericPCPlatform(lxcInterface);
            lxcInterface.setDefaultDownloadTarget(plattform.getDefaultDownloadTarget());
            new LXC(plattform, new String[0]);
            serverSocket = new ServerSocket(LXC_DAEMON_PORT);
            serverThread = new Thread(this);
            serverThread.setName("LxcDaemonThread");
            serverThread.start();
            System.out.println("LxcDaemon started successfully.");
        } catch (IOException ex) {
            System.out.println("Error starting socket on port " + LXC_DAEMON_PORT);
            System.exit(1);
        }
    }

    @Override
    public void run() {
        boolean run = true;
        while (run) {
            try {
                Socket socket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // print the errors that have occured since the last call to LxcDaemon:
                printErrors(socket.getOutputStream());

                String[] commands = reader.readLine().split(" ");

                if (commands[0].equals("stop")) {
                    socket.getOutputStream().write("stopping...\n".getBytes());
                    socket.close();
                    run = false;
                    System.exit(0);
                } else if (commands[0].equals("list")) {
                    socket.getOutputStream().write((lxcInterface.getStatus()).getBytes());
                } else if (commands[0].equals("upload-file")) {
                    socket.getOutputStream().write((lxcInterface.uploadFile(commands[1])).getBytes());
                } else if (commands[0].equals("stop-uploading-file")) {
                    socket.getOutputStream().write((lxcInterface.stopUploadFile(Integer.parseInt(commands[1]))).getBytes());
                } else if (commands[0].equals("download")) {
                    String target = "";
                    if (commands.length > 2) {
                        target = commands[2];
                    }
                    socket.getOutputStream().write((lxcInterface.downloadFile(Integer.parseInt(commands[1]), target)).getBytes());
                } else {
                    socket.getOutputStream().write(("unknown command: " + commands[0]).getBytes());
                }

                // print the errors that have occured during command execution:
                printErrors(socket.getOutputStream());

                socket.getOutputStream().write("\n\n".getBytes());
                socket.close();
            } catch (Exception ex) {
                // Connection closed before a command was sent
                // lets just ignore this and go on with the next request
            }
        }
    }

    private void printErrors(OutputStream stream) {
        while (!errorBuffer.isEmpty()) {
            try {
                stream.write((errorBuffer.remove(0) + "\n").getBytes());
            } catch (IOException ex) {
                // connection error, abort
            }
        }
    }

}
