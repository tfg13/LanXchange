/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.log;

import java.io.PrintStream;
import java.sql.Timestamp;

/**
 * Yes, I was unable to find a suitable + simple enough logging lib among the bazillion of choices, so I wrote my own :)
 * Features:
 * - simple, no complex reflection magic for loading
 * - designed for swing, cli and especially android
 * - manual, controlled init
 * - init as early as possible, depending on platform
 * - handle multiple inits + lazy init gracefully, this is required for some weird quirks of my android impl
 * - super simple log rotation
 * - hard limit on log file size, chosen by platform
 *
 * Everyone who logs gets an instance of this class.
 * The instance may be an object member or global/package-global, depending on the info in the "from" string.
 * The logging format produced by this is:
 * CODE DATE FROM MESSAGE
 * with:
 * - CODE = "ASRT", "ERR ", "WARN", "INFO"
 * - DATE = date, in ISO 8601
 * - FROM = class/package specific string, always 16 chars
 * - MESSAGE = actual message or stacktrace, arbitrary length
 */
public class LXCLogger {

    private static final String CODE_ASSERT = "ASRT";
    private static final String CODE_ERROR = "ERR ";
    private static final String CODE_WARN = "WARN";
    private static final String CODE_INFO = "INFO";

    private static final String EXCEPTION_SPACER = "                                           "; // 44*" "

    private final PrintStream backend;
    private final PrintStream exceptionPrinter;
    private final String from;

    /**
     * Internal constructor, use LXCLogBackend.getLogger() to get a logger.
     */
    LXCLogger(PrintStream backend, String from) {
        this.backend = backend;
        this.from = from;
        exceptionPrinter = new PrintStream(backend) {
            @Override
            public void println(Object o) {
                // this prints a large whitespace before each exception line to fix the alignment
                // best-effort only, as this relies on the internal implementation of printStackTrace to only use this
                // particular method
                super.print(EXCEPTION_SPACER);
                super.println(o);
            }
        };
    }

    public void info(String message) {
        log(CODE_INFO, message);
    }

    public void info(String message, Throwable cause) {
        log(CODE_INFO, message, cause);
    }

    public void warn(String message) {
        log(CODE_WARN, message);
    }

    public void warn(String message, Throwable cause) {
        log(CODE_WARN, message, cause);
    }

    public void error(String message) {
        log(CODE_ERROR, message);
    }

    public void error(String message,  Throwable cause) {
        log(CODE_ERROR, message, cause);
    }

    public void wtf(String message) {
        log(CODE_ASSERT, message);
    }

    public void wtf(String message,  Throwable cause) {
        log(CODE_ASSERT, message, cause);
    }

    private void log(String code, String message) {
        log(code, message, null);
    }

    private void log(String code, String message, Throwable cause) {
        backend.println(code + " " + getTime() + " " + from + ": " + message);
        if (cause != null) {
            cause.printStackTrace(exceptionPrinter);
        }
    }

    private String getTime() {
        // in the future, when java.time is available on android (coming with Android O)
        // return ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)

        // until then, we use java.sql.Timestamp. Not exactly faster than SimpleDateFormat, but at least thread safe
        return new Timestamp(System.currentTimeMillis()).toString().substring(0, 19); // cut off milliseconds
    }
}
