/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.plaf.cli.ui;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Misc static tools required by multiple classes of the CLI implementation.
 */
public final class CLITools {

    /**
     * This is the original System.out out.
     * LanXchange changes System.out for logging purposes, but the cli gui must still be able to print to stdout.
     */
    public static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
}
