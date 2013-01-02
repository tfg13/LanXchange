/*
 * Copyright 2009, 2010, 2011, 2012, 2013 Tobias Fleig (tobifleig gmail com)
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

import de.tobifleig.lxc.net.LXCInstance;
import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents a "file" offered by a certain LXCInstance.
 * A LXCFile can represent a single file, a single folder or multiple files and folders mixed.
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
     * Path to the parent-folder of all selected files.
     */
    private transient String sourceFile;
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
     * The list of files that the user want to share.
     * Including directorys, but not the files within them.
     */
    private transient List<File> files;
    /**
     * The jobs currently assigned to this file.
     */
    private transient List<LXCJob> jobs;
    /**
     * The textual representation that is displayed.
     */
    private String shownName;
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
    private int type;
    /**
     * A list containing ALL files that will be transfered on downloads.
     * This list includes files in sub-directorys.
     */
    private LinkedList<String> content;
    /**
     * Transport protocol version number.
     */
    private int lxcTransVersion = 1;
    /**
     * The maximum version number supported by this client.
     */
    private final transient int lxcTransVersionMax = 1;

    /**
     * Creates a new LXCFile with the given parameters.
     * Recursively scans all directorys in "files" and calculates the total size.
     * May take a long time.
     *
     * @param files a List containing all files and folders the user selected to share
     * @param shownN the name that is shown
     */
    public LXCFile(List<File> files, String shownN) {
	this.files = files;
	// Create name
	shownName = shownN;
	if (files.size() > 1) {
	    shownName = shownName.concat(" + " + (files.size() - 1));
	}

	// set type
	if (files.size() > 1) {
	    type = LXCFile.TYPE_MULTI;
	} else if (files.get(0).isDirectory()) {
	    type = LXCFile.TYPE_FOLDER;
	} else {
	    type = LXCFile.TYPE_FILE;
	}

	// misc
	sourceFile = files.get(0).getParent();
	content = new LinkedList<String>();

	// this constructor only creates local files
	instance = LXCInstance.local;

	// calculate size
	for (File file : files) {
	    fileSize += getSize(file);
	}

    }

    /**
     * Creates the internal file-list.
     * Will very likely go away in the future.
     *
     * @deprecated Allows external access to internal variables
     */
    public void createFiles() {
	files = new ArrayList<File>();
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
     * @param job the job to be removed
     */
    public void removeJob(LXCJob job) {
	jobs.remove(job);
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

    /**
     * Cuts the given absolute path down to a relative one that can be sent to the other side.
     *
     * @param absolute the absolute path
     * @return a relative path
     */
    private String cutRelativePath(String absolute) {
	String res = absolute.substring(sourceFile.length() + 1);
	return res;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj instanceof LXCFile) {
	    LXCFile test = (LXCFile) obj;
	    return (shownName.equals(test.shownName) && fileSize == test.fileSize && content.equals(test.content));
	} else {
	    return false;
	}
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 71 * hash + (this.shownName != null ? this.shownName.hashCode() : 0);
	hash = 71 * hash + (int) (this.fileSize ^ (this.fileSize >>> 32));
	hash = 71 * hash + (this.content != null ? this.content.hashCode() : 0);
	return hash;
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
     * Computes the size of a given File or directory.
     * Recursively scans directorys.
     * Only counts files. Directorys count as zero bytes.
     *
     * @param realFile a file to scan
     * @return the size of the file/dir in bytes.
     */
    private long getSize(File realFile) {

	long size = 0;
	File[] filelist = realFile.listFiles();
	ArrayList<File> filealist = new ArrayList<File>();
	if (filelist != null) {
	    filealist.addAll(Arrays.asList(filelist));
	} else {
	    filealist.add(realFile);
	}
	for (int i = 0; i < filealist.size(); i++) {
	    File file = filealist.get(i);
	    if (file.isDirectory()) {
		content.add(cutRelativePath(file.getAbsolutePath()) + "/");
		File[] contents = file.listFiles();
		if (contents == null) {
		    // content of directory must not be read
		    System.out.println("cannot read " + file.getAbsolutePath() + ", ignoring");
		    continue;
		}
		filealist.addAll(Arrays.asList(contents));
		continue;
	    } else {
		content.add(cutRelativePath(file.getAbsolutePath()));
	    }
	    size += file.length();
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
     * Returns the source file.
     *
     * @return the source file
     */
    public String getSourceFile() {
	return sourceFile;
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
    public List<File> getFiles() {
	return files;
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
}
