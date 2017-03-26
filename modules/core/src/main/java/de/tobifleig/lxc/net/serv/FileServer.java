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
package de.tobifleig.lxc.net.serv;

import de.tobifleig.lxc.data.FileManager;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;
import de.tobifleig.lxc.net.LookaheadObjectInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * This server listens for incoming download requests.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class FileServer implements Runnable {

    private final LXCLogger logger;
    /**
     * The listener to pass events to.
     */
    private FileServerListener listener;
    /**
     * The FileManager to get filelists etc from.
     */
    private FileManager fileManager;
    /**
     * The socket this server uses to listen to illegal commands.
     */
    private ServerSocket servSock;

    /**
     * Creates a new FileServer with the given parameters.
     *
     * @param listener the listener to pass events to
     * @param fileManager the filemanager, used to check if files are available for download
     */
    public FileServer(FileServerListener listener, FileManager fileManager) {
        this.logger = LXCLogBackend.getLogger("file-server");
        this.listener = listener;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        try {
            servSock = new ServerSocket();
            servSock.setReceiveBufferSize(212992);
            servSock.setPerformancePreferences(0, 0, 1);
            servSock.bind(new InetSocketAddress(27719));

            while (true) {
                Socket client;
                try {
                    client = servSock.accept();
                } catch (SocketException ex) {
                    // servSock was closed, ignore
                    break;
                }

                client.setSendBufferSize(212992);
                ObjectInputStream input;
                ObjectOutputStream output;
                try {
                    output = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
                    output.flush();
                    input = new LookaheadObjectInputStream(new BufferedInputStream(client.getInputStream()));
                    try {
                        LXCFile file = (LXCFile) input.readObject();
                        if (file != null) {
                            // get version number
                            int version = file.getLxcTransVersion();
                            // file still available?
                            file = fileManager.localRepresentation(file);
                            if (file != null && file.isLocal()) {
                                // send ACK, start transfer
                                output.writeByte('y');
                                output.flush();
                                listener.downloadRequest(client, file, output, input, client.getInetAddress(), version);
                            } else {
                                // refuse request
                                output.writeByte('n');
                                output.flush();
                            }
                        }
                    } catch (ClassNotFoundException | ClassCastException ex) {
                        logger.warn("Cannot read incoming file request", ex);
                    }
                } catch (IOException ex) {
                    logger.warn("Problem receiving file request", ex);
                }
            }

        } catch (Exception ex) {
            logger.error("Unrecoverable problem", ex);
        }
    }

    /**
     * Starts this server.
     */
    public void start() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("fileserver");
        thread.start();
    }

    /**
     * Stop this server.
     */
    public void stop() {
        try {
            servSock.close();
        } catch (IOException ex) {
            // ignore
        }
    }
}
