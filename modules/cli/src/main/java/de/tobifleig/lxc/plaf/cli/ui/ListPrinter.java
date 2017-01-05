package de.tobifleig.lxc.plaf.cli.ui;

import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.data.LXCFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core part of the UI, prints all files and ongoing transfers.
 */
public final class ListPrinter {

    private static final String TEMPLATE_FILENAME = "                              "; // 30 spaces

    private ListPrinter() {}

    public static void printList(List<LXCFile> files) {
        CLITools.out.println("LanXchange " + LXC.versionString + " (" + LXC.versionId + ") running.");

        List<LXCFile> ownFiles = files.stream().filter(f -> f.isLocal()).collect(Collectors.toList());
        List<LXCFile> remoteFiles = files.stream().filter(f -> !f.isLocal()).collect(Collectors.toList());

        if (ownFiles.isEmpty()) {
            CLITools.out.println("Not sharing any files.");
        } else {
            CLITools.out.println("Shared files:");
            printHeader();
            ownFiles.forEach(f -> {
                CLITools.out.print("  ");
                CLITools.out.print(formatId(f.id));
                CLITools.out.print("  ");
                CLITools.out.print(formatName(f.getShownName()));
                CLITools.out.print("  ");
                CLITools.out.print(LXCFile.getFormattedSize(f.getFileSize()));
                CLITools.out.println();
            });
        }

        if (remoteFiles.isEmpty()) {
            CLITools.out.println("No remote files found (yet).");
        } else {
            CLITools.out.println("Available files:");
            printHeader();
            remoteFiles.forEach(f -> {
                CLITools.out.print("  ");
                CLITools.out.print(formatId(f.id));
                CLITools.out.print("  ");
                CLITools.out.print(formatName(f.getShownName()));
                CLITools.out.print("  ");
                CLITools.out.print(LXCFile.getFormattedSize(f.getFileSize()));
                CLITools.out.println();
            });
        }
    }

    /**
     * Creates a constant-length string from the given file id
     */
    private static String formatId(long fileId) {
        // contact db?
        return "1   ";
    }

    /**
     * Creates a constant-length string from the given file name
     */
    private static String formatName(String name) {
        // ensure 30 chars length
        String result = (name + TEMPLATE_FILENAME).substring(0, 30);
        // insert "..." if name is too long
        if (name.length() > 30) {
            result = result.substring(0, 27) + "...";
        }
        return result;
    }

    private static void printHeader() {
        CLITools.out.println("  -ID-  -----------FILENAME-----------  ---SIZE---");
    }
}
