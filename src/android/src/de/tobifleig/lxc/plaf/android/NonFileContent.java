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
package de.tobifleig.lxc.plaf.android;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import de.tobifleig.lxc.data.VirtualFile;

public class NonFileContent extends VirtualFile {

    /**
     * Links to the real file.
     */
    private final ParcelFileDescriptor descriptor;

    /**
     * Android content:// Uri
     */
    private final Uri fileUri;

    /**
     * ContentResolver, required to open the InputStream.
     */
    private final ContentResolver resolver;

    /**
     * Creates a new virtual file for android content resources (>=kitkat)
     * 
     * @param name the name of this file, as usual
     * @param descriptor file descriptor to read the size
     * @param uri uri of the file
     * @param resolver to open an InputStream
     */
    public NonFileContent(String name, ParcelFileDescriptor descriptor, Uri uri, ContentResolver resolver) {
        super(name);
        this.descriptor = descriptor;
        this.fileUri = uri;
        this.resolver = resolver;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public List<VirtualFile> children() {
        throw new UnsupportedOperationException("only directorys have children!");
    }

    @Override
    public long size() {
        return descriptor.getStatSize();
    }

    @Override
    public long lastModified() {
        return System.currentTimeMillis();
    }

    @Override
    public InputStream getInputStream() throws FileNotFoundException {
        return resolver.openInputStream(fileUri);
    }

}
