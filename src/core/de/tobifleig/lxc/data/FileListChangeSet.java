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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of changes made to a file list.
 */
public class FileListChangeSet {

    /**
     * Files that have been added.
     */
    private final List<FileModification> addedFiles;
    /**
     * Files that have been removed.
     */
    private final List<FileModification> removedFiles;

    /**
     * Constructs a new FileListChangeSet with the given parameters.
     */
    public FileListChangeSet() {
        this.addedFiles = new ArrayList<FileModification>();
        this.removedFiles = new ArrayList<FileModification>();
    }

    /**
     * Returns all added files.
     * @return added files
     */
    public List<FileModification> getAddedFiles() {
        return addedFiles;
    }

    /**
     * Returns all removed files.
     * @return removed files
     */
    public List<FileModification> getRemovedFiles() {
        return removedFiles;
    }

    public void pushAddedFile(LXCFile file, int index) {
        addedFiles.add(new FileModification(file, index));
    }

    public void pushRemovedFile(LXCFile file, int index) {
        removedFiles.add(new FileModification(file, index));
    }

    /**
     * One single modification, LXCFile + (new/previous) index
     */
    public class FileModification {

        public final LXCFile file;
        public final int index;

        public FileModification(LXCFile file, int index) {
            this.file = file;
            this.index = index;
        }
    }
}
