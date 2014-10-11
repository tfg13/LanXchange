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
            public void downloadRequest(Socket socket, final LXCFile file, ObjectOutputStream outStream, ObjectInputStream inStream, InetAddress address, int transVersion) {
                final Seeder seed = new Seeder(socket, outStream, inStream, file, transVersion);
                TransceiverListener seedListener = new TransceiverListener() {
                    @Override
                    public void finished(boolean success, boolean removeFile) {
                        file.removeJob(jobs.get(seed), success);
                        if (removeFile) {
                            // File no longer available --> remove
                            NetworkManager.this.fileManager.removeLocal(file);
                            broadcastList();
                            NetworkManager.this.listener.uploadFailedFileMissing(file);
                        }
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
     * Afterwards, closes all sockets and threads created by the networking subsystem.
     * Kills all running transfers without further notice.
     */
    public void stop() {
        multicaster.stop(instances.getRemotes());
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
                final Leecher leech = new Leecher(server, input, output, file, targetFolder, file.getLxcTransVersion());
                TransceiverListener leechListener = new TransceiverListener() {
                    @Override
                    public void finished(boolean success, boolean removeFile) {
                        file.setLocked(false);
                        if (success) {
                            file.setAvailable(true);
                            listener.downloadComplete(file, targetFolder);
                        }
                        file.removeJob(jobs.get(leech), success);
                        if (removeFile) {
                            listener.downloadFailedFileMissing();
                        }
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
