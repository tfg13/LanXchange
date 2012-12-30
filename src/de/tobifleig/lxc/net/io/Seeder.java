/*
 * Copyright 2009, 2010, 2011, 2012 Tobias Fleig (tobifleig gmail com)
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
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The actual file-sender.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class Seeder extends Transceiver {

    @Override
    public void run() {
        System.out.println("Seeder: Starting transmission... (version " + transVersion + ")");
	long startTime = System.currentTimeMillis();

        try {
            List<File> files = file.getFiles();
            String parentFile = files.get(0).getParent();
            ArrayList<File> allList = new ArrayList<File>();
            allList.addAll(files);
            for (int i = 0; i < allList.size(); i++) {
                File currentFile = allList.get(i);
                if (currentFile.isDirectory()) {
                    // add files recursively
                    allList.addAll(Arrays.asList(currentFile.listFiles()));
                    // send directory itself
                    out.writeByte(files.contains(currentFile) ? 'D' : 'd');
                    out.writeUTF(absToRelPath(currentFile.getAbsolutePath(), parentFile));
                } else {
                    // transfer a file
                    if (currentFile.canRead()) {
                        // inform client
                        out.writeByte(files.contains(currentFile) ? 'F' : 'f');
                        // send date, if version >= 1
                        if (transVersion >= 1) {
                            out.writeLong(currentFile.lastModified());
                        }
                        // send size
                        out.writeLong(currentFile.length());
                        // send path
                        out.writeUTF(absToRelPath(currentFile.getAbsolutePath(), parentFile));
                        out.flush();
                        // send content (real data)
                        BufferedInputStream filein = null;
                        try {
                            filein = new BufferedInputStream(new FileInputStream(currentFile), 8388608);
                            byte[] buffer = new byte[4096];
                            int gotbytes;
                            while ((gotbytes = filein.read(buffer)) > 0) {
                                transferedBytes += gotbytes;
                                listener.progress();
                                out.write(buffer, 0, gotbytes);
                                out.flush();
                            }
                            // done
                            out.flush();
                        } catch (FileNotFoundException ex) {
                            System.out.println("Seeder: Unexpected Error! This should not happen...");
                        } finally {
                            try {
                                filein.close();
                            } catch (Exception ex) {
                            }
                        }
                    }
                }
            }
            // transfer complete
            out.writeByte('e');
            out.flush();
	    System.out.println("Finished in " + (System.currentTimeMillis() - startTime) + "ms, speed was " + (1.0 * totalBytes / (System.currentTimeMillis() - startTime)) + "kb/s");
            listener.finished(true);
            System.out.println("Seeder: Done seeding.");
        } catch (IOException ex) {
            listener.finished(false);
            System.out.println("Seeder: Lost connection, aborting.");
            ex.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
                System.gc(); // Closes filehandlers etc
            } catch (Exception ex) {
                // Who cares..
            }
        }

    }

    /**
     * Creates a new Seeder with the given parameters.
     * It is assumed the Streams as fully established
     * This means in particular:
     *  - Both Streams are connected to a remote Leecher
     *  - The remote side has been preconfigured and can start receiving files immediately
     * Please note:
     * This implementation uses two streams (intput+output) for Seeder and Leechers.
     * Obviously only one stream should be required (output for seed, input for leech)
     * Removing the second stream would break compatibilty with previous versions, therefore it is kept.
     * However, it should not be used and may be removed in the future.
     * @param output the OutputStream, connected and ready
     * @param input the InputStream, connected and ready
     * @param transFile the {@link LXCFile} that is to be transferred
     * @param transVersion version of the transfer protocol {@link LXCTransceiver}
     * @see LXCTransceiver
     */
    public Seeder(ObjectOutputStream output, ObjectInputStream input, LXCFile transFile, int transVersion) {
        this.file = transFile;
        this.out = output;
        this.in = input;
        this.transVersion = transVersion;
        this.totalBytes = file.getFileSize();
    }

    /**
     * Starts this Leecher.
     * Creates a new thread, returns immediately.
     */
    @Override
    public void start() {
        Thread thread = new Thread(this);
        thread.setName("seeder_" + file.getShownName());
        thread.start();
    }

    /**
     * Turns the given absolute path to be relative to parentPath.
     * This this step is essential, Leecher require paths to be relative.
     * @param absolute the local, absolute path of the current file
     * @param parentPath the local, absolte path to the parent directory of the particular LXCFile
     * @return a path relative to parentPath
     */
    private String absToRelPath(String absolute, String parentPath) {
        String res = absolute.substring(parentPath.length() + 1);
        return res;
    }

    @Override
    public void abort() {
	// Just kill it
        System.out.println("Leecher: Aborting upload upon user-request. Exceptions will occur!");
        try {
            in.close();
        } catch (Exception ex) {
        }
    }
}