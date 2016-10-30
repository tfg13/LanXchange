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
package de.tobifleig.lxc.data.impl;

import de.tobifleig.lxc.data.VirtualFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation for VirtualFile.
 *
 * Backed by "real" files (java.io.File)
 *
 * @author Tobias Fleig <tobifleig@googlemail.com>
 */
public class RealFile extends VirtualFile {

    /**
     * The real file.
     */
    private final File file;

    /**
     * Creates a new top-level VirtualFile that is backed by the given real file.
     *
     * @param file the real file
     */
    public RealFile(File file) {
        super(file.getName());
        this.file = file;
    }

    /**
     * Creates a new VirtualFile that is backed by the given real file.
     *
     * @param file the real file
     * @param base transfer base file
     */
    public RealFile(File file, File base) {
        super(file.getName(), file.getAbsolutePath().substring(base.getAbsolutePath().length() + 1));
        this.file = file;
    }

    /**
     * Creates a new VirtualFile that is backed by the given real file.
     * Internal method, used when the actual toplevel node is unknown, but the transferPath is available.
     *
     * @param file the name of the new file
     * @param parentTransferPath the transfer path of the parent
     */
    private RealFile(File file, String parentTransferPath) {
        super(file.getName(), parentTransferPath + File.separatorChar + file.getName());
        this.file = file;
    }

    /**
     * Returns the real file backing this virtual file.
     *
     * @return the real file
     */
    public File getBackingFile() {
        return file;
    }

    @Override
    public long size() {
        if (isDirectory()) {
            throw new IllegalStateException("is a directory");
        }
        return file.length();
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        if (isDirectory()) {
            throw new IllegalStateException("is a directory");
        }
        return new FileInputStream(file);
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public List<VirtualFile> children() {
        if (!isDirectory()) {
            throw new IllegalStateException("not a directory");
        }
        File[] content = file.listFiles();
        ArrayList<VirtualFile> result = new ArrayList<VirtualFile>();
        if (content != null) {
            for (File contentFile : content) {
                result.add(new RealFile(contentFile, getTransferPath()));
            }
        } else {
            System.out.println("Cannot read " + file.getAbsolutePath());
        }
        return result;
    }

    @Override
    public long lastModified() {
        return file.lastModified();
    }

}
