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
package de.tobifleig.lxc.net;

import de.tobifleig.lxc.data.FileManager;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.LXCJob;
import de.tobifleig.lxc.net.io.Leecher;
import de.tobifleig.lxc.net.io.Seeder;
import de.tobifleig.lxc.net.io.Transceiver;
import de.tobifleig.lxc.net.io.TransceiverListener;
import de.tobifleig.lxc.net.serv.FileServer;
import de.tobifleig.lxc.net.serv.FileServerListener;
import de.tobifleig.lxc.net.serv.ListServer;
import de.tobifleig.lxc.net.serv.ListServerListener;
import de.tobifleig.lxc.net.serv.PingServer;
import de.tobifleig.lxc.net.serv.PingServerListener;
import de.tobifleig.lxc.plaf.Platform;
import de.tobifleig.lxc.plaf.ProgressIndicator;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    private final FileServer fileServer;
    /**
     * this server listens for incoming file-lists.
     */
    private final ListServer listServer;
    /**
     * this server listens for ping/keepalives sent by other instances.
     */
    private final PingServer pingServer;
    /**
     * Manages and performs multicasts.
     */
    private final HeartbeatSender multicaster;
    /**
     * Manages NetworkInterfaces.
     * Triggers required actions if an interface goes online or offline
     */
    private final InterfaceManager interfacesurveillance;
    /**
     * Manages remote LXCInstances.
     */
    private final InstanceManager instances;
    /**
     * Used to call LXC when certain events occur.
     */
    private final NetworkManagerListener listener;
    /**
     * Contains all the running jobs.
     */
    private final HashMap<Transceiver, LXCJob> jobs;
    /**
     * Manages all local/remote files.
     */
    private final FileManager fileManager;

    /**
     * creates a new NetworkManager with the given parameters.
     *
     * @param listener the listener to pass events to
     * @param fileManager the filemanager who must be informed about received filelists etc.
     * @param platform the platform we are running on
     */
    public NetworkManager(final NetworkManagerListener listener, FileManager fileManager, Platform platform) {
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
            public void downloadRequest(Socket socket, final LXCFile file, ObjectOutputStream outStream, ObjectInputStream inStream, InetAddress address, int transVersion) {
                final Seeder seed = new Seeder(socket, outStream, inStream, file, transVersion);
                TransceiverListener seedListener = new TransceiverListener() {
                    @Override
                    public void finished(boolean success, boolean removeFile) {
                        int removeIndex = file.getJobs().indexOf(jobs.get(seed));
                        listener.notifyRemoveJob(file, removeIndex);
                        file.removeJob(jobs.get(seed), success);
                        if (removeFile) {
                            // File no longer available --> remove
                            NetworkManager.this.fileManager.removeLocal(file);
                            broadcastList();
                            NetworkManager.this.listener.uploadFailedFileMissing(file);
                        }
                        jobs.remove(seed);
                        NetworkManager.this.listener.refreshGui();
                    }
                };
                seed.setListener(seedListener);
                seed.setProgressIndicator(new ProgressIndicator() {

                    @Override
                    public void update(int percentage) {
                        NetworkManager.this.listener.refreshGui();
                    }
                });

                LXCJob job = new LXCJob(seed, file.getInstance());
                jobs.put(seed, job);
                file.addJob(job);
                listener.notifyJobAdded(file, file.getJobs().size() - 1);

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
        multicaster = new HeartbeatSender(platform.getRequiredMulticastHelpers(), instances.getRemotes());
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
     * Afterwards, closes all sockets and threads created by the networking subsystem.
     * Kills all running transfers without further notice.
     */
    public void stop() {
        multicaster.stop();
        interfacesurveillance.stop();
        fileServer.stop();
        listServer.stop();
        instances.stop();
        for (LXCJob job : jobs.values()) {
            job.abortTransfer();
        }
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
    public boolean connectAndDownload(final LXCFile file, final File targetFolder) {
        try {
            Socket server = new Socket();
            server.setPerformancePreferences(0, 0, 1);
            server.setSendBufferSize(212992);
            server.setReceiveBufferSize(212992);
            if (!connectToInstance(server, file.getInstance(), 27719)) {
                System.out.println("Download aborted. (See exceptions above)");
                file.setLocked(false);
                return false;
            }
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
                final Leecher leech = new Leecher(server, input, output, file, targetFolder, file.getLxcTransVersion());
                TransceiverListener leechListener = new TransceiverListener() {
                    @Override
                    public void finished(boolean success, boolean removeFile) {
                        file.setLocked(false);
                        if (success) {
                            file.setAvailable(true);
                            listener.downloadComplete(file, targetFolder);
                        }
                        int removeIndex = file.getJobs().indexOf(jobs.get(leech));
                        listener.notifyRemoveJob(file, removeIndex);
                        file.removeJob(jobs.get(leech), success);
                        if (removeFile) {
                            listener.downloadFailedFileMissing();
                        }
                        jobs.remove(leech);
                        listener.refreshGui();
                    }
                };
                leech.setListener(leechListener);
                leech.setProgressIndicator(new ProgressIndicator() {

                    @Override
                    public void update(int percentage) {
                        listener.refreshGui();
                    }
                });
                LXCJob job = new LXCJob(leech, file.getInstance());
                jobs.put(leech, job);
                file.addJob(job);
                listener.notifyJobAdded(file, file.getJobs().size() - 1);
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
                // prep done, send
                while (true) {
                    try {
                        for (LXCInstance inst : instances.getRemotes()) {
                            sendList(list, inst);
                        }
                        // this worked
                        break;
                    } catch (ConcurrentModificationException ex) {
                        // whoops, instance list was changed while iterating over it
                        // just re-send everything (= continue while)
                        System.out.println("Warning: Re-starting list broadcast due to unexpected instance list modification");
                        // this is obviously not a proper fix, but most fixes are much uglier
                        // since java lacks a CopyOnWriteMap
                        // additionally, this only happened once in 4 years ;)
                    }
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
            if (connectToInstance(sock, dest, 27717)) {
                ObjectOutputStream outp = new ObjectOutputStream(sock.getOutputStream());
                outp.writeObject(list);
                outp.flush();
                outp.close();
                sock.close();
            } // no else, errors are reported by connectToInstance
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Tries connecting the socket to the given instance, using all known addresses.
     *
     * @param socket the socket to connect
     * @param instance the instance to connect to
     * @param port the port to connect to
     * @return true iff successful
     */
    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private boolean connectToInstance(Socket socket, LXCInstance instance, int port) {
        // holds the exceptions encountered during the connection attempt.
        // only printed to logfile if *all* attempts fail
        Map<InetAddress, Exception> exceptions = new HashMap<InetAddress, Exception>();
        Iterator<InetAddress> iter = instance.getAddresses();
        boolean connected = false;
        while (!connected && iter.hasNext()) {
            InetAddress address = iter.next();
            try {
                socket.connect(new InetSocketAddress(address, port));
                connected = true;
            } catch (IOException ex) {
                System.out.println("Attempt to connect to " + address + " failed.");
                exceptions.put(address, ex);
            }
        }

        // print debug if not successful
        if (!connected) {
            System.out.println("All connection attempts to " + instance + "failed. Detailed errors for all addresses:");
            for (InetAddress address : exceptions.keySet()) {
                System.out.println("CON to " + address + " resulted in:");
                exceptions.get(address).printStackTrace();
            }
        }

        return connected;
    }
}
