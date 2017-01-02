package de.tobifleig.lxc.plaf.cli;

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
