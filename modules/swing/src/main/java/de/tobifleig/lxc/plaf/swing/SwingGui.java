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

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.GuiListener;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;

/**
 * The GUI
 *
 * @author Tobias Fleig <tobifleig googlemail com>
 */
public class SwingGui extends javax.swing.JFrame implements GuiInterface {

    private static final long serialVersionUID = 1L;
    /**
     * Timer for repaints.
     */
    private Timer guiTimer;
    /**
     * Currently scheduled timer task, if any.
     */
    private TimerTask timerTask;
    /**
     * Triggers the repaint-scheduler.
     */
    private boolean schedTrigger;
    /**
     * Time of last repaint.
     */
    private long lastRepaint;
    /**
     * Window in foreground, display at full fps.
     */
    private boolean fullFps = true;
    /**
     * The font to use for all text-rendering (guarantees a pixel-perfect look on all platforms)
     */
    private Font ubuFont;
    /**
     * The listener.
     */
    private GuiListener listener;
    /**
     * Overall progress manager, manages global progress displays like windows taskbar.
     */
    private OverallProgressManager progressManager;
    /**
     * The platform we are running on.
     */
    private final GenericPCPlatform platform;

    /**
     * Creates new form LXCGui3
     */
    public SwingGui(GenericPCPlatform platform) {
        this.platform = platform;
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    // set look and feel
                    try {
                        // Do not set L&F on linux, on some systems (ubuntu) this gets you a very ugly GTK file chooser
                        if (!System.getProperty("os.name").toLowerCase().equals("linux")) {
                            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    try {
                        InputStream ubuFontRes = ClassLoader.getSystemClassLoader().getResourceAsStream("Ubuntu-R.ttf");
                        if (ubuFontRes != null) {
                            ubuFont = Font.createFont(Font.TRUETYPE_FONT, ubuFontRes);
                        } else {
                            // try file
                            ubuFont = Font.createFont(Font.TRUETYPE_FONT, new File("Ubuntu-R.ttf"));
                        }

                    } catch (FontFormatException | IOException ex) {
                        ex.printStackTrace();
                    }
                    if (ubuFont == null) {
                        System.out.println("WARNING: Cannot find fontfile \"Ubuntu-R.ttf\"!");
                        ubuFont = Font.decode("Sans");
                    }
                    initComponents();
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
        progressManager = new OverallProgressManager() {
            @Override
            public void notifyOverallProgressChanged(int percentage) {
                // ignored
            }

            @Override
            public void notifySingleProgressChanged() {
                schedTrigger = true;
            }
        };
    }

    private void start() {
        guiTimer = new Timer();
        // start in fullFps mode
        fullFps = true;
        reConfigureTimer();

        this.addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                // reschedule timer, if required
                boolean oldFps = fullFps;
                fullFps = (e.getNewState() & Frame.ICONIFIED) == 0;
                if (fullFps != oldFps) {
                    reConfigureTimer();
                }
            }
        });
    }

    private void reConfigureTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        final int delay = fullFps ? 100 : 1000;

        timerTask = new TimerTask() {
            @Override
            public void run() {
                // always render if requested or 10x delay has passed
                if (schedTrigger || (System.currentTimeMillis() - lastRepaint) > (10 * delay)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            panel.repaint();
                        }
                    });
                    schedTrigger = false;
                    lastRepaint = System.currentTimeMillis();
                }
            }
        };

        guiTimer.schedule(timerTask, delay, delay);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panel = new de.tobifleig.lxc.plaf.swing.LXCPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("LanXchange");
        setMinimumSize(new java.awt.Dimension(308, 298));
        getContentPane().setLayout(new javax.swing.BoxLayout(getContentPane(), javax.swing.BoxLayout.LINE_AXIS));

        panel.setLayout(null);
        getContentPane().add(panel);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-410)/2, (screenSize.height-430)/2, 410, 430);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public de.tobifleig.lxc.plaf.swing.LXCPanel panel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void init(String[] args) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    // setup dnd
                    panel.setTransferHandler(new DropTransferHandler(new FileDropListener() {
                        @Override
                        public void displayCalcing() {
                            panel.setCalcing(true);
                        }

                        @Override
                        public void newCalcedFile(LXCFile file) {
                            panel.setCalcing(false);
                            listener.offerFile(file);
                        }

                        @Override
                        public String generateUniqueFileName(String base, String extension) {
                            return listener.generateUniqueFileName(base, extension);
                        }
                    }));
                    // setup pasting
                    panel.getActionMap().put(TransferHandler.getPasteAction().getValue(Action.NAME), TransferHandler.getPasteAction());
                    panel.getInputMap().put(KeyStroke.getKeyStroke("ctrl V"), TransferHandler.getPasteAction().getValue(Action.NAME));
                    try {
                        setIconImage(ImageIO.read(new File("img/logo.png")));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            if (listener.shutdown(false, true, true)) {
                                // swing sometimes refuses to shutdown correctly
                                System.exit(0);
                            }
                        }
                    });
                    // setup ctrl-a, ctrl-o shortcut for file sharing
                    panel.getInputMap().put(KeyStroke.getKeyStroke("ctrl A"), "selectShare");
                    panel.getInputMap().put(KeyStroke.getKeyStroke("ctrl O"), "selectShare");
                    panel.getActionMap().put("selectShare", new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            final File[] files = platform.openFileForSharing();
                            if (files != null && files.length > 0) {
                                for (File file : files) {
                                    if (!file.canRead()) {
                                        System.out.println("Aborting preparation for new files to share, cannot read file \"" + file.getAbsolutePath() + "\"");
                                        // showError() cannot must not called from event dispatcher thread
                                        JOptionPane.showMessageDialog(SwingGui.this, "Cannot read at least one of the selected files:\n\"" + file.getAbsolutePath() + "\"", "LXC - Error", JOptionPane.ERROR_MESSAGE);
                                        return;
                                    }
                                }
                                // create LXCFile in new thread, constructor blocks until size-calcing is finished!
                                final File first = files[0];
                                Thread thread = new Thread(new Runnable() {

                                    @Override
                                    public void run() {
                                        panel.setCalcing(true);
                                        LXCFile newFile = new LXCFile(LXCFile.convertToVirtual(Arrays.asList(files)), first.getName());
                                        panel.setCalcing(false);
                                        listener.offerFile(newFile);
                                    }
                                }, "lxc_helper_sizecalcer");
                                thread.setPriority(Thread.NORM_PRIORITY - 1);
                                thread.start();
                            } // adding files aborted, just ignore
                        }
                    });
                    // close help window on ESC
                    panel.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeHelp");
                    panel.getActionMap().put("closeHelp", new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            panel.closeHelp();
                        }
                    });
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void display() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    panel.setFileList(listener.getFileList());
                    panel.setUsedFont(ubuFont);
                    setVisible(true);
                    panel.start();
                    start();
                    update();
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void showError(final String error) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    JOptionPane.showMessageDialog(SwingGui.this, error, "LXC - Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (InterruptedException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void setGuiListener(GuiListener listener) {
        this.listener = listener;
        panel.setGuiListener(listener);
    }

    @Override
    public void update() {
        schedTrigger = true;
    }

    @Override
    public void notifyFileChange(int fileOrigin, int operation, int firstIndex, int numberOfFiles) {
        // swing gui is not interested in all this information
        update();
    }

    @Override
    public void notifyJobChange(int operation, LXCFile file, int index) {
        if (operation == GuiInterface.UPDATE_OPERATION_ADD) {
            progressManager.handleNewJob(file.getJobs().get(index));
        } else if (operation == GuiInterface.UPDATE_OPERATION_REMOVE) {
            progressManager.removeJob(file.getJobs().get(index));
        }
    }

    @Override
    public File getFileTarget(LXCFile file) {
        // platform handles this
        return platform.getFileTarget(file);
    }

    @Override
    public boolean confirmCloseWithTransfersRunning() {
        return (JOptionPane.showConfirmDialog(rootPane, "Exiting now will kill all running transfers. Quit anyway?", "Transfers running", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION);
    }

    public void setOverallProgressManager(OverallProgressManager progressManager) {
        this.progressManager = progressManager;
    }
}
