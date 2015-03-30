/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.data;

import de.tobifleig.lxc.data.impl.RealFile;
import de.tobifleig.lxc.net.LXCInstance;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a "file" offered by a certain LXCInstance.
 * A LXCFile can represent a single file, a single folder or multiple files and folders mixed.
 * <p/>
 * The name "LXCFile" does not describe the purpose of this class very well, it is kept to avoid breaking compatibility with older clients.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class LXCFile implements Serializable {

    static final long serialVersionUID = 2812501218020544733L;
    /**
     * The user wants to share a single file.
     */
    public static final int TYPE_FILE = 1;
    /**
     * The user wants to share a single folder.
     */
    public static final int TYPE_FOLDER = 2;
    /**
     * The user wants to share multiple files (and/or folders).
     */
    public static final int TYPE_MULTI = 3;
    /**
     * Identifier, chosen at random, required for equals.
     */
    public final long id;
    /**
     * If true, the file has been downloaded successfully.
     * Only valid for remote files.
     */
    private transient boolean isAvailable = false;
    /**
     * The LXCInstance that created/offers this file.
     * This is transient, because each running instance of LXC has its own representations of remote instances.
     */
    private transient LXCInstance instance;
    /**
     * If true, this file is locked and cannot be downloaded.
     * This is used to prevent further attempts to download a file while one is still connecting.
     */
    private transient boolean locked = false;
    /**
     * The list of files that the user wants to share.
     * Including directorys, but not the files within them.
     * Can be shared with other classes, because it is unmodifiable.
     */
    private transient List<VirtualFile> files;
    /**
     * The jobs currently assigned to this file.
     */
    private transient List<LXCJob> jobs = new ArrayList<LXCJob>();
    /**
     * The textual representation that is displayed.
     */
    private final String shownName;
    /**
     * The total size in bytes.
     * Is calculated by the constructor.
     * Includes all files in subfolders.
     * Directorys count as zero bytes.
     */
    private long fileSize;
    /**
     * The type of this file.
     * One of TYPE_FILE, TYPE_FOLDER or TYPE_MULTI
     */
    private final int type;
    /**
     * Transport protocol version number.
     */
    private int lxcTransVersion = 1;
    /**
     * The maximum version number supported by this client.
     */
    private final transient int lxcTransVersionMax = 1;
    /**
     * Number of successful transfers.
     */
    private transient int numberOfTransfers = 0;

    /**
     * The new constructor, creates a LXCFile with the given parameters.
     * May take a long time to calculate the size if many files are involved.
     *
     * @param fileList a list of VirtualFiles
     * @param shownN   the name that is shown
     */
    public LXCFile(List<VirtualFile> fileList, String shownN) {
        id = Double.doubleToLongBits(Math.random());
        // create a copy:
        this.files = new ArrayList<VirtualFile>(fileList);
        // Create name
        if (fileList.size() > 1) {
            shownName = shownN.concat(" + " + (fileList.size() - 1));
        } else {
            shownName = shownN;
        }

        // set type
        if (files.size() > 1) {
            type = LXCFile.TYPE_MULTI;
        } else if (files.get(0).isDirectory()) {
            type = LXCFile.TYPE_FOLDER;
        } else {
            type = LXCFile.TYPE_FILE;
        }

        // this constructor only creates local files
        instance = LXCInstance.local;

        // calculate size
        for (VirtualFile file : fileList) {
            fileSize += calcSize(file);
        }
    }

    /**
     * Adds a job to this LXCFile.
     *
     * @param job the new job
     */
    public void addJob(LXCJob job) {
        if (jobs == null) {
            jobs = new ArrayList<LXCJob>();
        }
        jobs.add(job);
    }

    /**
     * Removes a certain job from this LXCFile.
     *
     * @param job     the job to be removed
     * @param success true if transfer was successful
     */
    public void removeJob(LXCJob job, boolean success) {
        jobs.remove(job);
        if (success) {
            numberOfTransfers++;
        }
    }

    /**
     * Returns a List containing all LXCJobs added to this LXCFile.
     *
     * @return a List with all LXCJobs
     */
    public List<LXCJob> getJobs() {
        if (jobs == null) {
            jobs = new ArrayList<LXCJob>();
        }
        return jobs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LXCFile lxcFile = (LXCFile) o;

        return id == lxcFile.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    /**
     * Sets the available-flag.
     *
     * @param available new state of the flag
     */
    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    /**
     * Computes the size of a given VirtualFile.
     * Recursively scans directorys.
     * Only counts files. Directorys count as zero bytes.
     *
     * @param startFile a file to scan
     * @return the size of the file/dir+subs in bytes
     */
    private long calcSize(VirtualFile startFile) {
        long size = 0;
        LinkedList<VirtualFile> queue = new LinkedList<VirtualFile>();
        // init queue
        queue.addLast(startFile);
        // add everything up
        while (!queue.isEmpty()) {
            VirtualFile file = queue.pollFirst();
            if (file.isDirectory()) {
                queue.addAll(file.children());
            } else {
                size += file.size();
            }
        }
        return size;
    }

    /**
     * Returns the available-flag.
     *
     * @return the available-flag
     */
    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Returns the shown name.
     *
     * @return the shown name
     */
    public String getShownName() {
        return shownName;
    }

    /**
     * Returns the type.
     *
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the size of this file.
     *
     * @return the size of this file
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Returns the LXCInstance that created/offers this file.
     *
     * @return the instance
     */
    public LXCInstance getInstance() {
        return instance;
    }

    /**
     * Sets the LXCInstance that offers this file.
     *
     * @param instance the instance to set
     */
    public void setInstance(LXCInstance instance) {
        this.instance = instance;
    }

    /**
     * Returns the locked-flag.
     *
     * @return the locked-flag
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Sets the locked-flag.
     *
     * @param locked the new state
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * Returns the list of files the user wants to share.
     *
     * @return the file-list
     */
    public List<VirtualFile> getFiles() {
        return files;
    }

    /**
     * Sets the list of base files.
     * This is required after downloads.
     *
     * @param baseFiles
     */
    public void setBaseFiles(List<VirtualFile> baseFiles) {
        this.files = baseFiles;
    }

    /**
     * Returns the current set protocol version.
     *
     * @return the lxcTransVersion
     */
    public int getLxcTransVersion() {
        return lxcTransVersion;
    }

    /**
     * Limits the protocol version to the maximum supported by this local client.
     */
    public void limitTransVersion() {
        if (lxcTransVersion > lxcTransVersionMax) {
            lxcTransVersion = lxcTransVersionMax;
        }
    }

    /**
     * Returns true, if this file is offered by this local instance.
     * Returns false, if received from a remote instance.
     *
     * @return true, if local
     */
    public boolean isLocal() {
        return instance.id == LXCInstance.local.id;
    }

    /**
     * Returns the number of (completed) transfers.
     *
     * @return number of transfers
     */
    public int getNumberOfTransfers() {
        return numberOfTransfers;
    }

    /**
     * Converts a list of real files to a List of VirtualFiles.
     * Determines the common toplevel node, if any, and creates file accordingly.
     *
     * @param files a list of real (java.io.File) files
     * @return a list of VirtualFiles
     */
    public static List<VirtualFile> convertToVirtual(List<File> files) {
        // Search toplevel node(s):
        LinkedList<File> topLevelNodes = new LinkedList<File>();
        outer:
        for (File file : files) {
            // Check if this file is below any toplevel node
            for (File topLevelNode : topLevelNodes) {
                if (file.getAbsolutePath().startsWith(topLevelNode.getAbsolutePath())) {
                    // below - this means file is not a toplevel node
                    continue outer;
                }
            }
            // This file is a toplevel node - check if this makes other toplevel nodes obsolete
            for (int i = 0; i < topLevelNodes.size(); i++) {
                File topLevelNode = topLevelNodes.get(i);
                if (topLevelNode.getAbsolutePath().startsWith(file.getAbsolutePath())) {
                    // the new file is a parent of this toplevel node
                    topLevelNodes.remove(i);
                }
            }
            // Insert the new toplevel node
            topLevelNodes.add(file);
        }

        ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();
        // Each of these files now either has a parent in topLevelNodes or is a toplevel node itself.
        for (File file : files) {
            if (topLevelNodes.contains(file)) {
                // toplevel
                result.add(new RealFile(file));
            } else {
                // search topLevelNode
                for (File topLevelNode : topLevelNodes) {
                    if (file.getAbsolutePath().startsWith(topLevelNode.getAbsolutePath())) {
                        // found toplevel node
                        result.add(new RealFile(file, topLevelNode));
                        break;
                    }
                }
                // this should never be reached
                System.out.println("WARNING: Cannot find TopLevel node for " + file.getAbsolutePath());
                // just assume this is toplevel as well
                result.add(new RealFile(file));
            }
        }
        return result;
    }

    /**
     * Computes a human-readable String which approximates the value passed to this method.
     *
     * @param fSize the fileSize in bytes
     * @return a human-readable String containing the size (like 12,3 MiB)
     */
    public static String getFormattedSize(long fSize) {
        DecimalFormat form = new DecimalFormat("0.0");
        if (fSize < 1024) {
            return fSize + " B";
        } else if (fSize < 1048576) {
            String size = form.format(fSize / 1024d);
            return size + " KiB";
        } else if (fSize < 1073741824) {
            String size = form.format(fSize / 1048576d);
            return size + " MiB";
        } else if (fSize < 1099511627776l) {
            String size = form.format(fSize / 1073741824d);
            return size + " GiB";
        } else if (fSize < 1125899906842625l) {
            String size = form.format(fSize / 1099511627776d);
            return size + " TiB";
        } else {
            return "unknown";
        }
    }
}
