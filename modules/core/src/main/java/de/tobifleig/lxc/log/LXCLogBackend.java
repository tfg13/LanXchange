/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.log;

import de.tobifleig.lxc.util.ByteLimitOutputStream;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Logging backend, holds the actual print stream.
 *
 * See LXCLogger.java for some general comments about this logging system.
 *
 * Manual init by platform required, sends everything to System.out before init() is called.
 *
 * Note: This logging system has full support for both lazy initialization and re-initialization.
 * Instances returned by getLogger() will log to System.out before init() is called.
 * After init() is called, however, these same instances automatically log to the initialized file.
 * It is also perfectly legal to call init() again later to switch the logging target somewhere else,
 * reloading instances returned by getLogger() is unnecessary.
 */
public final class LXCLogBackend {

    /**
     * Marker String that is printed when the configured log file size limit is reached.
     */
    private static final String logFullMessage = "---- LXCLog: Suspended log output, hard size limit reached. ----";

    /**
     * Number of bytes that can be written before the max log file size is reached.
     *
     * This limit is ignored in debug mode
     */
    private static int byteLimit;
    /**
     * How many old log files to keep.
     */
    private static int logRotationCopies = 3;

    /**
     * The actual, backend printer.
     */
    private static ModifiableSinkPrintStream printer = new ModifiableSinkPrintStream(System.out);

    private LXCLogBackend() {}

    public static void init(File targetDir, int logSizeLimit, int logRotationCopies, boolean debug) {
        // if debug is set, just send everything to system.out so the IDE sees it
        // note: debug mode ignores the log file size limit
        if (debug) {
            System.out.println("LXCLog: Debug mode, logging everything to System.out");
            printer.switchOutputStream(System.out);// re-set to System.out, may have changed since init
            return;
        }
        if (targetDir == null || logSizeLimit <= 0) {
            throw new IllegalArgumentException("targetDir cannot be null, logSizeLimit must be greater than zero");
        }

        byteLimit = logSizeLimit;
        LXCLogBackend.logRotationCopies = logRotationCopies;

        boolean logOK = true;
        File logFile = createFile(targetDir);
        if (logFile == null) {
            logOK = false;
        } else {
            try {
                PrintStream sizeLimitedPrinter = createSizeLimitPrinter(logFile);
                printer.switchOutputStream(sizeLimitedPrinter);
            } catch (FileNotFoundException ex) {
                logOK = false;
            }
        }

        if (!logOK) {
            // continue to run and send everything to system.out
            System.err.println("LXCLog: Cannot initialize logging in dir \"" + targetDir.getAbsolutePath() +
                    "\", sending all log content to System.out instead.");
            printer.switchOutputStream(System.out);
        }
    }

    public static LXCLogger getLogger(String from) {
        // format from so it has 16 chars
        from = String.format("%16.16s", from);
        return new LXCLogger(printer, from);
    }

    private static File createFile(File targetDir) {
        File current = new File(targetDir, "lxc.log");
        // handle log rotation
        if (current.exists() && logRotationCopies > 0) {
            for (int i = logRotationCopies - 1; i >= 1; i--) {
                File rotateDown = new File(targetDir,"lxc.log.prev" + i);
                File rotateVictim = new File(targetDir,"lxc.log.prev" + (i + 1));
                if (rotateDown.exists()) {
                    if (rotateVictim.exists()) {
                        rotateVictim.delete();
                    }
                    rotateDown.renameTo(rotateVictim);
                }
            }
            File previous = new File(targetDir,"lxc.log.prev1");
            if (previous.exists()) {
                previous.delete();
            }
            current.renameTo(previous);
        }
        if (!current.exists()) {
            try {
                current.createNewFile();
            } catch (IOException ex) {
                System.err.println("Cannot write to logfile \"lxc.log\"");
            }
        }
        if (!current.canWrite()) {
            System.err.println("Cannot write to logfile \"lxc.log\"");
            return null;
        }
        return current;
    }

    private static PrintStream createSizeLimitPrinter(File logFile) throws FileNotFoundException {
        final FileOutputStream fileOut = new FileOutputStream(logFile);
        return new PrintStream(new ByteLimitOutputStream(fileOut, byteLimit, new Runnable() {

            boolean notePrinted = false;

            @Override
            public void run() {
                // when limit is reached, print notice in file once, bypassing the limited stream
                if (notePrinted) {
                    return;
                }
                notePrinted = true;
                try {
                    fileOut.write(logFullMessage.getBytes());
                } catch (IOException ex) {
                    // ignore
                }
            }
        }));
    }

}
