/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * A PrintStream that allows replacements of the underlying stream.
 * This is a bit hacky.
 */
public class ModifiableSinkPrintStream extends PrintStream {


    public ModifiableSinkPrintStream(OutputStream out) {
        super(out);
    }

    synchronized void switchOutputStream(OutputStream newOut) {
        OutputStream oldOut = this.out;
        this.out = newOut;
        try {
            this.flush();
            oldOut.flush();
            // close if different and not System.out or System.err
            if (!newOut.equals(oldOut) && !oldOut.equals(System.out) && oldOut.equals(System.err)) {
                oldOut.close();
            }
        } catch (IOException ex) {
            // ignore
        }
    }
}
