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
import de.tobifleig.lxc.plaf.impl.GenericPCPlatform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The LXC Daemon runs LXC in the backgroud and talks to the LxcDaemonController over a socket.
 *
 * @author Michael
 */
public class LxcDaemon implements Runnable {

    public static int LXC_SERVER_PORT = 12345;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private LXCDaemonUserInterface lxcInterface;

    public LxcDaemon() {
        try {
            lxcInterface = new LXCDaemonUserInterface();
            new LXC(new GenericPCPlatform(lxcInterface), new String[0]);
            serverSocket = new ServerSocket(LXC_SERVER_PORT);
            serverThread = new Thread(this);
            serverThread.setName("LxcDaemonThread");
            serverThread.start();
            System.out.println("LxcDaemon started successfully.");
        } catch (IOException ex) {
            System.out.println("Error starting socket on port " + LXC_SERVER_PORT);
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

                String[] commands = reader.readLine().split(" ");

                switch (commands[0]) {
                    case "status":
                        socket.getOutputStream().write("lxcd running".getBytes());
                        socket.close();
                        break;
                    case "stop":
                        socket.getOutputStream().write("stopping...".getBytes());
                        socket.close();
                        System.exit(0);
                        break;
                    case "list":
                        socket.getOutputStream().write((lxcInterface.getStatus() + "\n").getBytes());
                        socket.close();
                        break;
                    case "upload-file":
                        socket.getOutputStream().write((lxcInterface.uploadFile(commands[1]) + "\n").getBytes());
                        socket.close();
                        break;
                    case "stop-uploading-file":
                        socket.getOutputStream().write((lxcInterface.stopUploadFile(Integer.parseInt(commands[1])) + "\n").getBytes());
                        socket.close();
                        break;
                    default:
                        socket.getOutputStream().write(("unknown command: " + commands[0]).getBytes());
                        socket.close();
                }
            } catch (IOException ex) {
                // Connection closed before a command was sent
                // lets just ignore this and go on with the next request
            }
        }
    }

}
