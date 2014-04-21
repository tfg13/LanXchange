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
package de.tobifleig.lxc.plaf.impl.swing;

import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.GuiListener;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.JFileChooser;
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
     * Creates new form LXCGui3
     */
    public SwingGui() {
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
            ubuFont = Font.createFont(Font.TRUETYPE_FONT, new File("Ubuntu-R.ttf"));
        } catch (FontFormatException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (ubuFont == null) {
            System.out.println("WARNING: Cannot find fontfile \"Ubuntu-R.ttf\"!");
            ubuFont = Font.decode("Sans");
        }
        initComponents();
    }

    private void start() {
        guiTimer = new Timer();
        guiTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (schedTrigger) {
                    // fps?
                    if (fullFps) {
                        if (System.currentTimeMillis() - lastRepaint > 100) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    panel.repaint();
                                }
                            });
                            lastRepaint = System.currentTimeMillis();
                            schedTrigger = false;
                        }
                    } else {
                        if (System.currentTimeMillis() - lastRepaint > 1000) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    panel.repaint();
                                }
                            });
                            lastRepaint = System.currentTimeMillis();
                            schedTrigger = false;
                        }
                    }
                }
            }
        }, 3000, 100);

        this.addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                if (e.getNewState() != WindowEvent.WINDOW_ICONIFIED) {
                    fullFps = true;
                } else {
                    fullFps = false;
                }
            }
        });
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

        panel = new de.tobifleig.lxc.plaf.impl.swing.LXCPanel();

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
    public de.tobifleig.lxc.plaf.impl.swing.LXCPanel panel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void init(String[] args) {
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
                if (listener.shutdown(false, true)) {
                    // swing sometimes refuses to shutdown correctly
                    System.exit(0);
                }
            }
        });
    }

    @Override
    public void display() {
        panel.setFileList(listener.getFileList());
        panel.setUsedFont(ubuFont);
        setVisible(true);
        panel.start();
        start();
    }

    @Override
    public void showError(String error) {
        JOptionPane.showMessageDialog(this, error, "LXC - Error", JOptionPane.ERROR_MESSAGE);
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
    public File getFileTarget(LXCFile file) {
        JFileChooser cf = new JFileChooser();
        cf.setApproveButtonText("Choose target");
        cf.setApproveButtonToolTipText("Download files into selected directory");
        cf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        cf.setMultiSelectionEnabled(false);
        cf.setDialogTitle("Target directory for \"" + file.getShownName() + "\"");
        int chooseResult = cf.showDialog(this, null);
        if (chooseResult == JFileChooser.APPROVE_OPTION) {
            if (cf.getSelectedFile().canWrite()) {
                return cf.getSelectedFile();
            } else {
                // inform user
                showError("Cannot write there, please selected another target or start LXC as Administrator");
                // cancel
                System.out.println("Canceled, cannot write (permission denied)");
                return null;
            }
        } else {
            // cancel
            System.out.println("Canceled by user.");
            return null;
        }
    }

    @Override
    public boolean confirmCloseWithTransfersRunning() {
        return (JOptionPane.showConfirmDialog(rootPane, "Exiting now will kill all running transfers. Quit anyway?", "Transfers running", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION);
    }
}
