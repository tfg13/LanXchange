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
package de.tobifleig.lxc.plaf.swing;

import java.io.IOException;
import java.io.InputStream;

/**
 * Limits the number of bytes and actual data that can be read from an InputStream.
 * Limits the data to a small subset of plain ASCII.
 */
public class VersionDataFilterInputStream extends ByteLimitInputStream {

    protected VersionDataFilterInputStream(InputStream in, int byteLimit) {
        super(in, byteLimit);
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result != -1) {
            verifyData(result);
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        for (int i = off; i < off + read; i++) {
            verifyData(b[i]);
        }
        return read;
    }

    private static void verifyData(int data) {
        // allow LF, CR and everything between SP and ~
        if ((data < 32 || data > 126) && data != 10 && data != 13) {
            throw new RuntimeException("Security abort, invalid stream data");
        }
    }
}
