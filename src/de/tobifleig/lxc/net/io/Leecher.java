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
package de.tobifleig.lxc.net.io;

import de.tobifleig.lxc.data.LXCFile;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The actual file-receiver
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class Leecher extends Transceiver {

    /**
     * The folder to save the loaded files into.
     */
    private File targetFolder;

    @Override
    public void run() {
        System.out.println("Leecher: Starting transmission... (version " + transVersion + ")");

        long startTime = System.currentTimeMillis();

        try {

            // Create list for base-files (will likely go away in future releases)
            file.createFiles();

            while (true) {
                byte cmd = in.readByte();

                if (cmd == 'f' || cmd == 'F') {
                    // next file
                    // read date if version >= 1
                    long date = file.getLxcTransVersion() >= 1 ? in.readLong() : System.currentTimeMillis();
                    // read size
                    long size = in.readLong();
                    // read path
                    String path = in.readUTF();
                    // convert path
                    path = path.replace("/", File.separator);
                    path = path.replace("\\", File.separator);
                    // create file / parent folders
                    File target = new File(targetFolder, path);
                    target.getParentFile().mkdirs();
                    // remember base-files (deprecated)
                    if (cmd == 'F') {
                        file.getFiles().add(target);
                    }
                    // transfer file content (real data)
                    BufferedOutputStream fileout;
                    byte[] buffer = new byte[4096];
                    try {
                        fileout = new BufferedOutputStream(new FileOutputStream(target), 8388608);
                        while (size > 0) {
                            int read;
                            if (size >= buffer.length) {
                                read = in.read(buffer);
                            } else {
                                read = in.read(buffer, 0, (int) size);
                            }
                            fileout.write(buffer, 0, read);
                            size -= read;
                            transferedBytes += read;
                            listener.progress();
                        }
                        fileout.flush();
                        fileout.close();
                        target.setLastModified(date);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else if (cmd == 'd' || cmd == 'D') {
                    // folder
                    String path = in.readUTF();
                    // convert path
                    path = path.replace("/", File.separator);
                    path = path.replace("\\", File.separator);
                    // create folder
                    File target = new File(targetFolder, path);
                    target.mkdirs();
                    // add to base list, deprecated
                    if (cmd == 'D') {
                        file.getFiles().add(target);
                    }
                } else if (cmd == 'e') {
                    // done
                    System.out.println("Finished in " + (System.currentTimeMillis() - startTime) + "ms, speed was " + (1.0 * totalBytes / (System.currentTimeMillis() - startTime)) + "kb/s");
                    System.out.println("Leecher: Done receiving.");
                    listener.finished(true, false);
                    break;
                } else if (cmd == 's') {
                    // error, client no longer has the file
                    System.out.println("Error: Remote reports missing file, aborting transfer.");
                    listener.finished(false, true);
                    break;
                } else {
                    System.out.println("Leecher: Unused command: " + cmd);
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
            listener.finished(false, false);
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
            }
            try {
                out.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Creates a new Leecher with the given parameters.
     * It is assumed the Streams as fully established
     * This means in particular:
     * - Both Streams are connected to a remote Seeder
     * - The remote side has been preconfigured and can start receiving files immediately
     * Please note:
     * This implementation uses two streams (intput+output) for Seeder and Leechers.
     * Obviously only one stream should be required (output for seed, input for leech)
     * Removing the second stream would break compatibilty with previous versions, therefore it is kept.
     * However, it should not be used and may be removed in the future.
     *
     * @param output the OutputStream, connected and ready
     * @param input the InputStream, connected and ready
     * @param transFile the {@link LXCFile} that is to be transferred
     * @param targetFolder the folder to save the files into
     * @param transVersion version of the transfer protocol {@link LXCTransceiver}
     */
    public Leecher(ObjectInputStream input, ObjectOutputStream output, LXCFile transFile, File targetFolder, int transVersion) {
        this.file = transFile;
        this.out = output;
        this.in = input;
        this.targetFolder = targetFolder;
        this.transVersion = transVersion;

        this.totalBytes = file.getFileSize();
    }

    @Override
    public void start() {
        Thread thread = new Thread(this);
        thread.setName("leecher_" + file.getShownName());
        thread.start();
    }

    @Override
    public void abort() {
        // Just kill it
        System.out.println("Leecher: Aborting download upon user-request. Exceptions will occur!");
        try {
            in.close();
        } catch (Exception ex) {
        }
    }
}
