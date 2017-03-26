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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Limits the number of bytes and that can be written to an OutputStream.
 */
public class ByteLimitOutputStream extends FilterOutputStream {

    private int bytesLeft;
    private Runnable limitReachedAction;

    public ByteLimitOutputStream(OutputStream out, int byteLimit, Runnable limitReachedAction) {
        super(out);
        this.bytesLeft = byteLimit;
        this.limitReachedAction = limitReachedAction;
    }

    @Override
    public void write(int b) throws IOException {
        if (bytesLeft <= 0) {
            limitReachedAction.run();
            return;
        }
        super.write(b);
        bytesLeft -= 1;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (bytesLeft < len) {
            bytesLeft = 0; // prevent retry with smaller buffer
            limitReachedAction.run();
            return;
        }
        int leftPreSuper = bytesLeft;
        super.write(b, off, len);
        // subclass may call write(int) multiple times, avoid subtracting the limit multiple times
        if (bytesLeft != leftPreSuper - len) {
            bytesLeft -= len;
        }
    }
}
