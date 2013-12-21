package de.tobifleig.lxc.plaf.impl.swing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
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
import java.net.UnknownHostException;
import java.util.Enumeration;
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
        "img/stop.png",
        "img/txt.png",
        "img/update.png",
        "img/selfdist.png",
        "img/selfdist_small.png",
        "COPYING",
        "Ubuntu-R.ttf",
        "font_license.txt",
        "lanxchange.jar",
        "lxc",
        "lxc.exe",
        "lxc_updates.pub"};

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
                        System.out.println("selfdist: Download from: " + he.getRemoteAddress());
                        //he.setAttribute("Content-Type", "application/octet-stream");
                        he.sendResponseHeaders(200, lxc.length);
                        OutputStream out = he.getResponseBody();
                        out.write(lxc);
                        out.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }

            });
            httpServer.setExecutor(null);
            httpServer.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void stopServer() {
        if (httpServer != null) {
            httpServer.stop(1);
        }
    }

    private static void computeAddresses() {
        try {
            // get hostname & own ip
            String hostname = InetAddress.getLocalHost().getHostName();
            String ip = getBestLocalIp();
            dialog.setAddresses("http://" + hostname + ":8087/lxc.zip", "http://" + ip + ":8087/lxc.zip");
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
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
            System.out.println("selfdist: distribution created, size " + lxc.length);
        } catch (IOException ex) {
            ex.printStackTrace();
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
            // Manual search:
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
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println("selfdist: Unable to guess local ip!");
        return "127.0.0.1";
    }

    /**
     * private constructor, utility class...
     */
    private SelfDistributor() {
    }

}
