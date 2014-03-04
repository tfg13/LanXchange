package de.tobifleig.lxc.plaf.impl.android;

import java.util.ArrayList;
import java.util.List;

import de.tobifleig.lxc.data.LXCFile;

/**
 * Wraps LXCs file list for the android gui.
 * Caches information about number of elements etc.
 * 
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class FileListWrapper {
	
	/**
	 * The list to wrap.
	 */
	private final List<LXCFile> list;
	/**
	 * The last hashCode of the list.
	 * Used for caching.
	 */
	private long lastHashCode = -1;
	/**
	 * Local files.
	 */
	private final ArrayList<LXCFile> ownFiles;
	/**
	 * Remote files.
	 */
	private final ArrayList<LXCFile> networkFiles;
	
	/**
	 * Creates a new wrapper for the given list.
	 * @param list
	 */
	public FileListWrapper(List<LXCFile> list) {
		this.list = list;
		ownFiles = new ArrayList<LXCFile>();
		networkFiles = new ArrayList<LXCFile>();
		recompute();
	}
	
	/**
	 * Call this when the list may have changed.
	 */
	public void listChanged() {
		if (lastHashCode != list.hashCode()) {
			recompute();
		}
	}
	
	/**
	 * Returns the list of local LXCFiles
	 * @return the list of local LXCFiles
	 */
	public List<LXCFile> getLocalList() {
		return ownFiles;
	}
	
	/**
	 * Returns the list of remote LXCFiles
	 * @return the list of remote LXCFiles
	 */
	public List<LXCFile> getRemoteList() {
		return networkFiles;
	}
	
	/**
	 * Recomputes the list, creates the required sublists.
	 */
	private void recompute() {
		ownFiles.clear();
		networkFiles.clear();
		for (LXCFile file : list) {
			if (file.isLocal()) {
				ownFiles.add(file);
			} else {
				networkFiles.add(file);
			}
		}
	}

}
