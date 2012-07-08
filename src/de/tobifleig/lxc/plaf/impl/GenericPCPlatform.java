/*
 * Copyright 2009, 2010, 2011, 2012 Tobias Fleig (tobifleig gmail com)
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
package de.tobifleig.lxc.plaf.impl;

import de.tobifleig.lxc.Configuration;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.Platform;
import de.tobifleig.lxc.plaf.impl.swing.LXCUpdater;
import de.tobifleig.lxc.plaf.impl.swing.SwingGui;
import java.io.*;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * A generic Platform for all OS prividing Swing and write access to the
 * LXC-directory. Offers a main-method to start LanXchange.
 * 
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class GenericPCPlatform implements Platform {

	/**
	 * path to cfg-file.
	 */
	private static final String CONFIG_PATH = "lxc.cfg";
	/**
	 * the swing-gui.
	 */
	private SwingGui gui = new SwingGui();

	@Override
	public boolean hasAutoUpdates() {
		return true;
	}

	@Override
	public void checkAndPerformUpdates(String[] args) {
		// Find out if updating is allowed, requested, forced and/or if the
		// verification should be disabled.
		boolean checkForUpdates = false;
		boolean forceUpdate = false;
		boolean overrideVerification = false;
		boolean restartable = false;
		if (Configuration.containsKey("allowupdates")) {
			String result = Configuration.getStringSetting("allowupdates");
			if ("yes".equals(result.toLowerCase())
					|| "true".equals(result.toLowerCase())) {
				checkForUpdates = true;
			}
		} else {
			// not set yet, default to true (yes)
			checkForUpdates = true;
			Configuration.putStringSetting("allowupdates", "true");
		}
		// special settings
		for (String s : args) {
			if (s.equals("-update")) {
				checkForUpdates = true;
			} else if (s.equals("-forceupdate")) {
				forceUpdate = true;
			} else if (s.equals("-overrideVerification")) {
				overrideVerification = true;
			} else if (s.equals("-managed")) {
				// whoever launched LXC tells us he is able to restart us in
				// case of an update
				restartable = true;
			}
		}
		if (checkForUpdates || forceUpdate) {
			System.out.println("Checking for Updates...");
			// Ugly workaround, anonymous inner classes require (local)
			// variables to be final
			final boolean force = forceUpdate;
			final boolean noVerification = overrideVerification;
			final boolean managed = restartable;
			// check in seperate thread
			Runnable r = new Runnable() {

				@Override
				public void run() {
					try {
						LXCUpdater.checkAndPerformUpdate(gui, force,
								noVerification, managed);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};

			Thread t = new Thread(r);
			t.setName("updatecheck");
			t.setDaemon(true);
			t.start();
		} else {
			System.out
					.println("Not checking for updates. (disabled via lxc.cfg)");
		}
	}

	@Override
	public GuiInterface getGui(String[] args) {
		return gui;
	}

	@Override
	public void readConfiguration(String[] args) {
		BufferedReader reader = null;
		try {
			File cfgFile = new File(CONFIG_PATH);
			// create on first start
			if (!cfgFile.exists()) {
				cfgFile.createNewFile();
			}
			reader = new BufferedReader(new FileReader(cfgFile));
			String zeile;
			int i = 0; // line number
			while ((zeile = reader.readLine()) != null) {
				// read line after line, add content to Configuration
				int indexgleich = zeile.indexOf('='); // search for "="
				if (indexgleich == -1) {
				} else {
					String v1 = zeile.substring(0, indexgleich); // prefix
																	// (before
																	// =)
					String v2 = zeile.substring(indexgleich + 1); // suffix
																	// (after =)
					Configuration.putStringSetting(v1, v2);
				}
			}
		} catch (FileNotFoundException e1) {
			// this is serious. exit
			System.out.println("Cannot read/write configfile (cfg.txt)");
			// display warning (gui not available yet)
			JOptionPane
					.showMessageDialog(
							new JFrame(),
							"Cannot write to own folder. Please move to your home directory or start as administrator.",
							"Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		} catch (IOException e2) {
			e2.printStackTrace();
			System.exit(1);
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException ex) {
				// ignore
			}
		}
	}

	@Override
	public void writeConfiguration() {
		FileWriter writer = null;
		try {
			writer = new FileWriter(CONFIG_PATH);
			Iterator<String> iter = Configuration.getKeyIterator();
			while (iter.hasNext()) {
				String s = iter.next();
				writer.append(s.toString() + "="
						+ Configuration.getStringSetting(s) + '\n');
			}
		} catch (IOException ex) {
			System.out.println("CRITICAL: FEHLER BEIM SCHREIBEN DES LOGFILES!");
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException ex) {
			}
		}
	}

	@Override
	public boolean askForDownloadTargetSupported() {
		return true;
	}

	@Override
	public String getDefaultDownloadTarget() {
		// depends on confituration
		if (!Configuration.containsKey("defaulttarget")) {
			return null; // default = always ask
		} else {
			// check if valid
			File file = new File(
					Configuration.getStringSetting("defaulttarget"));
			if (file.isDirectory() && file.canWrite()) {
				return file.getAbsolutePath();
			}
		}
		return null; // invalid path = ask
	}

	/**
	 * Starts LanXchange on PC plaforms.
	 * 
	 * @param args
	 *            any arguments you want to pass to LanXchange
	 */
	public static void main(String[] args) {
		// check permissions for own folder. will be removed soon

		File home = new File(".");
		if (!home.canWrite()) {
			System.out.println("ERROR: Cannot write to own directory ("
					+ home.getAbsolutePath()
					+ "). Try running LXC in your home directory.");
			gui.showError("Cannot write to own folder. Please move to your home directory or start as administrator.");
			System.exit(1);
		}
		LXC lxc = new LXC(new GenericPCPlatform(), args);
	}
}
