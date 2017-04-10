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
import de.tobifleig.lxc.data.VirtualFile;
import de.tobifleig.lxc.data.impl.RealFile;
import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The actual file-receiver
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class Leecher extends Transceiver {

    private final LXCLogger logger;
    /**
     * The folder to save the loaded files into.
     */
    private final File targetFolder;

    @Override
    public void run() {
       logger.info("Starting transmission... (version " + transVersion + ")");

        long startTime = System.currentTimeMillis();
        List<VirtualFile> baseFiles = new ArrayList<VirtualFile>();

        try {

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
                    // track base files
                    if (cmd == 'F') {
                        baseFiles.add(new RealFile(target));
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
                            if (read == -1) {
                                // EOF
                                throw new EOFException("Sudden EOF, aborting.");
                            }
                            fileout.write(buffer, 0, read);
                            size -= read;
                            transferredBytes += read;
                            updateProgress();
                        }
                        fileout.flush();
                        fileout.close();
                        target.setLastModified(date);
                    } catch (FileNotFoundException ex) {
                        // cannot create file, possible reasons are:
                        // - name invalid on this file system
                        // - trying to create file, but already exists as dir
                        // - no write permission
                        //
                        // abort
                        logger.error("Cannot create file \"" + target.getAbsolutePath() + "\", aborting transfer", ex);
                        listener.finished(false, false, false, target.getAbsolutePath());
                        break;
                    } catch (IOException ex) {
                        if (!abort) {
                            logger.error("Unexpected IOException", ex);
                        }
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
                    // track base dirs
                    if (cmd == 'D') {
                        baseFiles.add(new RealFile(target));
                    }
                } else if (cmd == 'e') {
                    // done
                    logger.info("Finished in " + (System.currentTimeMillis() - startTime) + "ms, speed was " + String.format(Locale.US, "%.2f", 1.0 * totalBytes / (System.currentTimeMillis() - startTime)) + "kb/s");
                    logger.info("Done receiving.");
                    // set base files
                    file.setBaseFiles(baseFiles);
                    listener.finished(true, false, false, null);
                    break;
                } else if (cmd == 's') {
                    // error, client no longer has the file
                    logger.warn("Remote reports missing file, aborting transfer.");
                    listener.finished(false, false, true, null);
                    break;
                } else {
                    logger.warn("Unused command: " + cmd);
                }
            }

        } catch (IOException ex) {
            if (!abort) {
                logger.error("Unexpected IOException(2)", ex);
            }
            listener.finished(false, abort,false, null);
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
                // ignore
            }
            try {
                out.close();
            } catch (Exception ex) {
                // ignore
            }
            try {
                socket.close();
            } catch (Exception ex) {
                // ignore
            }
            // force immediate release of file handles
            System.gc();
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
     * @param socket the socket
     * @param output the OutputStream, connected and ready
     * @param input the InputStream, connected and ready
     * @param transFile the {@link LXCFile} that is to be transferred
     * @param targetFolder the folder to save the files into
     * @param transVersion version of the transfer protocol {@link Transceiver}
     */
    public Leecher(Socket socket, ObjectInputStream input, ObjectOutputStream output, LXCFile transFile, File targetFolder, int transVersion) {
        this.logger = LXCLogBackend.getLogger("leech-" + transFile.id);
        this.socket = socket;
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
        thread.setName("leecher_" + file.id);
        thread.start();
    }

    @Override
    public void abort() {
        // Just kill it
        logger.info("Aborting download upon user-request. Exceptions may occur!");
        abort = true;
        try {
            in.close();
        } catch (Exception ex) {
            // ignore
        }
    }
}
