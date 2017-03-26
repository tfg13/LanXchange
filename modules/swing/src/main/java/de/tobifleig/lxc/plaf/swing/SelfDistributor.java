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
package de.tobifleig.lxc.plaf.swing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This utility helps LanXchange to share itself.
 * This solves the long-standing chicken-or-egg-problem:
 * When relying on LXC to share files - how to share LXC?
 *
 * The functionality is rather simple. A webserver is created,
 * allowing other computers to download lxc.
 *
 * @author Tobias Fleig <tobifleig@googlemail.com>
 */
public final class SelfDistributor {

    private static final LXCLogger logger = LXCLogBackend.getLogger("selfdist");
    /**
     * The GUI.
     */
    private static SelfDistributorDialog dialog;
    /**
     * True while the server is enabled.
     */
    private static boolean enabled = false;
    /**
     * The file server.
     * Really just a simple http server that answers everything with a copy of LXC.
     */
    private static HttpServer httpServer;
    /**
     * A zip-file, containing all of lxc.
     * Dynamically generated at first server startup
     */
    private static byte[] lxc;

    private static final String[] fileList = new String[]{
        "img/busy.png",
        "img/cancel.png",
        "img/del.png",
        "img/done.png",
        "img/download.png",
        "img/file.png",
        "img/folder.png",
        "img/harddisk.png",
        "img/help.png",
        "img/logo.png",
        "img/mini.png",
        "img/multiple.png",
        "img/plus.png",
        "img/screw.png",
        "img/selfdist.png",
        "img/selfdist_small.png",
        "img/small.png",
        "img/stop.png",
        "img/txt.png",
        "img/update.png",
        "img/drop.png",
        "COPYING",
        "3rd_party_licenses/font_license.txt",
        "lanxchange.jar",
        "lxc",
        "lxc.exe",};

    /**
     * Displays the GUI.
     *
     * @param parent the parent frame
     */
    public static void showGui(Frame parent) {
        if (dialog == null) {
            dialog = new SelfDistributorDialog(parent, false);
            computeAddresses();
            dialog.setStartStopListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    if (enabled) {
                        dialog.setButtonText("Enable Self Distribution");
                        stopServer();
                    } else {
                        dialog.setButtonText("Disable Self Distribution");
                        startServer();
                    }
                    enabled = !enabled;
                }
            });
        }
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static void startServer() {
        if (lxc == null) {
            createDistribution();
        }
        try {
            httpServer = HttpServer.create(new InetSocketAddress(8087), 0);
            httpServer.createContext("/lxc", new HttpHandler() {

                @Override
                public void handle(HttpExchange he) {
                    try {
                        logger.info("Download from: " + he.getRemoteAddress());
                        //he.setAttribute("Content-Type", "application/octet-stream");
                        he.sendResponseHeaders(200, lxc.length);
                        OutputStream out = he.getResponseBody();
                        out.write(lxc);
                        out.close();
                    } catch (IOException ex) {
                        logger.warn("Download failed", ex);
                    }
                }

            });
            httpServer.setExecutor(null);
            httpServer.start();
        } catch (IOException ex) {
            logger.error("Unrecoverable error in httpserver", ex);
        }
    }

    private static void stopServer() {
        if (httpServer != null) {
            httpServer.stop(1);
        }
    }

    private static void computeAddresses() {
        // get hostname & own ip
        String hostname = getBestHostName();
        String ip = getBestLocalIp();
        dialog.setAddresses("http://" + hostname + ":8087/lxc.zip", "http://" + ip + ":8087/lxc.zip");
    }

    private static void createDistribution() {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            // create a zipfile with all required files
            ZipOutputStream outStream = new ZipOutputStream(buffer);
            for (String fileName : fileList) {
                ZipEntry entry = new ZipEntry(fileName);
                outStream.putNextEntry(entry);
                // read + store file
                FileInputStream fileIn = new FileInputStream(fileName);
                byte[] copyBuffer = new byte[1024];
                int numBytes;
                while ((numBytes = fileIn.read(copyBuffer)) > 0) {
                    outStream.write(copyBuffer, 0, numBytes);
                }
                fileIn.close();
                outStream.closeEntry();
            }
            outStream.close();
            lxc = buffer.toByteArray();
            logger.info("distribution created, size " + lxc.length);
        } catch (IOException ex) {
            logger.error("Cannot create distribution", ex);
        }
    }

    /**
     * This tries to figure out a local Ip address.
     * The problem with InetAddress.getLocalHost().getHostAddress() is that it sometimes returns loopback addresses.
     *
     * @return the best known local address
     */
    private static String getBestLocalIp() {
        try {
            // use default method if it produces serious results:
            InetAddress auto = InetAddress.getLocalHost();
            if (!auto.isLoopbackAddress()) {
                return auto.getHostAddress();
            }
        } catch (IOException ex) {
            // ignore, search manually
        }
        // Manual search:
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface inter = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = inter.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress cand = inetAddresses.nextElement();
                    if (!cand.isLoopbackAddress() && cand instanceof Inet4Address) {
                        // just pick this one
                        return cand.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            // nothing works!
            logger.error("Unexpected exception during local ip guessing", ex);
        }

        logger.error("Unable to guess local ip!");
        return "127.0.0.1";
    }

    /**
     * Try to figure out a usable host name.
     * I used InetAddress.getLocalHost().getHostName() for a while, but it can crash or return "localhost".
     *
     * @return a best-effort host name for this machine
     */
    private static String getBestHostName() {
        // try old method first, works fine most of the time
        try {
            String naiveResult = InetAddress.getLocalHost().getHostName();
            if (naiveResult != null && !naiveResult.equals("localhost")) {
                // just use this
                return naiveResult;
            }
        } catch (UnknownHostException ex) {
            // this did not work, try more sophisticated methods
        }

        String envVar;
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            envVar = "COMPUTERNAME";
        } else { // assume *nix
            envVar = "HOSTNAME";
        }

        String envResult = System.getenv(envVar);
        if (envResult != null && !envResult.equals("localhost")) {
            return envResult;
        }

        String hostnameResult = execHelper("hostname");
        if (hostnameResult != null && !hostnameResult.equals("localhost")) {
            return hostnameResult;
        }

        // special trick for *nix
        if (!System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            String etcHostnameResult = execHelper("cat /etc/hostname");
            if (etcHostnameResult != null && !etcHostnameResult.equals("localhost")) {
                return etcHostnameResult;
            }
        }

        logger.error("Giving up on local host name!");
        return "localhost";
    }

    private static String execHelper(String cmd) {
        try {
            Process proc = Runtime.getRuntime().exec(cmd);
            try (Scanner s = new Scanner(proc.getInputStream()).useDelimiter("\\A")) {
                return (s.hasNext() ? s.next() : "").trim();
            }
        } catch (IOException ex) {
            logger.warn("Running \"" + cmd + "\" failed:", ex);
        }
        return null;
    }

    /**
     * private constructor, utility class...
     */
    private SelfDistributor() {
    }

}
