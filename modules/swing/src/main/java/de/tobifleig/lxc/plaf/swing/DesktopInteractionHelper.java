/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Tobias Fleig (tobifleig gmail com)
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

import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Helpers to open websites, send mails etc.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
final class DesktopInteractionHelper {

    private static final LXCLogger logger = LXCLogBackend.getLogger("desktop-helper");

    /**
     * utility class
     */
    private DesktopInteractionHelper() {}

    /**
     * Attempts to open the given URL with the default browser
     * @param url website url
     */
    static void openURL(String url) {
        // should work on windows, most linux desktops
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(url));
                    return;
                } catch (IOException | URISyntaxException ex) {
                    logger.error("open browser with method 1 failed", ex);
                }
            }
        }
        // may work on some other linux desktops
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("xdg-open " + url);
            return;
        } catch (IOException ex) {
            logger.error("open browser with method 2 failed", ex);
        }

        // last straw for mac
        try {
            runtime.exec("open" + url);
        } catch (IOException ex) {
            logger.error("open browser with method 3 failed", ex);
        }
    }

    /**
     * Attempts to open the uses mail program to send a mail.
     * @param mailto mailto:mail@example.com
     */
    static void sendMail(String mailto) {
        // should work on windows, most linux desktops
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.MAIL)) {
                try {
                    desktop.mail(new URI(mailto));
                } catch (IOException | URISyntaxException ex) {
                    logger.error("send mail failed", ex);
                }
            }
        }
    }

    /**
     * Attempts to open a file with the users default program for the file.
     * Multiple files open the first one, directories open in file manager (ideally).
     * Opening files with the "default" application is a source of constant drama even in 2016.
     * For example, it does not always work on android, and there is even an API for that.
     * This code performs exactly one simple attempt on the usual platforms.
     * Linux requires xdg-open, which is much better than most people think.
     *
     * @param file the file to open
     */
    static void openFile(File file) {
        // there is Desktop::open(File), but it does strange things
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            try {
                Runtime.getRuntime().exec("rundll32 SHELL32.DLL,ShellExec_RunDLL "+ file.getAbsolutePath());
            } catch (IOException ex) {
                logger.error("open file with method 1 failed", ex);
            }
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                Runtime.getRuntime().exec("open "+ file.getAbsolutePath());
            } catch (IOException ex) {
                logger.error("open file with method 2 failed", ex);
            }
        } else { // linux, bsd etc
            // requires xdg-open
            try {
                Runtime.getRuntime().exec("xdg-open "+ file.getAbsolutePath());
            } catch (IOException ex) {
                logger.error("open file with method 3 failed", ex);
            }
        }
    }
}
