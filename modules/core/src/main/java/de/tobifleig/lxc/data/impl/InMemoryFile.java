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
package de.tobifleig.lxc.data.impl;

import de.tobifleig.lxc.data.VirtualFile;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * Implementation of VirtualFile for memory cached content.
 * Can be used to share clipboard contents.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class InMemoryFile extends VirtualFile {

    /**
     * Stores the file content.
     */
    private final byte[] rawData;

    /**
     * Creates a new InMemoryFile that is backed by the given byte array.
     * The given raw data array must not be modified after passing it to this function.
     *
     * @param name full file name
     * @param data raw data, will not be copied
     */
    public InMemoryFile(String name, byte[] data) {
        super(name);
        rawData = data;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public List<VirtualFile> children() {
        return null;
    }

    @Override
    public long size() {
        return rawData.length;
    }

    @Override
    public long lastModified() {
        return System.currentTimeMillis();
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        return new ByteArrayInputStream(rawData);
    }
}
