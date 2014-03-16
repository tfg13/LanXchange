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
package de.tobifleig.lxc.data;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * A virtual file.
 * Virtual files will be stored as real files on downloads, but may not exist as files on upload.
 * This enables LXC to share everything that has a predictable size and offers an InputStream for reading.
 *
 * @author Tobias Fleig <tobifleig@googlemail.com>
 */
public abstract class VirtualFile {

    /**
     * The full name of this virtual file.
     */
    private final String name;

    /**
     * The full transfer path for this virtual file.
     * Transfer paths are full paths from the common parent of all transfered files.
     * For directorys: Must *not* end with the path seperator ('/')
     */
    private final String transferPath;

    /**
     * Creates a new top-level virtual file.
     * Top-level files have no parents.
     *
     * @param name the full file name
     */
    public VirtualFile(String name) {
        this.name = name;
        transferPath = name;
    }

    /**
     * Creates a new virtual file.
     *
     * @param name the full file name
     * @param transferPath the path from the parent to this file (including the name of this file)
     */
    public VirtualFile(String name, String transferPath) {
        this.name = name;
        this.transferPath = transferPath;
    }

    /**
     * Returns the name of this virtual file.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the transfer path of this virtual file.
     *
     * @return the transfer path
     */
    public String getTransferPath() {
        return transferPath;
    }

    /**
     * Weather is VirtualFile represents a directory.
     * If true, children() returns a list of all sub-elements.
     * If false, size() and getInputStream() are defined.
     *
     * @return true, if this VirtualFile is a directory
     */
    public abstract boolean isDirectory();

    /**
     * Returns a list of all sub-elements of this directory.
     * Must not be called if isDirectory returns false.
     *
     * @return a list of all sub-elements
     */
    public abstract List<VirtualFile> children();

    /**
     * Returns the size of this file in bytes.
     * Must not be called if isDirectory returns true.
     *
     * @return the size in bytes
     */
    public abstract long size();

    /**
     * Returns the point in time when this file was last modified.
     * Must be implemented, if this is not available or makes no sense one should use System.currentTimeMillis();
     *
     * @return the point in time where this file was last modified
     */
    public abstract long lastModified();

    /**
     * Opens and returns an InputStream for the contents of this file.
     * Must not be called if isDirectory returns true.
     *
     * @return a new InputStream
     * @throws FileNotFoundException if the attempt to create the InputStream failed
     */
    public abstract InputStream getInputStream() throws FileNotFoundException;

}
