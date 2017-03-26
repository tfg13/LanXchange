/*
 * Copyright (C) 2016 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Limits the number of bytes and that can be read from an InputStream.
 */
public class ByteLimitInputStream extends FilterInputStream {

    private int bytesAvailable;

    public ByteLimitInputStream(InputStream in, int byteLimit) {
        super(in);
        bytesAvailable = byteLimit;
    }

    @Override
    public int read() throws IOException {
        if (bytesAvailable <= 0) {
            return -1;//EOS
        }
        int result = super.read();
        if (result != -1) {
            bytesAvailable -= 1;
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, Math.min(bytesAvailable, len));
        if (read != -1) {
            bytesAvailable -= read;
        }
        return read;
    }

    @Override
    public int available() throws IOException {
        return Math.min(bytesAvailable, super.available());
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException("Mark not supported");
    }

    @Override
    public long skip(long n) throws IOException {
        long result = super.skip(Math.min(bytesAvailable, n));
        bytesAvailable -= result;
        return result;
    }
}
