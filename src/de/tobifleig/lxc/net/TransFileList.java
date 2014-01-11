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
package de.tobifleig.lxc.net;

import de.tobifleig.lxc.data.LXCFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a List of LXCFiles offered by a certain LXCInstance.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class TransFileList implements Serializable {

    /**
     * Fixed versionUID, for compatibility with previous versions.
     */
    static final long serialVersionUID = -2084933771170654220L;
    /**
     * The actual List containing all offered LXCFiles.
     */
    private final ArrayList<LXCFile> list;
    /**
     * The id of the LXCInstance that created this list.
     */
    private int originId;

    /**
     * Create a new TransFileList based on the given List of offered LXCFiles.
     *
     * @param ownList a List of all offerd LXCFiles
     */
    public TransFileList(List<LXCFile> ownList) {
        list = new ArrayList<LXCFile>(ownList);
        originId = LXCInstance.local.id;
    }

    /**
     * Sets the LXCInstance of all Files in this list.
     * This is used by remote instances to insert their local representation of this instance.
     *
     * @param instance the instance to insert in all LXCFiles.
     */
    public void setInstance(LXCInstance instance) {
        for (LXCFile file : list) {
            file.setInstance(instance);
        }
    }

    /**
     * Limits the protocolVersion of all LXCFiles in this list to the maximum supported.
     */
    public void limitTransVersions() {
        for (LXCFile file : list) {
            file.limitTransVersion();
        }
    }

    /**
     * Retrieves the internal representation of this List.
     *
     * @return the internal list of all offered LXCFiles
     */
    public List<LXCFile> getAll() {
        return list;
    }

    /**
     * Returns the id of the LXCInstance that created this list.
     *
     * @return the id of the LXCInstance that created this list
     */
    public int getOriginId() {
        return originId;
    }
}
