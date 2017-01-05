/*
 * Copyright 2017 Tobias Fleig (tobifleig gmail com)
 * All rights reserved.
 *
 * Usage of this source code is governed by the GNU GENERAL PUBLIC LICENSE version 3 or (at your option) any later version.
 * For the full license text see the file COPYING or https://github.com/tfg13/LanXchange/blob/master/COPYING
 */
package de.tobifleig.lxc.plaf.cli;

import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.plaf.GuiInterface;
import de.tobifleig.lxc.plaf.GuiListener;
import de.tobifleig.lxc.plaf.cli.cmds.ListCommand;
import de.tobifleig.lxc.plaf.cli.cmds.StartCommand;
import de.tobifleig.lxc.plaf.cli.cmds.StopCommand;
import de.tobifleig.lxc.plaf.cli.ui.CLITools;
import de.tobifleig.lxc.plaf.cli.ui.ListPrinter;
import de.tobifleig.lxc.plaf.pc.PCPlatform;
import de.tobifleig.lxc.plaf.pc.UpdaterGui;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The CLI is non-interactive.
 * This backend hosts the LanXchange core, runs in the background and awaits commands.
 */
public class CLIBackend extends PCPlatform {

    /**
     * Port used for frontend backend communication
     */
    public static final int LANXCHANGE_DAEMON_PORT = 27718;

    /**
     * If a stop command (without force parameter) is
     * received twice within this time limit, auto force.
     */
    private static final int DOUBLE_STOP_AUTOFORCE_THRESHOLD = 10000;

    private GuiListener listener;

    /**
     * Flag that indicates if the server mainloop must quit.
     */
    private boolean stopServer = false;

    private long lastStopTime = 0;

    public CLIBackend(String[] args) {
        super(args);

        LXC lxc = new LXC(this, args);

        receiveLoop();
    }

    public static void main(String[] args) {
        // this should only be called by lxcc (the cli launch script)
        if (args.length == 0 || !args[0].equals("-cli")) {
            CLITools.out.println("Please use the script \"lxcc\" to interact with the CLI version of LanXchange. " +
                    "If you don't know what this means, you probably want to run \"lxc\"");
            return;
        }

        // replace "-cli" with "-managed" for core/the updater
        String[] argsCopy = new String[args.length];
        System.arraycopy(args, 0, argsCopy, 0, args.length);
        argsCopy[0] = "-managed";
        new CLIBackend(argsCopy);
    }

    @Override
    public GuiInterface getGui(String[] args) {
        return new GuiInterface() {
            @Override
            public void init() {
                // ignore
            }

            @Override
            public void display() {
                // ignore
            }

            @Override
            public void showError(String error) {
                CLITools.out.println("Error: " + error);
            }

            @Override
            public void setGuiListener(GuiListener guiListener) {
                CLIBackend.this.listener = guiListener;
            }

            @Override
            public void update() {
                // ignore
            }

            @Override
            public void notifyFileChange(int fileOrigin, int operation, int firstIndex, int numberOfFiles) {
                // ignore
            }

            @Override
            public void notifyJobChange(int operation, LXCFile file, int index) {
                // ignore
            }

            @Override
            public File getFileTarget(LXCFile file) {
                //TODO
                throw new UnsupportedOperationException("not implemented yet");
            }

            @Override
            public boolean confirmCloseWithTransfersRunning() {
                // TODO think about this
                return false;
            }
        };
    }

    @Override
    public void downloadComplete(LXCFile file, File targetFolder) {

    }

    @Override
    public void showEarlyError(String error) {

    }

    @Override
    public UpdaterGui getUpdaterGui() {
        return null;
    }

    private void handleCommand(BackendCommand command) {
        // try to cast
        switch (command.getType()) {
            case START:
                handleStart((StartCommand) command);
                break;
            case STOP:
                handleStop((StopCommand) command);
                break;
            case LIST:
                handleList((ListCommand) command);
                break;
            default:
                // front end should never send invalid commands
                throw new AssertionError("unexpected command!");
        }
    }

    private void handleList(ListCommand command) {
        ListPrinter.printList(listener.getFileList());
    }

    private void handleStart(StartCommand startCommand) {
        if (!startCommand.isBackendLaunched()) {
            CLITools.out.println("Backend already running");
        }
    }

    private void handleStop(StopCommand stopCommand) {
        boolean force = stopCommand.isForce();
        if (!force && (System.currentTimeMillis() - lastStopTime) < DOUBLE_STOP_AUTOFORCE_THRESHOLD) {
            // auto force
            CLITools.out.println("Received STOP command again withing 10s, aborting all transfers.");
            force = true;
        }

        stopServer = listener.shutdown(force, false, false);
        if (!stopServer) {
            CLITools.out.println("Aborted STOP command because transfers are running in the background." +
                    "Re-send STOP withing 10s to force-quit LanXchange and abort all running transfers.");
        }
    }

    private void receiveLoop() {
        try (ServerSocket serverSocket = new ServerSocket(LANXCHANGE_DAEMON_PORT)) {

            CLITools.out.println("LanXchange is now running in the background. Stop with \"lxcc stop\"");
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    if (!clientSocket.getInetAddress().isLoopbackAddress()) {
                        // do not accept connections from the outside
                        clientSocket.close();
                    }
                    try (ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream())) {
                        BackendCommand command = (BackendCommand) input.readObject();
                        handleCommand(command);
                    } catch (IOException | ClassNotFoundException | ClassCastException ex) {
                        // TODO handle exception during communication with this client
                    }
                    if (stopServer) {
                        break;
                    }
                } catch (IOException ex) {
                    // abort loop if exception happens during serverSocket.accept()
                    break;
                }
            }

        } catch (IOException ex) {
            // TODO exception during socket setup
        }
        CLITools.out.println("LanXchange background daemon stopped.");
    }
}
