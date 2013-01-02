/*
 * Copyright 2009, 2010, 2011, 2012, 2013 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.net;

import de.tobifleig.lxc.data.FileManager;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.LXCJob;
import de.tobifleig.lxc.net.io.Leecher;
import de.tobifleig.lxc.net.io.Seeder;
import de.tobifleig.lxc.net.io.Transceiver;
import de.tobifleig.lxc.net.io.TransceiverListener;
import de.tobifleig.lxc.net.serv.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;

/**
 * Manages networking.
 * Manages (auto-detected) instances, incoming lists/requests.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class NetworkManager {

    /**
     * this server listens for incoming file-requests.
     */
    private FileServer fileServer;
    /**
     * this server listens for incoming file-lists.
     */
    private ListServer listServer;
    /**
     * this server listens for ping/keepalives sent by other instances.
     */
    private PingServer pingServer;
    /**
     * Manages and performs multicasts.
     */
    private HeartbeatSender multicaster;
    /**
     * Manages NetworkInterfaces.
     * Triggers required actions if an interface goes online or offline
     */
    private InterfaceManager interfacesurveillance;
    /**
     * Manages remote LXCInstances.
     */
    private InstanceManager instances;
    /**
     * Used to call LXC when certain events occur.
     */
    private NetworkManagerListener listener;
    /**
     * Contains all the running jobs.
     */
    private HashMap<Transceiver, LXCJob> jobs;
    /**
     * Manages all local/remote files.
     */
    private FileManager fileManager;

    /**
     * creates a new NetworkManager with the given parameters.
     *
     * @param listener the listener to pass events to
     * @param fileManager the filemanager who must be informed about received filelists etc.
     */
    public NetworkManager(NetworkManagerListener listener, FileManager fileManager) {
	this.listener = listener;
	this.fileManager = fileManager;
	jobs = new HashMap<Transceiver, LXCJob>();
	instances = new InstanceManager(new InstanceManagerListener() {
	    @Override
	    public void instanceAdded(LXCInstance newInstance) {
		broadcastList();
		// request list
		sendList(null, newInstance);
	    }

	    @Override
	    public void instanceRemoved(LXCInstance removedInstance) {
		NetworkManager.this.listener.instanceRemoved(removedInstance);
	    }
	});
	fileServer = new FileServer(new FileServerListener() {
	    @Override
	    public void downloadRequest(final LXCFile file, ObjectOutputStream outStream, ObjectInputStream inStream, InetAddress address, int transVersion) {
		final Seeder seed = new Seeder(outStream, inStream, file, transVersion);
		TransceiverListener leechListener = new TransceiverListener() {
		    @Override
		    public void progress() {
			NetworkManager.this.listener.triggerGui();
		    }

		    @Override
		    public void finished(boolean success) {
			file.removeJob(jobs.get(seed));
			NetworkManager.this.listener.triggerGui();
		    }
		};
		seed.setListener(leechListener);

		LXCJob job = new LXCJob(seed, instances.getByAddress(address));
		jobs.put(seed, job);
		file.addJob(job);

		seed.start();
	    }
	}, fileManager);
	listServer = new ListServer(new ListServerListener() {
	    @Override
	    public void listReceived(TransFileList list, InetAddress host) {
		NetworkManager.this.listener.listReceived(list, instances.getOrCreateInstance(host, list.getOriginId()));
	    }

	    @Override
	    public void listRequested() {
		broadcastList();
	    }
	});
	pingServer = new PingServer(new PingServerListener() {
	    @Override
	    public void pingReceived(byte[] data, InetAddress host) {
		instances.computePing(data, host);
	    }
	});
	multicaster = new HeartbeatSender();
	interfacesurveillance = new InterfaceManager(pingServer, multicaster);
    }

    /**
     * Attempts to start the networking-subsystem.
     * May fail if another instance of lxc is already running on this system. In this case, false is returned.
     *
     * @return true, if started. false if already running.
     */
    public boolean checkSingletonAndStart() {
	try {
	    listServer.start();
	    fileServer.start();
	    multicaster.start();
	    interfacesurveillance.start();
	    instances.start();
	    return true;
	} catch (BindException ex) {
	    // already running
	    return false;
	}
    }

    /**
     * Stops the networking-subsystem.
     * Triggers sending of a special offline-signal and stops sending heartbeats.
     * Please note that most ServerSockets are not closed, which means that the ports are not freed.
     * NetworkManager expectes the JVM to shut down shortly after calling this method.
     * It is known that this is considered bad practice and therefore it will likely be changed in future releases.
     */
    public void stop() {
	multicaster.stop();
    }

    /**
     * Tells NetworkManager to download the given file.
     * It is assumed that the given file is already locked.
     * NetworkManager will handle all futute locking/delocking of this file.
     *
     * @param file the requested file
     * @param targetFolder the folder to download the file(s) to
     * @return true, if connection could be established
     */
    public boolean connectAndDownload(final LXCFile file, File targetFolder) {
	try {
	    Socket server = new Socket();
	    server.setPerformancePreferences(0, 0, 1);
	    server.setSendBufferSize(212992);
	    server.setReceiveBufferSize(212992);
	    server.connect(new InetSocketAddress(file.getInstance().getDownloadAddress(), 27719));
	    ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(server.getOutputStream()));
	    output.flush();
	    ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(server.getInputStream()));

	    // send request
	    output.writeObject(file);
	    output.flush();

	    // read answer
	    byte result = input.readByte();

	    if (result == 'n') {
		// request refused
		System.out.println("Request refused!");
		file.setLocked(false);
	    } else if (result == 'y') {
		// accepted
		final Leecher leech = new Leecher(input, output, file, targetFolder, file.getLxcTransVersion());
		TransceiverListener leechListener = new TransceiverListener() {
		    @Override
		    public void progress() {
			listener.triggerGui();
		    }

		    @Override
		    public void finished(boolean success) {
			file.setLocked(false);
			if (success) {
			    file.setAvailable(true);
			    listener.downloadComplete(file);
			}
			file.removeJob(jobs.get(leech));
			listener.triggerGui();
		    }
		};
		leech.setListener(leechListener);
		LXCJob job = new LXCJob(leech, file.getInstance());
		jobs.put(leech, job);
		file.addJob(job);
		leech.start();
	    }
	} catch (NoRouteToHostException ex) {
	    System.out.println("Download aborted. No route to host.");
	    file.setLocked(false);
	    return false;
	} catch (IOException ex) {
	    System.out.println("Download aborted. IOException.");
	    file.setLocked(false);
	    ex.printStackTrace();
	}
	return true;
    }

    /**
     * Broadcasts out ownList to every known instance.
     * Creates a new Thread for this.
     */
    public void broadcastList() {
	Thread t = new Thread(new Runnable() {
	    @Override
	    public void run() {
		TransFileList list = fileManager.getTransFileList();
		// Vorbereitungen fertig, senden
		for (LXCInstance inst : instances.getRemotes()) {
		    sendList(list, inst);
		}
	    }
	});
	t.setDaemon(true);
	t.setName("lxc_helper_listsender");
	t.start();
    }

    /**
     * Sends the given TransFileList to the given LXCInstance
     *
     * @param list the list to send
     * @param dest the destination to send the list to
     */
    private void sendList(TransFileList list, LXCInstance dest) {
	try {
	    Socket sock = new Socket();
	    sock.setSoTimeout(2000);
	    sock.connect(new InetSocketAddress(dest.getDownloadAddress(), 27717));
	    ObjectOutputStream outp = new ObjectOutputStream(sock.getOutputStream());
	    outp.writeObject(list);
	    outp.flush();
	    outp.close();
	    sock.close();
	} catch (IOException ex) {
	    ex.printStackTrace();
	}
    }
}
