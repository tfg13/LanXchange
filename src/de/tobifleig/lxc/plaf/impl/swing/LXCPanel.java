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

import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.data.FileManager;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.LXCJob;
import de.tobifleig.lxc.plaf.GuiListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The main part of the Swing-Gui.
 * Displays everything.
 * Overrides paintComponent and uses its own font to
 * guarantee a pixel-perfect look on all platforms.
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class LXCPanel extends JPanel {

    /*
     * Code is "quick and dirty", needs a rewrite...
     */
    private static final long serialVersionUID = 1L;
    @Deprecated
    private List<LXCFile> allFiles;
    private transient Image logo;
    private transient Image txt;
    private transient Image mini;
    private transient Image harddisk;
    private transient Image fileImg;
    private transient Image folder;
    private transient Image multi;
    private transient Image delete;
    private transient Image busy;
    private transient Image done;
    private transient Image help;
    private transient Image screw;
    private transient Image download;
    private transient Image cancel;
    private Color background;
    private Color selBackground;
    private Font f1;
    private Font f2;
    private FontMetrics mer;
    private FontMetrics mer2;
    private int selectedIndex = -1;
    private int subJobDeleteSelected = -1;
    boolean detailSelected = false;
    boolean running = false;
    boolean helpHovered = false;
    private FileManager fileManager;
    private GuiListener guiListener;
    private OptionsDialog options;
    private boolean calcing;

    @Override
    public void paintComponent(Graphics g) {
	if (running) {
	    GradientPaint grad = new GradientPaint(new Point(0, 0), new Color(192, 192, 192, 0), new Point(this.getWidth() / 2, 0), Color.LIGHT_GRAY, true);
	    Graphics2D g2 = (Graphics2D) g;
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.setColor(Color.WHITE);
	    g2.fillRect(0, 0, this.getWidth(), this.getHeight());
	    // center LXC-Logo
	    g2.drawImage(logo, (this.getWidth() / 2) - (logo.getWidth(this) / 2), (this.getHeight() / 2) - (logo.getHeight(this) / 2), this);
	    // Text "LanXchange" (bottom, center)
	    g2.drawImage(txt, (this.getWidth() / 2) - (txt.getWidth(this) / 2), this.getHeight() - txt.getHeight(this), this);
	    // help button
	    if (help != null) {
		g2.drawImage(help, 0, this.getHeight() - 21, this);
		g2.setColor(Color.BLACK);
		g2.drawLine(0, this.getHeight() - 35, 35, this.getHeight());
	    }
	    // settings button
	    if (screw != null) {
		g2.drawImage(screw, this.getWidth() - 21, this.getHeight() - 21, this);
		g2.setColor(Color.BLACK);
		g2.drawLine(this.getWidth() - 35, this.getHeight(), this.getWidth(), this.getHeight() - 35);
	    }
	    // header
	    if (!allFiles.isEmpty()) {
		g2.setColor(background);
		g2.fillRect(-1, 0, this.getWidth() + 1, 19);
		g2.setPaint(grad);
		g2.drawRect(-1, -1, this.getWidth() + 1, 20);
		g2.setPaint(null);
		g2.setFont(f2);
		g2.setColor(Color.BLACK);
		g2.drawString("Type", 6, 8 + (mer2.getAscent() / 2));
		g2.drawString("Name", 47, 8 + (mer2.getAscent() / 2));
		g2.drawString("Size", (int) (1.0 * this.getWidth() * 0.7), 8 + (mer2.getAscent() / 2));
	    } else {
		g2.setColor(background);
		g2.fillRect(-1, 40, this.getWidth() + 1, 30);
		g2.setPaint(grad);
		g2.drawRect(-1, 40, this.getWidth() + 1, 30);
		g2.setPaint(null);
		g2.setFont(f2);
		g2.setColor(Color.BLACK);
		String noFilesText1 = "nothing found in your home network";
		g2.drawString(noFilesText1, this.getWidth() / 2 - mer2.stringWidth(noFilesText1) / 2, 40 + mer2.getAscent() + 4);
		String noFilesText2 = "drop files here to start sharing";
		g2.drawString(noFilesText2, this.getWidth() / 2 - mer2.stringWidth(noFilesText2) / 2, 40 + mer2.getAscent() + 15);
	    }
	    int y = 20; // y-coordinate
	    // files
	    for (int i = 0; i < allFiles.size(); i++) {
		LXCFile file = allFiles.get(i);
		// number of jobs?
		int jobYPxl = 0;
		if (file.getJobs() != null) {
		    jobYPxl += file.getJobs().size() * 20;
		}
		// selected (=hovered)
		if (selectedIndex != i) {
		    g2.setColor(background);
		} else {
		    g2.setColor(selBackground);
		}
		if (jobYPxl == 0 && !file.isLocal()) {
		    g2.fillRect(-1, y, this.getWidth() + 1, selectedIndex == i ? 40 : 30);
		} else {
		    g2.fillRect(-1, y, this.getWidth() + 1, 30 + jobYPxl);
		}
		// type
		g2.drawImage(file.isLocal() ? harddisk : mini, 4, y + 5, this);
		if (file.getType() == LXCFile.TYPE_FILE) {
		    g2.drawImage(fileImg, 25, y + 5, this);
		} else if (file.getType() == LXCFile.TYPE_FOLDER) {
		    g2.drawImage(folder, 25, y + 5, this);
		} else if (file.getType() == LXCFile.TYPE_MULTI) {
		    g2.drawImage(multi, 25, y + 5, this);
		}
		// name
		g2.setColor(Color.BLACK);
		g2.setFont(f1);
		renderCutString(file.getShownName(), (int) (1.0 * this.getWidth() * 0.7) - 49, g2, 47, y + 14 + (mer.getAscent() / 2), mer);
		// size
		g2.drawString(LXCFile.getFormattedSize(file.getFileSize()), (int) (1.0 * this.getWidth() * 0.7), y + 14 + (mer.getAscent() / 2));
		if (file.isLocal()) {
		    if (detailSelected && selectedIndex == i) {
			g2.setColor(background);
			g2.fillRect(this.getWidth() - 23, y + 6, 17, 19);
		    }
		    // trash
		    g2.drawImage(delete, this.getWidth() - 25, y + 5, this);
		} else {
		    g2.drawImage(file.isAvailable() ? done : download, this.getWidth() - 25, y + 5, this);
		}
		if (selectedIndex == i) {
		    if (jobYPxl == 0 && !file.isLocal()) {
			g2.setFont(f2);
			String status = "Available - Click to download";
			if (file.isAvailable()) {
			    status = "Download completed - Right-click to remove/redownload";
			}
			if (!file.isLocked()) {
			    g2.drawString(status, this.getWidth() / 2 - mer2.stringWidth(status) / 2, y + 36); // center
			}

		    }
		}
		if (file.isLocked() && jobYPxl == 0) {
		    g2.setColor(selBackground);
		    g2.fillRect(2, y, this.getWidth() - 4, selectedIndex == i ? 40 : 30);
		    // busy
		    g2.drawImage(busy, this.getWidth() / 2 - busy.getWidth(this) / 2, selectedIndex == i ? y + 12 : y + 7, this);
		}
		// jobs
		List<LXCJob> jobs = file.getJobs();
		if (jobs != null) {
		    for (int o = 0; o < jobs.size(); o++) {
			LXCJob job = jobs.get(o);
			String to = "from";
			if (job.isIsSeeder()) {
			    to = "to";
			}
			g2.setFont(f2);
			g2.setColor(Color.BLACK);
			renderCutString(to + " " + job.getRemote().getName(), (int) (this.getWidth() * 0.3), g2, 10, y + 30 + o * 20 + 14, mer2);
			// progress bar
			g2.drawRect((int) (this.getWidth() * 0.65), y + 30 + o * 20 + 5, (int) (this.getWidth() * 0.35) - 25, 10);
			// progress in percent
			float progress = job.getTrans().getProgress();
			String sProg = ((int) (progress * 100)) + "%";
			if (progress < 0 || progress > 1) {
			    sProg = "N/A";
			} else {
			    // core of progress bar
			    g2.fillRect((int) (this.getWidth() * 0.65) + 2, y + 30 + o * 20 + 7, (int) (((int) (this.getWidth() * 0.3) - 9) * progress), 7);
			}
			renderCutString(sProg, (int) (this.getWidth() * 0.1), g2, (int) (this.getWidth() * 0.55), y + 30 + o * 20 + 14, mer2);
			// speed:
			renderCutString(LXCFile.getFormattedSize(job.getTrans().getCurrentSpeed()) + "/s", (int) (this.getWidth() * 0.15), g2, (int) (this.getWidth() * 0.35), y + 30 + o * 20 + 14, mer2);
			// cancel-button + hover
			if (subJobDeleteSelected == o) {
			    g2.setColor(background);
			    g2.fillRect(this.getWidth() - 22, y + 30 + o * 20 + 3, 15, 15);
			}
			g2.drawImage(cancel, this.getWidth() - 22, y + 30 + o * 20 + 3, this);
		    }
		}
		if (selectedIndex == i) {
		    if (jobYPxl == 0 && !file.isLocal()) {
			y += 40;
		    } else {
			y += 30 + jobYPxl;
		    }
		} else {
		    y += 30 + jobYPxl;
		}
	    }
	    // calcing
	    if (calcing) {
		g2.setColor(background);
		g2.fillRect(-1, y, this.getWidth() + 1, 20);
		g2.setPaint(grad);
		g2.drawLine(0, y + 20, this.getWidth() + 1, y + 20);
		g2.setPaint(null);
		// busy
		g2.drawImage(busy, 4, y, this);
		g2.drawImage(busy, this.getWidth() - 25, y, this);
		// text
		g2.setColor(Color.BLACK);
		g2.setFont(f1);
		String calcText = "calculating file size...";
		g2.drawString(calcText, this.getWidth() / 2 - mer.stringWidth(calcText) / 2, y + 9 + (mer.getAscent() / 2));
	    }
	    // seperator line
	    if (!allFiles.isEmpty()) {
		g2.setPaint(grad);
		g2.drawLine(0, y, this.getWidth(), y);
		g2.setPaint(null);
	    }
	    // display help text?
	    if (helpHovered) {
		g2.setColor(Color.WHITE);
		g2.fillRect(5, this.getHeight() - 205, 190, 200);
		g2.setColor(Color.BLACK);
		g2.drawRect(5, this.getHeight() - 205, 190, 200);
		g2.setFont(f1);
		g2.drawString("quick help", 15, this.getHeight() - 180);
		g2.setFont(f2);
		g2.drawString("This window contains all files", 15, this.getHeight() - 150);
		g2.drawString("currently available in your", 15, this.getHeight() - 140);
		g2.drawString("home network.", 15, this.getHeight() - 130);
		g2.drawString("To download files, simply", 15, this.getHeight() - 110);
		g2.drawString("click them with your mouse.", 15, this.getHeight() - 100);
		g2.drawString("", 15, this.getHeight() - 90);
		g2.drawString("To offer files yourself,", 15, this.getHeight() - 80);
		g2.drawString("drag them into this window.", 15, this.getHeight() - 70);
		g2.drawString("", 15, this.getHeight() - 60);
		g2.drawString("", 15, this.getHeight() - 50);
		g2.drawString("Modify the default settings", 15, this.getHeight() - 40);
		g2.drawString("by clicking the wrench", 15, this.getHeight() - 30);
		g2.setColor(Color.LIGHT_GRAY);
		g2.drawString("v" + LXC.versionId, 190 - mer2.stringWidth("v" + LXC.versionId), this.getHeight() - 10);

	    }
	} else {
	    g.setColor(Color.BLACK);
	    g.fillRect(0, 0, this.getWidth(), this.getHeight());
	    g.setColor(Color.WHITE);
	    g.setFont(Font.decode("Sans-30"));
	    g.drawString("...", getWidth() / 4 * 3, getHeight() / 4 * 3);
	}

    }

    /**
     * Renders a String, but guarantees a maximum width.
     *
     * @param text the String
     * @param maxWidth max width in pixel
     * @param g2 graphics context
     * @param x x-target
     * @param y y-target
     */
    private void renderCutString(String text, final int maxWidth, Graphics2D g2, int x, int y, final FontMetrics metrics) {
	if (metrics.stringWidth(text) > maxWidth) {
	    // Zu lang.
	    text = text.concat("...");
	    while (text.length() > 3) {
		text = text.substring(0, text.length() - 4).concat("...");
		if (metrics.stringWidth(text) <= maxWidth) {
		    break;
		}
	    }
	}
	g2.drawString(text, x, y);
    }

    /**
     * Creates a new LXCPanel.
     * It is possible to create and display a LXCPanel without any other LXC-Component.
     * This feature was required for the NetBeans GUI-Builder.
     */
    @SuppressWarnings("unchecked")
    public LXCPanel() {
	background = new java.awt.Color(255, 255, 255, 200);
	selBackground = new java.awt.Color(220, 220, 220, 200);
	options = new OptionsDialog((JFrame) SwingUtilities.getRoot(LXCPanel.this), true);
    }

    /**
     * "Starts" the Panel.
     * Must be called only once.
     */
    public void start() {
	try {
	    logo = ImageIO.read(new File("img/logo.png"));
	    mini = ImageIO.read(new File("img/mini.png"));
	    harddisk = ImageIO.read(new File("img/harddisk.png"));
	    txt = ImageIO.read(new File("img/txt.png"));
	    fileImg = ImageIO.read(new File("img/file.png"));
	    folder = ImageIO.read(new File("img/folder.png"));
	    multi = ImageIO.read(new File("img/multiple.png"));
	    delete = ImageIO.read(new File("img/del.png"));
	    busy = ImageIO.read(new File("img/busy.png"));
	    done = ImageIO.read(new File("img/done.png"));
	    help = ImageIO.read(new File("img/help.png"));
	    screw = ImageIO.read(new File("img/screw.png"));
	    download = ImageIO.read(new File("img/download.png"));
	    cancel = ImageIO.read(new File("img/cancel.png"));
	} catch (IOException ex) {
	    ex.printStackTrace();
	}
	mer = this.getGraphics().getFontMetrics(f1);
	mer2 = this.getGraphics().getFontMetrics(f2);
	running = true;
	this.addMouseListener(new MouseListener() {
	    @Override
	    public void mouseClicked(MouseEvent e) {
	    }

	    @Override
	    public void mousePressed(MouseEvent e) {
	    }

	    @Override
	    public void mouseReleased(final MouseEvent e) {
		if (e.getX() > getWidth() - 21 && e.getY() > getHeight() - 21) {
		    // display settings
		    options.showAndWait();
		    guiListener.reloadConfiguration();
		}
		// delete?
		if (detailSelected && selectedIndex != -1) {
		    try {
			LXCFile file = allFiles.get(selectedIndex);
			if (file.isLocal()) {
			    guiListener.removeFile(file);
			}
		    } catch (Exception ex) {
		    }
		}

		// Cancel download
		if (subJobDeleteSelected != -1 && selectedIndex != -1) {
		    // Search job and cancel it
		    allFiles.get(selectedIndex).getJobs().get(subJobDeleteSelected).abortTransfer();
		} else if (selectedIndex > -1 && selectedIndex < allFiles.size()) {
		    // simple click
		    final LXCFile file = allFiles.get(selectedIndex);
		    if (!file.isLocked() && !file.isLocal() && !file.isAvailable()) {
			// init dl (threaded)
			file.setLocked(true);
			Thread t = new Thread(new Runnable() {
			    @Override
			    public void run() {
				guiListener.downloadFile(file, e.isShiftDown());
				selfTrigger();
			    }
			});
			t.setName("lxc_helper_initdl_" + file.getShownName());
			t.setDaemon(true);
			t.start();
		    } else if (e.getButton() == 3 && !file.isLocked() && !file.isLocal() && file.isAvailable()) {
			// reset file. (disappears or allows re-download)
			guiListener.resetFile(file);
		    }
		}
		selfTrigger();
	    }

	    @Override
	    public void mouseEntered(MouseEvent e) {
	    }

	    @Override
	    public void mouseExited(MouseEvent e) {
		if (selectedIndex != -1) {
		    selectedIndex = -1;
		    selfTrigger();
		}
		if (subJobDeleteSelected != -1) {
		    subJobDeleteSelected = -1;
		    selfTrigger();
		}
	    }
	});
	this.addMouseMotionListener(new MouseMotionListener() {
	    @Override
	    public void mouseDragged(MouseEvent e) {
	    }

	    @Override
	    public void mouseMoved(MouseEvent e) {
		// help-button?
		if (e.getX() < 21 && e.getY() > LXCPanel.this.getHeight() - 21) {
		    if (!helpHovered) {
			selfTrigger();
			helpHovered = true;
		    }
		} else {
		    // turn off help
		    if (helpHovered) {
			selfTrigger();
			helpHovered = false;
		    }
		    int newSelIndex = 0;
		    int my = e.getY();
		    int pre = 20;
		    int prev = 20;
		    if (my < pre) {
			newSelIndex = -1;
		    } else {
			// Add file after file until we get there
			for (int i = 0; i < allFiles.size(); i++) {
			    int plus;
			    LXCFile file = allFiles.get(i);
			    if (file.isLocal()) {
				int jobnum = file.getJobs() == null ? 0 : file.getJobs().size();
				plus = 30 + jobnum * 20;
			    } else {
				int jobnum = file.getJobs() == null ? 0 : file.getJobs().size();
				if (jobnum == 0) {
				    if (selectedIndex == i) {
					plus = 40;
				    } else {
					plus = 30;
				    }
				} else {
				    plus = 30 + jobnum * 20;
				}
			    }
			    pre += plus;
			    // reached mouseY?
			    if (pre >= my) {
				break;
			    } else {
				newSelIndex++;
				prev += plus;
			    }
			}
		    }
		    boolean changed = false;
		    if (newSelIndex < 0 || newSelIndex >= allFiles.size()) {
			// invalid, set to -1
			if (selectedIndex != -1) {
			    selectedIndex = -1;
			    changed = true;
			}
		    } else {
			if (selectedIndex != newSelIndex) {
			    selectedIndex = newSelIndex;
			    changed = true;
			}
			// job-delete button
			if (e.getX() >= LXCPanel.this.getWidth() - 22 && e.getX() <= LXCPanel.this.getWidth() - 7) {
			    // mouseX in range, check Y
			    boolean found = false;
			    for (int o = 0; o < allFiles.get(selectedIndex).getJobs().size(); o++) {
				if (e.getY() >= prev + 30 + 4 + o * 20 && e.getY() <= prev + 30 + 4 + o * 20 + 15) {
				    if (subJobDeleteSelected != o) {
					subJobDeleteSelected = o;
					changed = true;
				    }
				    found = true;
				    break;
				}
			    }
			    if (!found) {
				if (subJobDeleteSelected != -1) {
				    subJobDeleteSelected = -1;
				    changed = true;
				}
			    }
			} else {
			    if (subJobDeleteSelected != -1) {
				subJobDeleteSelected = -1;
				changed = true;
			    }
			}
			if (e.getX() > LXCPanel.this.getWidth() - 28 && e.getX() < LXCPanel.this.getWidth() - 7) {
			    if (e.getY() >= prev + 5 && e.getY() <= prev + 26) {
				if (!detailSelected) {
				    detailSelected = true;
				    changed = true;
				}

			    } else {
				if (detailSelected) {
				    detailSelected = false;
				    changed = true;
				}
			    }
			} else {
			    if (detailSelected) {
				detailSelected = false;
				changed = true;
			    }
			}
		    }
		    if (changed) {
			selfTrigger();
		    }
		}
	    }
	});

	selfTrigger();
    }

    /**
     * Triggers a full repaint ASAP.
     */
    private void selfTrigger() {
	SwingUtilities.invokeLater(new Runnable() {
	    @Override
	    public void run() {
		repaint();
	    }
	});
    }

    /**
     * Sets the file manager.
     * Must be called before start().
     *
     * @param fileManager
     */
    public void setFileList(List<LXCFile> fileList) {
	this.allFiles = fileList;
    }

    /**
     * Sets the gui listener.
     * Must be called before start().
     *
     * @param guiListener the new gui listener
     */
    public void setGuiListener(GuiListener guiListener) {
	this.guiListener = guiListener;
    }

    /**
     * Turns displaying of "size calculation in progress" on or off.
     *
     * @param calcing if calculation is currently running.
     */
    void setCalcing(boolean calcing) {
	this.calcing = calcing;
	selfTrigger();
    }

    /**
     * Sets the font to be used for displaying text.
     *
     * @param font
     */
    void setUsedFont(Font font) {
	f1 = font.deriveFont(15f);
	f2 = font.deriveFont(10f);
    }
}
