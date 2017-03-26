/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014, 2015 Tobias Fleig (tobifleig gmail com)
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

import de.tobifleig.lxc.Configuration;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.VirtualFile;
import de.tobifleig.lxc.data.impl.InMemoryFile;
import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * The TransferHandler that manages drag&drop-operations.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class DropTransferHandler extends TransferHandler {

    private static final long serialVersionUID = 1L;
    /**
     * A special MIME type to work around java bug 4899516.
     */
    private static final String URI_LIST_MIME_TYPE = "text/uri-list;class=java.lang.String";
    /**
     * Flavor for copy-pasted plain text.
     */
    private static final String PLAIN_TEXT_MIME_TYPE = "application/x-java-serialized-object; class=java.lang.String";

    private final LXCLogger logger;
    /**
     * The known flavors for actual files.
     */
    private DataFlavor fileFlavor, uriListFlavor;
    /**
     * The known flavors for in-memory data that can be shared and saved as a file by the downloader.
     */
    private DataFlavor[] inMemoryFlavors;
    /**
     * The listener to pass events to.
     */
    private FileDropListener listener;

    /**
     * Creates a new DropTransferHandler with the given parameters.
     *
     * @param listener the listener to pass events to
     */
    public DropTransferHandler(FileDropListener listener) {
        logger = LXCLogBackend.getLogger("droptransfer-handler");
        this.listener = listener;
        fileFlavor = DataFlavor.javaFileListFlavor;
        try {
            uriListFlavor = new DataFlavor(URI_LIST_MIME_TYPE);
            // generate flavors for in-memory types
            inMemoryFlavors = new DataFlavor[2];
            inMemoryFlavors[0] = new DataFlavor(PLAIN_TEXT_MIME_TYPE);
            inMemoryFlavors[1] = DataFlavor.imageFlavor;
        } catch (ClassNotFoundException ex) {
            logger.error("Unable to create dnd flavors", ex);
        }
    }

    @Override
    public Transferable createTransferable(JComponent c) {
        return null;
    }

    /**
     * Checks if the flavor array contains the given flavor
     *
     * @param flavors the array of flavors
     * @param flavor  the flavor to search
     * @return true, iff found
     */
    private static boolean containsFlavor(DataFlavor[] flavors, DataFlavor flavor) {
        for (DataFlavor f : flavors) {
            if (flavor.equals(f)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the most fitting DataFlavor that can be used for in-memory content.
     *
     * @param flavors the list of flavors
     * @return the best DataFlavor or null
     */
    private DataFlavor getBestInMemoryFlavor(DataFlavor[] flavors) {
        for (DataFlavor inMemoryFlavor : inMemoryFlavors) {
            for (DataFlavor flavor : flavors) {
                if (inMemoryFlavor.equals(flavor)) {
                    return inMemoryFlavor;
                }
            }
        }
        return null;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    @Override
    public void exportDone(JComponent source, Transferable data, int action) {
    }

    @Override
    public boolean canImport(JComponent c, DataFlavor[] flavors) {
        return containsFlavor(flavors, fileFlavor) || containsFlavor(flavors, uriListFlavor) || getBestInMemoryFlavor(flavors) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(JComponent c, Transferable t) {
        DataFlavor[] importFlavors = t.getTransferDataFlavors();

        if ("true".equals(Configuration.getStringSetting("debug_printDropFlavors"))) {
            logger.info("Printing flavors: " + importFlavors.length);
            for (DataFlavor f : importFlavors) {
                try {
                    logger.info(f.getMimeType());
                    logger.info("" + t.getTransferData(f));
                    logger.info("");
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        if (!canImport(c, importFlavors)) {
            return false;
        }

        try {
            if (containsFlavor(importFlavors, fileFlavor) || containsFlavor(importFlavors, uriListFlavor)) {
                // actual files

                List<File> files = null;
                if (containsFlavor(importFlavors, fileFlavor)) {
                    // Windows & OS X & Linux
                    try {
                        files = (java.util.List<File>) t.getTransferData(fileFlavor);
                    } catch (InvalidDnDOperationException ex) {
                        // OpenJDK bug 7188838
                        // no known resolution, fall back to manual uri-lists if possible
                        if (containsFlavor(importFlavors, uriListFlavor)) {
                            files = textURIListToFileList((String) t.getTransferData(uriListFlavor));
                        }
                    }
                } else {
                    // old, buggy vms on Linux
                    files = textURIListToFileList((String) t.getTransferData(uriListFlavor));
                }

                // create LXCFile in new thread, constructor blocks until size-calcing is finished!
                final File first = files.get(0);
                final List<File> fileList = files;
                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        listener.displayCalcing();
                        LXCFile tempFile = new LXCFile(LXCFile.convertToVirtual(fileList), first.getName());
                        listener.newCalcedFile(tempFile);
                    }
                }, "lxc_helper_sizecalcer");
                thread.setPriority(Thread.NORM_PRIORITY - 1);
                thread.start();

                return false;

            } else {
                DataFlavor inMemoryFlavor = getBestInMemoryFlavor(importFlavors);
                if (inMemoryFlavor != null) {
                    // memory cached content like copy-pasted strings, can be downloaded as a file anyway
                    byte[] data;
                    String name;
                    if (inMemoryFlavor.equals(inMemoryFlavors[0])) {
                        // string
                        // convert to byte array
                        ByteArrayOutputStream arrayOutput = new ByteArrayOutputStream();
                        OutputStreamWriter writer = new OutputStreamWriter(arrayOutput);
                        writer.write((String) t.getTransferData(inMemoryFlavors[0]));
                        writer.close();
                        data = arrayOutput.toByteArray();
                        name = listener.generateUniqueFileName("text", "txt");
                    } else {
                        // image
                        ByteArrayOutputStream arrayOutput = new ByteArrayOutputStream();
                        Object image = t.getTransferData(inMemoryFlavors[1]);
                        if (!(image instanceof RenderedImage)) {
                            logger.warn("Unsupported image format");
                            return false;
                        }
                        ImageIO.write((RenderedImage) image, "png", arrayOutput);
                        data = arrayOutput.toByteArray();
                        name = listener.generateUniqueFileName("image","png");
                    }

                    LXCFile memFile = new LXCFile(Collections.<VirtualFile>singletonList(new InMemoryFile(name, data)), name);
                    listener.newCalcedFile(memFile);
                    return true;
                } else {
                    logger.error("Unsupported Drop-Operation. Sry");
                }
            }
        } catch (UnsupportedFlavorException ufe) {
            logger.warn("importData: unsupported data flavor");
        } catch (IOException ieo) {
            logger.warn("importData: I/O exception");
        } catch (RuntimeException ex) {
            // the event dispatcher silently discards this exception, therefore we catch it here
            logger.error("importData: Runtime Exception.", ex);
        }
        return false;
    }

    /**
     * Convert a URIList to a File list.
     * This is a workaround for java bug 4899516
     *
     * @param data a String containing a encoded file-list
     * @return a real file list
     */
    private java.util.List<File> textURIListToFileList(String data) {
        java.util.List<File> list = new ArrayList<File>(1);
        for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens(); ) {
            String s = st.nextToken();
            if (s.startsWith("#")) {
                // the line is a comment (as per the RFC 2483)
                continue;
            }
            try {
                if (s.charAt(0) == '\0') {
                    // ignore this line, this is JDK bug 7188838
                    continue;
                }
                URI uri = new URI(s);
                File file = new File(uri);
                list.add(file);
            } catch (URISyntaxException | IllegalArgumentException ex) {
                logger.error("Unexpected exception during textURIListToFileList", ex);
            }
        }
        return list;
    }
}
