/*
 * Copyright 2009, 2010, 2011, 2012, 2013, 2014 Tobias Fleig (tobifleig gmail com)
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

import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.data.LXCJob;
import de.tobifleig.lxc.data.VirtualFile;
import de.tobifleig.lxc.data.impl.RealFile;
import de.tobifleig.lxc.plaf.GuiListener;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
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
    private static final int HOVER_NONE = -1;
    private static final int HOVER_SELFDIST = 0;
    private static final int HOVER_SETTINGS = 1;
    private static final int HOVER_HELP = 2;
    private static final int DRAWMODE_INIT = 3;
    private static final int DRAWMODE_MAIN = 4;
    private static final int DRAWMODE_HELP = 5;
    @Deprecated
    private List<LXCFile> allFiles;
    private transient Image logo;
    private transient Image small;
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
    private transient Image selfdist_small;
    private final Color background;
    private final Color selBackground;
    private Font f0;
    private Font f1;
    private Font f1b;
    private Font f2;
    private Font f1Fallback;// if filename contains glyphs not supported by the default ubuntu font
    private FontMetrics mer0;
    private FontMetrics mer1;
    private FontMetrics mer1Fallback;
    private FontMetrics mer1b;
    private FontMetrics mer2;
    private Cursor urlClickableCursor;
    private int selectedIndex = -1;
    private int subJobDeleteSelected = -1;
    private int traySelected = HOVER_NONE;
    private boolean detailSelected = false;
    private int masterDrawMode = DRAWMODE_INIT;
    private GuiListener guiListener;
    private final OptionsDialog options;
    private boolean calcing;
    /**
     * Vertical scroll distance in files (entries in list)
     */
    private int scrollY = 0;

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        GradientPaint grad = new GradientPaint(new Point(0, 0), new Color(192, 192, 192, 0), new Point(this.getWidth() / 2, 0), Color.LIGHT_GRAY, true);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setBackground(Color.WHITE);
        g2.clearRect(0, 0, this.getWidth(), this.getHeight());
        switch (masterDrawMode) {
            case DRAWMODE_INIT:
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, this.getWidth(), this.getHeight());
                g.setColor(Color.WHITE);
                g.setFont(Font.decode("Sans-30"));
                g.drawString("...", getWidth() / 4 * 3, getHeight() / 4 * 3);
                break;
            case DRAWMODE_HELP:
                // background
                g2.setColor(background);
                g2.fillRect(-1, -1, this.getWidth() + 1, this.getHeight() + 2);
                g2.fillRect(-1, -1, this.getWidth() + 1, this.getHeight() + 2);
                // help symbol + hover
                if (traySelected == HOVER_HELP) {
                    g2.setColor(Color.GRAY);
                } else {
                    g2.setColor(selBackground);
                }
                g2.fillRect(0, this.getHeight() - 30, 29, 30);
                g2.drawImage(help, 5, this.getHeight() - 25, this);
                // header - LanXchange
                g2.setFont(f0);
                g2.setColor(Color.BLACK);
                g2.drawString("Lan    change", this.getWidth() / 2 - mer0.stringWidth("Lan    change") / 2, 35);
                g2.drawImage(small, this.getWidth() / 2 - 31, 16, this);
                // seperator line
                g2.setPaint(grad);
                g2.drawLine(0, 55, this.getWidth(), 55);
                g2.setPaint(null);
                int xstart = 20;
                if (this.getWidth() < 375 && this.getWidth() >= 360) {
                    xstart = 10;
                } else if (this.getWidth() < 360) {
                    xstart = 5;
                }
                // howto
                g2.setColor(Color.BLACK);
                g2.setFont(f1b);
                g2.drawString("How-to: ", xstart, 80);
                g2.setFont(f1);
                g2.drawString("- to share files, drop them in this window", xstart, 105);
                g2.drawString("- start LanXchange on another device", xstart, 125);
                if (this.getWidth() >= 350) {
                    g2.drawString("- your files should show up there, click to transfer", xstart, 145);
                } else {
                    g2.drawString("- your files show up there, click to transfer", xstart, 145);
                }
                // files won't show up
                g2.setFont(f1b);
                g2.drawString("Files don't show up?", xstart, 175);
                g2.setFont(f1);
                if (this.getWidth() >= 350) {
                    g2.drawString("- made for small local networks (think: home wifi)", xstart, 200);
                    g2.drawString("- all devices must be connected at the same time", xstart, 220);
                } else {
                    g2.drawString("- made for local networks (home wifi)", xstart, 200);
                    g2.drawString("- all devices must be connected", xstart, 220);
                }
                g2.drawString("- check your firewall", xstart, 240);
                if (this.getHeight() >= 284) {
                    // seperator line
                    g2.setPaint(grad);
                    g2.drawLine(0, 253, this.getWidth(), 253);
                    g2.setPaint(null);
                }
                // mail
                g2.setColor(Color.BLACK);
                if (this.getHeight() >= 325) {
                    if (this.getWidth() >= 382) {
                        g2.drawString("Tell me what you think!", 20, 283);
                    } else {
                        g2.drawString("Mail:", 20, 283);
                    }
                    g2.setFont(f1b);
                    g2.drawString("mail@lanxchange.com", this.getWidth() - 20 - mer1b.stringWidth("mail@lanxchange.com"), 283);
                }
                // github
                if (this.getHeight() >= 360) {
                    g2.setFont(f1);
                    if (this.getWidth() >= 375) {
                        g2.drawString("Source Code:", 20, 318);
                    }
                    g2.setFont(f1b);
                    g2.drawString("github.com/tfg13/lanxchange", this.getWidth() - 20 - mer1b.stringWidth("github.com/tfg13/lanxchange"), 318);
                }
                // License
                if (this.getHeight() >= 387) {
                    g2.setFont(f1);
                    if (this.getWidth() >= 365) {
                        g2.drawString("Free Software!  License:", 20, 353);
                    } else {
                        g2.drawString("License:", 20, 353);
                    }
                    g2.setFont(f1b);
                    g2.drawString("GNU GPL v3+", this.getWidth() - 20 - mer1b.stringWidth("GNU GPL v3+"), 353);
                }
                // version
                g2.setFont(f2);
                g2.setColor(Color.BLACK);
                String versionText = LXC.versionString + "   (" + LXC.versionId + ")";
                g2.drawString(versionText, this.getWidth() - mer2.stringWidth(versionText) - 5, this.getHeight() - 18);
                // legal
                String legalText = "Copyright  2009-2016  Tobias Fleig  -  All rights reserved";
                g2.drawString(legalText, this.getWidth() - mer2.stringWidth(legalText) - 5, this.getHeight() - 6);
                break;
            case DRAWMODE_MAIN:
            default:
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setBackground(Color.WHITE);
                g2.clearRect(0, 0, this.getWidth(), this.getHeight());
                // center LXC-Logo
                g2.drawImage(logo, (this.getWidth() / 2) - (logo.getWidth(this) / 2), (this.getHeight() / 2) - (logo.getHeight(this) / 2), this);
                g2.setColor(background);
                g2.fillRect(-1, this.getHeight() - 30, this.getWidth() + 2, 30);
                g2.setPaint(grad);
                g2.drawRect(-1, this.getHeight() - 30, this.getWidth() + 2, 30);
                g2.setPaint(null);
                if (traySelected == HOVER_NONE) {
                    // Text "LanXchange" (bottom, center)
                    g2.drawImage(txt, (this.getWidth() / 2) - (txt.getWidth(this) / 2), this.getHeight() - txt.getHeight(this) - 2, this);
                }
                // selfdist button
                // hover?
                if (traySelected == HOVER_SELFDIST) {
                    g2.setColor(selBackground);
                    g2.fillRect(this.getWidth() - 59, this.getHeight() - 30, 29, 30);
                    g2.setColor(Color.GRAY);
                    g2.drawString("self distribution", this.getWidth() / 2 - (mer1.stringWidth("self distribution") / 2), this.getHeight() - 10);
                }
                g2.drawImage(selfdist_small, this.getWidth() - 55, this.getHeight() - 25, this);
                // settings button
                // hover?
                if (traySelected == HOVER_SETTINGS) {
                    g2.setColor(selBackground);
                    g2.fillRect(this.getWidth() - 30, this.getHeight() - 30, 30, 30);
                    g2.setColor(Color.GRAY);
                    g2.drawString("settings", this.getWidth() / 2 - (mer1.stringWidth("settings") / 2), this.getHeight() - 10);
                }
                g2.drawImage(screw, this.getWidth() - 25, this.getHeight() - 25, this);
                // help button
                if (traySelected == HOVER_HELP) {
                    g2.setColor(selBackground);
                    g2.fillRect(0, this.getHeight() - 30, 29, 30);
                    g2.setColor(Color.GRAY);
                    g2.drawString("help / about", this.getWidth() / 2 - (mer1.stringWidth("help / about") / 2), this.getHeight() - 10);
                }
                g2.drawImage(help, 5, this.getHeight() - 25, this);

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
                    String noFilesText1 = "files shared by other devices appear in this window";
                    g2.drawString(noFilesText1, this.getWidth() / 2 - mer2.stringWidth(noFilesText1) / 2, 40 + mer2.getAscent() + 4);
                    String noFilesText2 = "drop or paste files here to start sharing";
                    g2.drawString(noFilesText2, this.getWidth() / 2 - mer2.stringWidth(noFilesText2) / 2, 40 + mer2.getAscent() + 15);
                }
                int y = 20; // y-coordinate
                // set up clipping
                Shape clip = g2.getClip();
                g2.clip(new Rectangle(0, 20, this.getWidth(), this.getHeight() - 20 - 30));
                // file list may have changed, make sure scrollY is still within bounds
                int maxVisibleFiles = getMaxVisibleFiles();
                scrollY = Math.min(scrollY, Math.max(0, allFiles.size() - maxVisibleFiles));
                // files
                maxVisibleFiles++;
                for (int i = scrollY; i < Math.min(allFiles.size(), scrollY + maxVisibleFiles); i++) {
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
                    FontMetrics nameMetrics = mer1;
                    if (f1.canDisplayUpTo(file.getShownName()) == -1) {
                        g2.setFont(f1);
                    } else {
                        // contains chars not included in the default ubuntu font file, try to get from OS
                        g2.setFont(f1Fallback);
                        nameMetrics = mer1Fallback;
                    }
                    if (selectedIndex == i && file.isLocal()) {
                        renderCutString(file.getShownName(), (int) (1.0 * this.getWidth() * 0.7) - 49, g2, 47, y + 8 + (nameMetrics.getAscent() / 2), nameMetrics);
                    } else {
                        renderCutString(file.getShownName(), (int) (1.0 * this.getWidth() * 0.7) - 49, g2, 47, y + 14 + (nameMetrics.getAscent() / 2), nameMetrics);
                    }
                    if (!g2.getFont().equals(f1)) {
                        g2.setFont(f1);
                        nameMetrics = mer1;
                    }
                    // size
                    if (detailSelected && selectedIndex == i && file.isLocal()) {
                        g2.drawString(LXCFile.getFormattedSize(file.getFileSize()), (int) (1.0 * this.getWidth() * 0.7), y + 8 + (mer1.getAscent() / 2));
                        g2.setFont(f2);
                        g2.drawString("Click to remove", (int) (1.0 * this.getWidth() * 0.7), y + 27);
                    } else {
                        g2.drawString(LXCFile.getFormattedSize(file.getFileSize()), (int) (1.0 * this.getWidth() * 0.7), y + 14 + (mer1.getAscent() / 2));
                    }
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
                        if (file.isLocal()) {
                            g2.setFont(f2);
                            g2.setColor(Color.BLACK);
                            String downloads;
                            if (file.getNumberOfTransfers() != 1) {
                                downloads = file.getNumberOfTransfers() + " downloads";
                            } else {
                                downloads = "1 download";
                            }
                            g2.drawString(downloads, 47, y + 27);
                        }
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
                            int progress = job.getTrans().getProgress();
                            String sProg = progress + "%";
                            if (progress < 0 || progress > 100) {
                                sProg = "N/A";
                            } else {
                                // core of progress bar
                                g2.fillRect((int) (this.getWidth() * 0.65) + 2, y + 30 + o * 20 + 7, (int) (((int) (this.getWidth() * 0.3) - 9) * (progress / 100f)), 7);
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
                    g2.drawString(calcText, this.getWidth() / 2 - mer1.stringWidth(calcText) / 2, y + 9 + (mer1.getAscent() / 2));
                }
                // separator line
                if (!allFiles.isEmpty()) {
                    g2.setPaint(grad);
                    g2.drawLine(0, y, this.getWidth(), y);
                    g2.setPaint(null);
                }
                // scrollbar
                if (allFiles.size() > maxVisibleFiles - 1) {
                    // length of scrollbar
                    double visibleFraction = ((double) maxVisibleFiles - 1) / allFiles.size();
                    // position of scrollbar
                    double visibleRangeStart = ((double) scrollY) / allFiles.size();
                    // draw
                    int visAreaHeight = (this.getHeight() - 20 - 30);
                    int scrollbarStart = (int) (visAreaHeight * visibleRangeStart + 20);
                    int scrollbarLength = (int) (visAreaHeight * visibleFraction);
                    g2.setColor(Color.BLACK);
                    g2.drawLine(this.getWidth() - 2, scrollbarStart, this.getWidth() - 2, scrollbarStart + scrollbarLength);
                    g2.drawLine(this.getWidth() - 3, scrollbarStart, this.getWidth() - 3, scrollbarStart + scrollbarLength);
                }
                // reset clip
                g2.setClip(clip);
                break;
        }
    }

    private int getMaxVisibleFiles() {
        int maxPixel = LXCPanel.this.getHeight() - 20 - 30;
        int total = 0;
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
            total += plus;
            if (total > maxPixel) {
                return i;
            }
        }
        return allFiles.size();
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

    private Image loadImg(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException ex) {
            System.out.println("ERROR loading image \"" + path + "\". Re-Download LanXchange to fix.");
            // create 1x1 transparent image as replacement
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, new java.awt.Color(0, 0, 0, 0).getRGB());
            return image;
        }
    }

    /**
     * "Starts" the Panel.
     * Must be called only once.
     */
    public void start() {
        logo = loadImg("img/logo.png");
        mini = loadImg("img/mini.png");
        small = loadImg("img/small.png");
        harddisk = loadImg("img/harddisk.png");
        txt = loadImg("img/txt.png");
        fileImg = loadImg("img/file.png");
        folder = loadImg("img/folder.png");
        multi = loadImg("img/multiple.png");
        delete = loadImg("img/del.png");
        busy = loadImg("img/busy.png");
        done = loadImg("img/done.png");
        help = loadImg("img/help.png");
        screw = loadImg("img/screw.png");
        download = loadImg("img/download.png");
        cancel = loadImg("img/cancel.png");
        selfdist_small = loadImg("img/selfdist_small.png");
        mer0 = this.getGraphics().getFontMetrics(f0);
        mer1 = this.getGraphics().getFontMetrics(f1);
        mer1Fallback = this.getGraphics().getFontMetrics(f1Fallback);
        mer1b = this.getGraphics().getFontMetrics(f1b);
        mer2 = this.getGraphics().getFontMetrics(f2);
        urlClickableCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        masterDrawMode = DRAWMODE_MAIN;
        this.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                switch (masterDrawMode) {
                    case DRAWMODE_INIT:
                        // ignore input in this mode
                        break;
                    case DRAWMODE_HELP:
                        // right click exits
                        if (e.getButton() == 3) {
                            masterDrawMode = DRAWMODE_MAIN;
                            LXCPanel.this.setCursor(Cursor.getDefaultCursor());
                        } else {
                            // close window?
                            if (e.getX() < 30 && e.getY() > LXCPanel.this.getHeight() - 30) {
                                masterDrawMode = DRAWMODE_MAIN;
                                LXCPanel.this.setCursor(Cursor.getDefaultCursor());
                            }
                            // click mail, github, license?
                            if (e.getY() >= 260 && e.getY() < 295) {
                                // mail
                                DesktopInteractionHelper.sendMail("mailto:mail@lanxchange.com");
                            } else if (e.getY() >= 295 && e.getY() < 330) {
                                // github
                                DesktopInteractionHelper.openURL("https://github.com/tfg13/LanXchange");
                            } else if (e.getY() >= 330 && e.getY() < LXCPanel.this.getHeight() - 30) {
                                // gpl
                                DesktopInteractionHelper.openURL("https://www.gnu.org/licenses/gpl-3.0.en.html");
                            }
                        }
                        break;
                    case DRAWMODE_MAIN:
                    default:
                        if (e.getX() > LXCPanel.this.getWidth() - 55 && e.getX() < LXCPanel.this.getWidth() - 25 && e.getY() > LXCPanel.this.getHeight() - 25) {
                            // display selfdist
                            SelfDistributor.showGui((JFrame) SwingUtilities.getRoot(LXCPanel.this));
                        } else if (e.getX() > LXCPanel.this.getWidth() - 25 && e.getY() > LXCPanel.this.getHeight() - 25) {
                            // display settings
                            options.showAndWait();
                            guiListener.reloadConfiguration();
                        } else if (e.getX() < 25 && e.getY() > LXCPanel.this.getHeight() - 25) {
                            // open help
                            masterDrawMode = DRAWMODE_HELP;
                        } else if (e.getButton() == 1 && detailSelected && selectedIndex != -1 && selectedIndex < allFiles.size() && allFiles.get(selectedIndex).isLocal()) { // delete?
                            try {
                                LXCFile file = allFiles.get(selectedIndex);
                                if (file.isLocal()) {
                                    guiListener.removeFile(file);
                                }
                            } catch (Exception ex) {
                            }
                        } else if (e.getButton() == 1 && subJobDeleteSelected != -1 && selectedIndex != -1) { // cancel download
                            // Search job and cancel it
                            allFiles.get(selectedIndex).getJobs().get(subJobDeleteSelected).abortTransfer();
                        } else if (selectedIndex > -1 && selectedIndex < allFiles.size()) {
                            // simple click
                            final LXCFile file = allFiles.get(selectedIndex);
                            if (e.getButton() == 1 && !file.isLocked() && !file.isLocal() && !file.isAvailable()) {
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
                            } else if (!file.isLocked() && !file.isLocal() && file.isAvailable()) {
                                if (e.getButton() == 1) {
                                    // try to open the file
                                    VirtualFile vFile = file.getFiles().get(0);
                                    if (vFile instanceof RealFile) {
                                        RealFile rFile = (RealFile) vFile;
                                        DesktopInteractionHelper.openFile(rFile.getBackingFile());
                                    }
                                } else if (e.getButton() == 3) {
                                    // reset file. (disappears or allows re-download)
                                    guiListener.resetFile(file);
                                }
                            }
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
                switch (masterDrawMode) {
                    case DRAWMODE_INIT:
                        // ignore
                        break;
                    case DRAWMODE_HELP:
                        // hover help button
                        if (e.getX() < 25 && e.getY() > LXCPanel.this.getHeight() - 25) {
                            if (traySelected != HOVER_HELP) {
                                traySelected = HOVER_HELP;
                                selfTrigger();
                            }
                        } else {
                            if (traySelected != HOVER_NONE) {
                                traySelected = HOVER_NONE;
                                selfTrigger();
                            }
                        }
                        if (e.getY() >= 260 && e.getY() <= LXCPanel.this.getHeight() - 25) {
                            if (!LXCPanel.this.getCursor().equals(urlClickableCursor)) {
                                LXCPanel.this.setCursor(urlClickableCursor);
                            }
                        } else {
                            if (!LXCPanel.this.getCursor().equals(Cursor.getDefaultCursor())) {
                                LXCPanel.this.setCursor(Cursor.getDefaultCursor());
                            }
                        }
                        break;
                    case DRAWMODE_MAIN:
                    default:
                        // help-button?
                        if (e.getX() < 25 && e.getY() > LXCPanel.this.getHeight() - 25) {
                            if (traySelected != HOVER_HELP) {
                                traySelected = HOVER_HELP;
                                selfTrigger();
                            }
                        } else if (e.getX() > LXCPanel.this.getWidth() - 55 && e.getX() < LXCPanel.this.getWidth() - 25 && e.getY() > LXCPanel.this.getHeight() - 25) {
                            // self-distribution button
                            if (traySelected != HOVER_SELFDIST) {
                                traySelected = HOVER_SELFDIST;
                                selfTrigger();
                            }
                        } else if (e.getX() > LXCPanel.this.getWidth() - 25 && e.getY() > LXCPanel.this.getHeight() - 25) {
                            // settings button
                            if (traySelected != HOVER_SETTINGS) {
                                traySelected = HOVER_SETTINGS;
                                selfTrigger();
                            }
                        } else {
                            // turn off hovers
                            if (traySelected != -1) {
                                traySelected = -1;
                                selfTrigger();
                            }
                            int newSelIndex = scrollY;
                            int my = e.getY();
                            int pre = 20;
                            int prev = 20;
                            if (my < pre) {
                                newSelIndex = -1;
                            } else {
                                // Add file after file until we get there
                                int maxVisibleFiles = LXCPanel.this.getMaxVisibleFiles();
                                for (int i = scrollY; i < Math.min(allFiles.size(), scrollY + maxVisibleFiles + 1); i++) {
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
            }
        });
        this.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (masterDrawMode == DRAWMODE_MAIN) {
                    if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                        int maxVisibleFiles = getMaxVisibleFiles();
                        int maxScrollY = allFiles.size() - maxVisibleFiles;
                        int delta = 0;
                        if (e.getPreciseWheelRotation() > 0) {
                            delta = 1;
                        } else if (e.getPreciseWheelRotation() < 0) {
                            delta = -1;
                        }
                        int newScrollY = scrollY + delta;
                        if (newScrollY >= 0 && newScrollY <= maxScrollY) {
                            if (newScrollY != scrollY) {
                                selfTrigger();
                            }
                            scrollY = newScrollY;
                        }
                    }
                }
            }
        });
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
     * Sets the file list.
     *
     * @param fileList fileList
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
     * Will fall back to system provided "sans serif" font
     * for filenames with unsupported characters.
     * (unsupported = default ubuntu font file does not include them)
     *
     * @param font font to use to display text
     */
    void setUsedFont(Font font) {
        f0 = font.deriveFont(Font.BOLD, 20f);
        f1 = font.deriveFont(15f);
        f1Fallback = Font.decode(Font.SANS_SERIF); // OS-supplied font with support for more chars
        f1b = f1.deriveFont(Font.BOLD);
        f2 = font.deriveFont(10f);
    }
}
