package de.tobifleig.lxc.plaf.cli;

import de.tobifleig.lxc.plaf.cli.cmds.StartCommand;
import de.tobifleig.lxc.plaf.cli.cmds.StopCommand;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * The frontend, mostly submits commands to the backend.
 */
public class CLIFrontend {

    private static int EXITCODE_REQUEST_DAEMON = 7;

    public static void main(String[] args) {
        // this should only be called by lxcc (the cli launch script)
        if (args.length == 0 || !args[0].equals("-cli")) {
            CLITools.out.println("Please use the script \"lxcc\" to interact with the CLI version of LanXchange. " +
                    "If you don't know what this means, you probably want to run \"lxc\"");
            return;
        }

        boolean startRequired = false;
        boolean alreadyStarted = false;

        if (args.length > 1 && args[1].equals("bl")) {
            alreadyStarted = true;
        }

        // strip "-cli", optional "bl" from args
        String[] oldArgs = args;
        args = new String[args.length - (alreadyStarted ? 2 : 1)];
        System.arraycopy(oldArgs, alreadyStarted ? 2 : 1, args, 0, args.length);

        if (args.length == 0) {
            CLITools.out.println("Please specify a command or use \"help\" as a starter");
            return;
        }

        // parse commands

        BackendCommand command = null;
        switch (args[0].toLowerCase()) {
            case "start":
            case "launch":
            case "run":
                command = parseStart(args, alreadyStarted);
                break;
            case "stop":
            case "quit":
            case "exit":
                command = parseStop(args);
                break;
            case "help":
            case "h":
            case "?":
                printHelp();
                break;
            default:
                CLITools.out.println("Unknown command. Use \"help\" for a list of all supported commands");
        }

        // try to send this command (will only work if the backend is already running)
        if (command != null) {
            CLITools.out.println("DEBUG: Launching command now!");
            if (!connectAndPushCommand(command)) {
                // did not work, try again after backend was started
                startRequired = true;
            }
        }

        if (startRequired) {
            if (alreadyStarted) {
                // TODO error
                CLITools.out.println("Unable to connect to backend!");
            } else {
                // request restart?
                if (command == null || command.startBackendOnSendError()) {
                    CLITools.out.println("DEBUG: Failure, requesting backend start...");
                    System.exit(7);
                } else {
                    CLITools.out.println("DEBUG: Unable to deliver command, but this is ok.");
                }
            }
        }
    }

    private static boolean connectAndPushCommand(BackendCommand command) {
        // try to connect to the backend.
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), CLIBackend.LANXCHANGE_DAEMON_PORT)) {
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            output.writeObject(command);
            output.flush();
            CLITools.out.println("Command submission to backend OK");
            return true;
        } catch (IOException e) {
            // TODO handle exception during connection/delivery attempt
            return false;
        }
    }

    private static BackendCommand parseStart(String[] data, boolean alreadyStarted) {
        if (verifyCommandLength(data, 0, 0)) {
            return new StartCommand(alreadyStarted);
        }
        return null;
    }

    private static BackendCommand parseStop(String[] data) {
        if (verifyCommandLength(data, 0, 1)) {
            if (data.length == 1) {
                return new StopCommand(false);
            } else { // length is guaranteed 2
                if (data[1].toLowerCase().equals("force")) {
                    return new StopCommand(true);
                } else {
                    CLITools.out.println("Invalid optional parameter for STOP command." +
                            "The only accepted parameter is \"force\"");
                }
            }
        }
        return null;
    }

    private static void printHelp() {
        // TODO
        CLITools.out.println("help will be added in the future!");
    }

    private static boolean verifyCommandLength(String[] args, int minNumAdditionalParams, int maxNumAdditionalParams) {
        int additionalParams = args.length - 1; // do not consider command itself
        if (additionalParams < minNumAdditionalParams || additionalParams > maxNumAdditionalParams) {
            if (minNumAdditionalParams == maxNumAdditionalParams) {
                CLITools.out.println("Invalid number of additional parameters for this command. Expected " +
                        minNumAdditionalParams + ", but got" + additionalParams);
            } else {
                CLITools.out.println("Invalid number of additional parameters for this command. Expected between" +
                        minNumAdditionalParams + " and " + maxNumAdditionalParams + ", but got" + additionalParams);
            }
            return false;
        }
        return true;
    }


}
