/*
 * Copyright 2009, 2010, 2011, 2012, 2013 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.plaf.impl.swing;

import de.tobifleig.lxc.Configuration;
import de.tobifleig.lxc.data.LXCFile;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
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
     * The known flavors.
     */
    private DataFlavor fileFlavor, stringFlavor, uriListFlavor;
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
	this.listener = listener;
	fileFlavor = DataFlavor.javaFileListFlavor;
	stringFlavor = DataFlavor.stringFlavor;

	try {
	    uriListFlavor = new DataFlavor(URI_LIST_MIME_TYPE);
	} catch (ClassNotFoundException e) {
	    e.printStackTrace();
	}
    }

    @Override
    public Transferable createTransferable(JComponent c) {
	return null;
    }

    /**
     * Computes if the given list of flavors contains the FileListFlavor
     *
     * @param flavors the list of flavors
     * @return ture, if FileListFlavor is contained
     */
    private boolean hasFileFlavor(DataFlavor[] flavors) {
	for (int i = 0; i < flavors.length; i++) {
	    if (fileFlavor.equals(flavors[i])) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Computes if the given list of flavors contains the StringFlavor
     *
     * @param flavors the list of flavors
     * @return ture, if StringFlavor is contained
     */
    private boolean hasStringFlavor(DataFlavor[] flavors) {
	for (int i = 0; i < flavors.length; i++) {
	    if (stringFlavor.equals(flavors[i])) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Computes if the given list of flavors contains the URIListFlavor
     *
     * @param flavors the list of flavors
     * @return ture, if URIListFlavor is contained
     */
    private boolean hasURIListFlavor(DataFlavor[] flavors) {
	for (int i = 0; i < flavors.length; i++) {
	    if (uriListFlavor.equals(flavors[i])) {
		return true;
	    }
	}
	return false;
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
	if (hasFileFlavor(flavors)) {
	    return true;
	}
	if (hasStringFlavor(flavors)) {
	    return true;
	}
	if (hasURIListFlavor(flavors)) {
	    return true;
	}
	return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(JComponent c, Transferable t) {

	if ("true".equals(Configuration.getStringSetting("debug_printDropFlavors"))) {
	    DataFlavor[] dlist = t.getTransferDataFlavors();
	    System.out.println("Printing flavors: " + dlist.length);
	    for (DataFlavor f : dlist) {
		try {
		    System.out.println(f.getMimeType());
		    System.out.println(t.getTransferData(f));
		    System.out.println();
		} catch (Exception ex) {
		}
	    }
	}


	if (!canImport(c, t.getTransferDataFlavors())) {
	    return false;
	}

	try {
	    if (hasFileFlavor(t.getTransferDataFlavors()) || hasURIListFlavor(t.getTransferDataFlavors())) {

		List<File> files;
		if (hasFileFlavor(t.getTransferDataFlavors())) {
		    // Windows & OS X & Linux
		    files = (java.util.List<File>) t.getTransferData(fileFlavor);
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
			LXCFile tempFile = new LXCFile(fileList, first.getName());
			listener.newCalcedFile(tempFile);
		    }
		}, "lxc_helper_sizecalcer");
		thread.setPriority(Thread.NORM_PRIORITY - 1);
		thread.start();

		return false;

	    } else if (hasStringFlavor(t.getTransferDataFlavors())) {

		System.out.println("Unsupported Drop-Operation. Sry");

		return true;
	    }
	} catch (UnsupportedFlavorException ufe) {
	    System.out.println("importData: unsupported data flavor");
	} catch (IOException ieo) {
	    System.out.println("importData: I/O exception");
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
	for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
	    String s = st.nextToken();
	    if (s.startsWith("#")) {
		// the line is a comment (as per the RFC 2483)
		continue;
	    }
	    try {
		URI uri = new URI(s);
		File file = new File(uri);
		list.add(file);
	    } catch (URISyntaxException e) {
		e.printStackTrace();
	    } catch (IllegalArgumentException e) {
		e.printStackTrace();
	    }
	}
	return list;
    }
}
