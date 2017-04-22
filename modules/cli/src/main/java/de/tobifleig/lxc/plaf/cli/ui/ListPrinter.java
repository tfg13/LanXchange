package de.tobifleig.lxc.plaf.cli.ui;

import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.data.LXCFile;
import de.tobifleig.lxc.log.LXCLogBackend;
import de.tobifleig.lxc.log.LXCLogger;
import de.tobifleig.lxc.plaf.cli.LocalFileIDManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core part of the UI, prints all files and ongoing transfers.
 */
public class ListPrinter {

    private static final String TEMPLATE_FILENAME = "                              "; // 30 spaces
    private static final String TEMPLATE_ID = "    "; // 4 spaces

    private final LXCLogger logger;

    private LocalFileIDManager idManager;

    public ListPrinter(LocalFileIDManager idManager) {
        logger = LXCLogBackend.getLogger("cli-listprinter");
        this.idManager = idManager;
    }

    public void printList(List<LXCFile> files) {
        List<LXCFile> ownFiles = files.stream().filter(f -> f.isLocal()).collect(Collectors.toList());
        List<LXCFile> remoteFiles = files.stream().filter(f -> !f.isLocal()).collect(Collectors.toList());

        if (ownFiles.isEmpty()) {
            CLITools.out.println("Not sharing any files from this machine.");
        } else {
            CLITools.out.println("Shared files:");
            printHeader();
            ownFiles.forEach(f -> {
                CLITools.out.print("  ");
                CLITools.out.print(formatId(f));
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
                CLITools.out.print(formatId(f));
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
    private String formatId(LXCFile file) {
        int id = idManager.getId(file);
        if (id < 0) {
            // this should never ever happen
            logger.wtf("Error: No ID for file " + file);
            id = -1;
        }
        String idString = String.valueOf(id);
        // set length to 4, fill with whitespace
        return (idString + TEMPLATE_ID).substring(0, 4);
    }

    /**
     * Creates a constant-length string from the given file name
     */
    private String formatName(String name) {
        // ensure 30 chars length
        String result = (name + TEMPLATE_FILENAME).substring(0, 30);
        // insert "..." if name is too long
        if (name.length() > 30) {
            result = result.substring(0, 27) + "...";
        }
        return result;
    }

    private void printHeader() {
        CLITools.out.println("  -ID-  -----------FILENAME-----------  ---SIZE---");
    }
}
