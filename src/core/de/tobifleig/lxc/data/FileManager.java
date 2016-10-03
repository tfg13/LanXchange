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

import de.tobifleig.lxc.net.LXCInstance;
import de.tobifleig.lxc.net.TransFileList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages own & remote LXCFiles
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class FileManager {

    /**
     * Contains all LXCFiles.
     */
    private final LinkedList<LXCFile> files;
    /**
     * Stores the latest List of available Files for each instance.
     */
    private final HashMap<LXCInstance, List<LXCFile>> recentFileLists;

    /**
     * Creates a new FileManager.
     */
    public FileManager() {
        files = new LinkedList<LXCFile>();
        recentFileLists = new HashMap<LXCInstance, List<LXCFile>>();
    }

    /**
     * Computes incoming TransFileLists.
     *
     * @param receivedList the received list
     * @param sender the origin of this list
     */
    public FileListChangeSet computeFileList(TransFileList receivedList, LXCInstance sender) {
        FileListChangeSet changeSet = new FileListChangeSet();
        List<LXCFile> addList = receivedList.getAll();
        recentFileLists.put(sender, new ArrayList<LXCFile>(addList)); // store a copy!
        Iterator<LXCFile> iter = files.iterator();
        // Remove all files no longer offered by this instance
        int remoteRemoveIndex = -1;//need old indices for gui
        while (iter.hasNext()) {
            LXCFile file = iter.next();
            if (!file.isLocal()) {
                remoteRemoveIndex++;
            }
            if (sender.equals(file.getInstance())) {
                if (!addList.contains(file)) {
                    // no longer offered, remove it, if not download{ing,ed}
                    if (file.getJobs().isEmpty() && !file.isAvailable()) {
                        iter.remove();
                        changeSet.pushRemovedFile(file, remoteRemoveIndex);
                    } else {
                        // keep, ignore in next step:
                        addList.remove(file);
                    }
                } else {
                    // already known, ignore in next step
                    addList.remove(file);
                }
            }
        }

        receivedList.setInstance(sender);
        receivedList.limitTransVersions();
        for (int i = 0; i < addList.size(); i++) {
            changeSet.pushAddedFile(addList.get(i), files.size() + i);
        }
        files.addAll(addList);
        return changeSet;
    }

    /**
     * Removes all LXCFiles offered by the given LXCInstance
     *
     * @param instance the instance which files should be no longer available
     */
    public FileListChangeSet instanceRemoved(LXCInstance instance) {
        FileListChangeSet removals = new FileListChangeSet();
        recentFileLists.remove(instance);
        Iterator<LXCFile> iter = files.iterator();
        int remoteRemoveIndex = -1;
        while (iter.hasNext()) {
            LXCFile file = iter.next();
            if (!file.isLocal()) {
                remoteRemoveIndex++;
            }
            if (file.getInstance().equals(instance)) {
                // only delete if not download{ing,ed}
                if (file.getJobs().isEmpty() && !file.isAvailable()) {
                    iter.remove();
                    removals.pushRemovedFile(file, remoteRemoveIndex);
                }
            }
        }
        return removals;
    }

    /**
     * Returns the local representation for a LXCFile sent by a remote instance.
     * This step is required to upload a file, because only the local representation contains the paths to the source files.
     *
     * @param remoteRepresentation the remote representation
     * @return the local representation, or null if file not available
     */
    public LXCFile localRepresentation(LXCFile remoteRepresentation) {
        int index = files.indexOf(remoteRepresentation);
        if (index != -1) {
            return files.get(index);
        }
        return null;
    }

    /**
     * Creates an up-to-date TransFileList containing all LXCFiles offered by this instance.
     *
     * @return a TransFileList containing all LXCFiles
     */
    public TransFileList getTransFileList() {
        ArrayList<LXCFile> offeredFiles = new ArrayList<LXCFile>();
        for (LXCFile file : files) {
            if (file.isLocal()) {
                offeredFiles.add(file);
            }
        }
        return new TransFileList(offeredFiles);
    }

    /**
     * Returns a list of all known Files.
     * This list is backed by the internal list, but not modifiable.
     *
     * @see java.util.Collections
     * @return a list of all known files, backed but unmodifiable
     */
    public List<LXCFile> getList() {
        return Collections.unmodifiableList(files);
    }

    /**
     * Adds a file offered by the local instance.
     *
     * @param newfile the new file
     * @return index of new file
     */
    public int addLocal(LXCFile newfile) {
        // do not allow duplicates
        for (LXCFile file : files) {
            if (file.equals(newfile)) {
                return -1;
            }
        }
        files.add(newfile);
        return files.size() - 1;
    }

    /**
     * Removes a file offered by the local instance.
     *
     * @param file a file offered by the local instance
     */
    public void removeLocal(LXCFile file) {
        files.remove(file);
    }

    /**
     * Returns true, if there are any transfers running.
     *
     * @return true, if still transferring
     */
    public boolean transferRunning() {
        for (LXCFile file : files) {
            if (!file.getJobs().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resets a available file.
     * Resetting a file reverts the "downloaded"-flag.
     * If no longer available, the file will disappear.
     * Otherwise, it can be redownloaded.
     *
     * @param file the available, non-local file to reset
     */
    public void resetAvailableFile(LXCFile file) {
        if (file.isAvailable() && !file.isLocal() && !file.isLocked()) {
            file.setAvailable(false);
            if (!recentFileLists.containsKey(file.getInstance()) || !recentFileLists.get(file.getInstance()).contains(file)) {
                files.remove(file);
            }
        } else {
            throw new IllegalArgumentException("Cannot reset given file! (debug: "
                    + file.getShownName() + " " + file.isAvailable() + " "
                    + file.isLocal() + " " + file.isLocked());
        }
    }

    /**
     * Generates a new file name that is currently unused.
     * Collisions are resolved by adding a number after the base name
     * @param base base name, like "image"
     * @param extension extension, without the dot, like "png"
     * @return (probably) unique name like "image05.png"
     */
    public String generateUniqueFileName(String base, String extension) {
        int counter = 0;
        outer:
        while (true) {
            counter++;
            String proposal = base + counter + "." + extension;
            for (LXCFile file : files) {
                if (file.getShownName().equals(proposal)) {
                    continue outer;
                }
            }
            // no such file
            return proposal;
        }
    }
}
