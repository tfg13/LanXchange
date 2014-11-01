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
package de.tobifleig.lxc.plaf.impl.swing;

import de.tobifleig.lxc.plaf.impl.ui.UpdateDialog;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.plaf.impl.ui.UserInterface;
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
 * This system never installs anything without comfirmation. For security reasons, only signed updates are accepted.
 *
 * At default settings, this class will check for updates on every start.
 * "Checking for updates" means reading the version file from updates.lanxchange.com and comparing it to LXC.versionId.
 * If a newer version is available, the user will be asked, if he/she wants to download.
 * If the answer is yes, the update is downloaded and installed and LXC is restarted automatically.
 * This usually happens withing seconds.
 * "Download and install" updates means downloading update_master.zip from updates.lanxchange.com and extracting it.
 * It contains to files: Another zipfile containing a full copy of LXC and a signature file.
 * Before proceeding, the (X509, SHA256withRSA) signature is checked to match the pubkey contained in lxc_updates.pub
 * If correct, the zipfile is extracted into the main folder, overriding all files.
 * Afterwards outdated files contained in oldFiles (see below) are deleted.
 *
 * The signature is created by de.tobifleig.lxc.util.Signer, but for obvious reasons, the private key is not distributed.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public final class LXCUpdater {

    /**
     * Files, that should no longer be found in the users installation and therefore must be deleted.
     */
    private static final String[] oldFiles = new String[]{"lxc_debug.exe", "LXC.ico", "Ubuntu-R.ttf", "lxc_updates.pub"};

    /**
     * Checks for updates, promts the user and installs them.
     *
     * @param gui the gui, required to display the dialog
     * @param forceUpdate, if true, updates are installed even if there is no new version (however, the user is promted)
     * @param overrideVerification if true, the signature is not checked (dangerous!)
     * @param restartable if true, whoever started LXC is aware of this updatesystem and wants to be notifiyed when LXC should be restarted rather than regularely terminated (this is done by returning exit code 6 rather than 0)
     * @throws Exception may throw a bunch of exceptions, this class requires, working internet, github, signature checks etc.
     */
    public static void checkAndPerformUpdate(UserInterface userInterface, boolean forceUpdate, boolean overrideVerification, boolean restartable) throws Exception {
        if (forceUpdate) {
            System.out.println("Info: Forcing update...");
        }

        // Contact update server, download version file
        Scanner scanner = new Scanner(new URL("http://updates.lanxchange.com/v").openStream(), "utf8");
        int gotver = Integer.parseInt(scanner.nextLine());
        String title = scanner.nextLine();
        scanner.close();
        // compare version number
        if (gotver > LXC.versionId || forceUpdate) {
            System.out.println("Newer Version available!");
            UpdateDialog updateDialog = userInterface.getUpdateDialog();
            updateDialog.setTitle(title);
            // prompt user
            if (updateDialog.isUpdate()) {
                updateDialog.toProgressView();
                // download update
                URL url = new URL("http://updates.lanxchange.com/update_master.zip");
                FileOutputStream os = new FileOutputStream(new File("update_dl.zip"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    byte tmp_buffer[] = new byte[4096];
                    InputStream is = conn.getInputStream();
                    int n;
                    while ((n = is.read(tmp_buffer)) > 0) {
                        os.write(tmp_buffer, 0, n);
                        os.flush();
                    }
                }
                os.close();
                // verify signature
                updateDialog.setStatusToVerify();
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
                    updateDialog.setStatusToInstall();
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
                                if (zipEntry.isDirectory()) {
                                    file.mkdirs();
                                } else {
                                    new File(file.getParent()).mkdirs(); // create folder, if required
                                    BufferedInputStream bins = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                                    BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(file));
                                    for (int len; (len = bins.read(buffer3)) != -1;) {
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

                    // cleanup, delete outdated files
                    for (String s : oldFiles) {
                        File f = new File(s);
                        if (f.exists()) {
                            f.delete();
                        }
                    }

                    //done
                    updateDialog.setStatusToRestart();
                    updateDialog.setRestartTime(5, !restartable);
                    Thread.sleep(1000);
                    updateDialog.setRestartTime(4, !restartable);
                    Thread.sleep(1000);
                    updateDialog.setRestartTime(3, !restartable);
                    Thread.sleep(1000);
                    updateDialog.setRestartTime(2, !restartable);
                    Thread.sleep(1000);
                    updateDialog.setRestartTime(1, !restartable);
                    Thread.sleep(1000);
                    updateDialog.setRestartTime(0, !restartable);
                    System.exit(6);

                } else {
                    System.out.println("ERROR: Bad signature! File corrupted (OR MANIPULATED!!!). Will not update!");
                    updateDialog.setStatusToError();
                    return;
                }
            } else {
                System.out.println("Update rejected by user");
            }
            updateDialog.setVisible(false);
            updateDialog.dispose();
        } else {
            System.out.println("You have the latest version");
        }

    }

    /**
     * Utility-Class, private Constructor
     */
    private LXCUpdater() {
    }
}
