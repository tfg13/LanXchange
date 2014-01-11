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

import de.tobifleig.lxc.net.TransFileList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The ListServer listens to port 27717 and receives file lists of remote instances.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class ListServer implements Runnable {

    /**
     * The ServerSocket used.
     */
    private ServerSocket servSock;
    /**
     * The listener, used to deliver incoming filelists.
     */
    private ListServerListener listener;

    @Override
    public void run() {
        try {
            while (true) {
                Socket client = servSock.accept(); // Wait for next list, blocks
                try {
                    ObjectInputStream input = new ObjectInputStream(client.getInputStream());
                    try {
                        TransFileList list = (TransFileList) input.readObject();
                        if (list != null) {
                            listener.listReceived(list, client.getInetAddress());
                        } else {
                            // List-request
                            listener.listRequested();
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
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Binds the Socket to port 27717.
     * Used for the single-instance-check.
     *
     * @throws BindException
     */
    private void bindSocket() throws BindException {
        try {
            servSock = new ServerSocket(27717);
        } catch (IOException ex) {
            if (ex instanceof BindException) {
                throw new BindException();
            }
        }
    }

    /**
     * Creates a new ListServer with the given parameters.
     *
     * @param listener the listener used to deliver incoming filelists
     */
    public ListServer(ListServerListener listener) {
        this.listener = listener;
    }

    /**
     * Starts the ListServer.
     * Binds to port 27717.
     *
     * @throws BindException if port is already occupied
     */
    public void start() throws BindException {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("listserver");
        this.bindSocket();
        thread.start();
    }
}
