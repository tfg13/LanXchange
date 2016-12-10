/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Tobias Fleig (tobifleig gmail com)
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

import de.tobifleig.lxc.LXC;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class manages automated updates for swing-platforms.
 * The update system never installs anything without confirmation.
 *
 * This system downloads unauthenticated data from the internet.
 * The author is aware of this and changed this system multiple times over the years in order to harden it against many
 * possible attack vectors. The following is a non-exhaustive list of vectors that have been considered and how
 * this updater deals with them.
 *
 * - Generic manipulation of update file by MitM or malicious update server:
 *      + Updates are signed and only a single, pinned certificate is accepted.
 * - Zip extraction/directory traversals attacks
 *      + Before the verification is complete, the updater only extracts two files with fixed names.
 *      + Directory traversals beyond (up) LanXchange main directory are always prevented (even for valid signatures).
 * - Version number trickery / downgrade attacks
 *      + The updater verifies the server really distributes a valid signature for the version it claims to distribute.
 *      + The updater only accepts updates newer than the version that is running.
 * - Manipulation of unauthenticated content displayed to the user before update
 *      + The version number comes from a text file that is read unauthenticated, the updater only accepts
 *        a very limited number of bytes over this channel and limits all data to very few ASCII chars to stop fancy
 *        trickery with unicode.
 *
 * By default, LanXchange checks for updates on every start.
 * This check can be disabled by unchecking the corresponding option in the settings menu or by putting
 * "allowupdates=false" in the config file.
 *
 * LanXchange does not transfer any personal data during the update process.
 * The author does not collect any data via the update mechanism.
 * Note that the updates are hosted on servers run by Amazon AWS and/or Github, which may log your IP address, time of request, etc.
 *
 * In order to check for updates, LanXchange downloads the version description (updates.lanxchange.com/v) and
 * compares the offered version to the current version.
 *
 * If a newer version is found, the user is prompted.
 *
 * The first installation step is to download the update.
 * The update then extracts two fixed files, a zip file with a full installation and a signature file.
 *
 * During the verification, the updater checks the signature in order to make sure the zip file with the
 * full installation is unaltered. The only accepted public key resides in lxc_updates.pub.
 * If the signature is valid, the updater extracts a version file ("v") from the verified zip in order
 * to prevent downgrade attacks. Updates are only accepted, if the supplied version matches the version the server claimed
 * to send and the version is newer. Exception: when started with the command line option "-forceUpdate", the updater
 * will also accept re-installs of the same version (but still no older ones).
 *
 * After the verification, the updater extracts all files and triggers a restart.
 *
 * The signature is created by de.tobifleig.lxc.util.Signer, but for obvious reasons, the private key is not distributed.
 *
 * All verification steps can be skipped by command line options. This is logged.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public final class LXCUpdater {

    /**
     * Files contained in older versions that can be deleted if they still exist.
     */
    private static final String[] oldFiles = new String[]{"lxc_debug.exe", "LXC.ico", "Ubuntu-R.ttf", "lxc_updates.pub", "font_license.txt"};

    /**
     * Max number of bytes read from unauthenticated text sources.
     */
    private static final int versionBytesLimit = 32;

    /**
     * Max number of bytes for the full update file
     */
    private static final int fullUpdateSizeLimit = 512 * 1024 * 1024; // 512MiB

    /**
     * Checks for updates, prompts the user and installs them.
     *
     * @param gui the gui, required to display the dialog
     * @param forceUpdate skips online version check and allows re-installation of the current version. Does *not* disable protection against downgrade attacks
     * @param overrideVerification skips signature check (dangerous!)
     * @param allowDowngrade disables downgrade check (dangerous!)
     * @param restartable if true, whoever started LXC is aware of this update system and wants to be notified when LXC should be restarted rather than regularly terminated (this is done by returning exit code 6 rather than 0)
     * @throws Exception may throw a bunch of exceptions, this class requires, working internet, github, signature checks etc.
     */
    public static void checkAndPerformUpdate(SwingGui gui, boolean forceUpdate, boolean overrideVerification, boolean allowDowngrade, boolean restartable) throws Exception {
        if (forceUpdate) {
            System.out.println("Info: Forcing update...");
        }
        if (overrideVerification) {
            System.out.println("Warning: Update signature check disabled by startup flag");
        }
        if (allowDowngrade) {
            System.out.println("Warning: Update downgrade protection disabled by startup flag");
        }

        // Contact update server, download version file
        Scanner scanner = new Scanner(new VersionDataFilterInputStream(new URL("http://updates.lanxchange.com/v").openStream(), versionBytesLimit), "utf8");
        int gotver = Integer.parseInt(scanner.nextLine());
        String title = scanner.nextLine();
        scanner.close();
        // compare version number
        if (gotver > LXC.versionId || forceUpdate) {
            System.out.println("Newer Version available!");
            UpdateDialog updateGui = new UpdateDialog(gui, true, title);
            // prompt user
            if (updateGui.isUpdate()) {
                updateGui.toProgressView();
                // download update
                URL url = new URL("http://updates.lanxchange.com/update_master.zip");
                FileOutputStream os = new FileOutputStream(new File("update_dl.zip"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    byte tmp_buffer[] = new byte[4096];
                    // limit update size
                    InputStream is = new ByteLimitInputStream(conn.getInputStream(), fullUpdateSizeLimit);
                    int n;
                    while ((n = is.read(tmp_buffer)) > 0) {
                        os.write(tmp_buffer, 0, n);
                        os.flush();
                    }
                }
                os.close();
                // verify signature
                updateGui.setStatusToVerify();
                // extract update & signature file
                File psource = new File("update_dl.zip");
                ZipFile masterZip = new ZipFile(psource);
                ZipEntry updatecontent = masterZip.getEntry("lxc.zip");
                byte[] buffer = new byte[1024];
                BufferedInputStream masterbins = new BufferedInputStream(masterZip.getInputStream(updatecontent));
                BufferedOutputStream masterbout = new BufferedOutputStream(new FileOutputStream(new File("temp_update.zip")));
                for (int len; (len = masterbins.read(buffer)) != -1;) {
                    masterbout.write(buffer, 0, len);
                }
                masterbins.close();
                masterbout.close();
                ZipEntry signfile = masterZip.getEntry("lxc.sign");
                byte[] buffer2 = new byte[1024];
                BufferedInputStream signbins = new BufferedInputStream(masterZip.getInputStream(signfile));
                BufferedOutputStream signbout = new BufferedOutputStream(new FileOutputStream(new File("temp_update.zip.sign")));
                for (int len; (len = signbins.read(buffer2)) != -1;) {
                    signbout.write(buffer2, 0, len);
                }
                signbins.close();
                signbout.close();
                masterZip.close();
                // the check itself
                KeyFactory fact = KeyFactory.getInstance("RSA");
                InputStream ins = ClassLoader.getSystemClassLoader().getResourceAsStream("lxc_updates.pub");
                if (ins == null) {
                    // try file
                    ins = new FileInputStream(new File("lxc_updates.pub"));
                }
                ByteArrayOutputStream sigRead = new ByteArrayOutputStream();
                int bytesRead = 0;
                byte[] sigBuffer = new byte[1024];
                while ((bytesRead = ins.read(sigBuffer)) != -1) {
                    sigRead.write(sigBuffer, 0, bytesRead);
                }
                ins.close();
                X509EncodedKeySpec priKeySpec = new X509EncodedKeySpec(sigRead.toByteArray());
                PublicKey pubKey = fact.generatePublic(priKeySpec);

                Signature sign = Signature.getInstance("SHA256withRSA");
                sign.initVerify(pubKey);

                FileInputStream in = new FileInputStream("temp_update.zip");
                int bufSize = 1024;
                byte[] sbuffer = new byte[bufSize];
                int n = in.read(sbuffer, 0, bufSize);
                while (n != -1) {
                    sign.update(sbuffer, 0, n);
                    n = in.read(sbuffer, 0, bufSize);
                }
                in.close();

                FileInputStream inss = new FileInputStream(new File("temp_update.zip.sign"));
                byte[] bs = new byte[inss.available()];
                inss.read(bs);
                inss.close();
                // signature ok?
                if (sign.verify(bs) || overrideVerification) {
                    // protect against downgrade attacks
                    if (allowDowngrade || verifyVersion(new File("temp_update.zip"), gotver, forceUpdate)) {
                        updateGui.setStatusToInstall();
                        // extract update
                        File source = new File("temp_update.zip");
                        File target = new File(".");
                        byte[] buffer3 = new byte[1024];
                        ZipFile zipFile = new ZipFile(source);
                        try {
                            Enumeration<? extends ZipEntry> zipEntryEnum = zipFile.entries();

                            while (zipEntryEnum.hasMoreElements()) {
                                try {
                                    ZipEntry zipEntry = zipEntryEnum.nextElement();
                                    File file = new File(target, zipEntry.getName());
                                    if (!file.toPath().normalize().toAbsolutePath().startsWith(target.getAbsolutePath())) {
                                        // directory traversal beyond root dir
                                        System.out.println("WARNING: Skipped directory traversal in file: \"" + file.getAbsolutePath() + "\"");
                                        continue;
                                    }
                                    if (zipEntry.isDirectory()) {
                                        file.mkdirs();
                                    } else {
                                        new File(file.getParent()).mkdirs(); // create folder, if required
                                        BufferedInputStream bins = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                                        BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(file));
                                        for (int len; (len = bins.read(buffer3)) != -1; ) {
                                            bout.write(buffer3, 0, len);
                                        }
                                        bins.close();
                                        bout.close();
                                    }
                                } catch (IOException ex) {
                                    System.out.println("Update-Error: Cannot unpack file!");
                                    ex.printStackTrace();
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        } finally {
                            zipFile.close();
                        }

                        // delete tempfiles
                        new File("temp_update.zip.sign").delete();
                        new File("temp_update.zip").delete();
                        new File("update_dl.zip").delete();
                        new File("v").delete();

                        // cleanup, delete outdated files
                        for (String s : oldFiles) {
                            File f = new File(s);
                            if (f.exists()) {
                                f.delete();
                            }
                        }

                        //done
                        updateGui.setStatusToRestart();
                        updateGui.setRestartTime(5, !restartable);
                        Thread.sleep(1000);
                        updateGui.setRestartTime(4, !restartable);
                        Thread.sleep(1000);
                        updateGui.setRestartTime(3, !restartable);
                        Thread.sleep(1000);
                        updateGui.setRestartTime(2, !restartable);
                        Thread.sleep(1000);
                        updateGui.setRestartTime(1, !restartable);
                        Thread.sleep(1000);
                        updateGui.setRestartTime(0, !restartable);
                        System.exit(6);

                    } else {
                        System.out.println("ERROR: Downgrade prevented. Current version is " + LXC.versionId + ", but server claimed to send " + gotver + ". Will not update.");
                        updateGui.setStatusToError();
                        return;
                    }

                } else {
                    System.out.println("ERROR: Bad signature! File corrupted (OR MANIPULATED!!!). Will not update.");
                    updateGui.setStatusToError();
                    return;
                }
            } else {
                System.out.println("Update rejected by user");
            }
            updateGui.setVisible(false);
            updateGui.dispose();
        } else {
            System.out.println("You have the latest version");
        }

    }

    /**
     * Extracts the embedded version number from the signed update file to protect against downgrade attacks.
     * Only allows updates (returns true) iff:
     * 1. The embedded version equals the version the update server claims to distribute.
     * AND
     * 2. The embedded version is strictly greater than the version currently running.
     *
     * Note: If "forceUpdate" is true, the second check is weakened and also allows the current version to be installed again (current version equals update version)
     *
     * @param zipFile the downloaded and verified update distribution
     * @param forceUpdate whether to accept re-installations (update version number equals current version number)
     * @return true if all checks passed, false otherwise
     */
    private static boolean verifyVersion(File zipFile, int claimedVersion, boolean forceUpdate) {
        try {
            ZipFile zip = new ZipFile(zipFile);
            Scanner scanner = new Scanner(zip.getInputStream(zip.getEntry("v")), "utf8");
            int embeddedVersion = Integer.parseInt(scanner.nextLine());
            // test 1: embedded version must match version server claimed to send
            if (embeddedVersion == claimedVersion) {
                // test 2: embedded version must be greater than current version, equal is allowed if forceUpdate is true
                if (embeddedVersion > LXC.versionId || (forceUpdate && embeddedVersion == LXC.versionId)) {
                    // all checks passed
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Update version verification failed. Details:");
            e.printStackTrace();
        }
        return false;
    }


    /**
     * Utility-Class, private Constructor
     */
    private LXCUpdater() {
    }
}
