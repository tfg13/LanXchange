package de.tobifleig.lxc.packaging;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import static de.tobifleig.lxc.packaging.PackagingTestCommon.*;

public class DesktopReleaseTest {

    private static final String desktopRelease = "releases/stable/lxc.zip";

    private static File tempDir;

    static File[] coreFiles = new File[]{
            new File("3rd_party_licenses", "font_license.txt"),
            new File("img", "busy.png"),
            new File("img", "cancel.png"),
            new File("img", "del.png"),
            new File("img", "done.png"),
            new File("img", "download.png"),
            new File("img", "drop.png"),
            new File("img", "file.png"),
            new File("img", "folder.png"),
            new File("img", "harddisk.png"),
            new File("img", "help.png"),
            new File("img", "logo.png"),
            new File("img", "mini.png"),
            new File("img", "multiple.png"),
            new File("img", "plus.png"),
            new File("img", "screw.png"),
            new File("img", "small.png"),
            new File("img", "stop.png"),
            new File("img", "txt.png"),
            new File("img", "update.png"),
            new File("COPYING"),
            new File("lanxchange.jar"),
            new File("lxc"),
    };

    static File[] additionalFiles = new File[]{
            new File("lxc.exe"),
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        basicChecks(new File("."), new File(desktopRelease));
        // extract to allow inspection
        DesktopReleaseTest.tempDir = Files.createTempDirectory("lxc_packaging_tests_desktop").toFile();
        extractZipFile(DesktopReleaseTest.tempDir, desktopRelease);
    }

    @Test
    public void TestDesktopRelease() {
        ArrayList<File> expectedFiles = new ArrayList<>();
        expectedFiles.addAll(Arrays.asList(coreFiles));
        expectedFiles.addAll(Arrays.asList(additionalFiles));
        for (File file : expectedFiles) {
            basicChecks(DesktopReleaseTest.tempDir, file);
        }

        noOtherFiles(DesktopReleaseTest.tempDir, expectedFiles);
    }

    @Test
    public void DesktopJar() throws IOException {
        validateMainJar(new File(DesktopReleaseTest.tempDir, "lanxchange.jar"));
    }

    @AfterClass
    public static void afterClass() {
        delRec(DesktopReleaseTest.tempDir);
    }
}
