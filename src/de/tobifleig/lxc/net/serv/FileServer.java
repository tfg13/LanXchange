/*
 * Copyright 2009, 2010, 2011, 2012 Tobias Fleig (tobifleig gmail com)
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
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This server listens for incoming download requests.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class FileServer implements Runnable {

    /**
     * The listener to pass events to.
     */
    private FileServerListener listener;
    /**
     * The FileManager to get filelists etc from.
     */
    private FileManager fileManager;

    /**
     * Creates a new FileServer with the given parameters.
     *
     * @param listener the listener to pass events to
     * @param fileManager the filemanager, used to check if files are available for download
     */
    public FileServer(FileServerListener listener, FileManager fileManager) {
	this.listener = listener;
	this.fileManager = fileManager;
    }

    @Override
    public void run() {
	try {
	    ServerSocket servSock = new ServerSocket();
	    servSock.setReceiveBufferSize(212992);
	    servSock.setPerformancePreferences(0, 0, 1);
	    servSock.bind(new InetSocketAddress(27719));

	    while (true) {
		Socket client = servSock.accept();
		client.setSendBufferSize(212992);
		ObjectInputStream input;
		ObjectOutputStream output;
		try {
		    output = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
		    output.flush();
		    input = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
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
				listener.downloadRequest(file, output, input, client.getInetAddress(), version);
			    } else {
				// refuse request
				output.writeByte('n');
				output.flush();
			    }
			}
		    } catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		    } catch (ClassCastException ex) {
			ex.printStackTrace();
		    }
		} catch (IOException ex) {
		    ex.printStackTrace();
		}
	    }

	} catch (Exception ex) {
	    ex.printStackTrace();
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
}
