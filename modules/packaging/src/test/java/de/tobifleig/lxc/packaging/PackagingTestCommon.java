package de.tobifleig.lxc.packaging;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

public final class PackagingTestCommon {

    static File[] jarFiles = new File[]{
            // just pick one file from each dependency and gradle module
            // as a general check if something was copied
            new File("com/sun/jna", "Platform.class"),
            new File("de/tobifleig/lxc", "LXC.class"),
            new File("de/tobifleig/lxc/plaf/swing", "Main.class"),
            new File("de/tobifleig/lxc/plaf/pc", "LXCUpdater.class"),
            new File("META-INF", "MANIFEST.MF"),
            new File("win32-x86", "lxcwin.dll"),
            new File("win32-x86-64", "lxcwin.dll"),
            new File("lxc_updates.pub"),
            new File("Ubuntu-R.ttf"),
    };

    /**
     * Basic checks for a file. It should exist and be non-empty.
     */
    public static void basicChecks(File tempDir, File f) {
        File reHomed = new File(tempDir, f.getPath());
        assertTrue("File " + reHomed.getPath() + " missing", reHomed.isFile());
        assertNotEquals("File " + reHomed.getPath() + " unexpected zero size", 0, reHomed.length());
    }

    /**
     * Checks no other files are present than the expected ones
     */
    public static void noOtherFiles(File tempDir, ArrayList<File> fileList) {
        HashSet<String> expected = new HashSet<>();
        for (File f : fileList) {
            expected.add(f.getPath());
        }
        for (File f : flattenDirEntries("", tempDir)) {
            assertTrue("Unexpected file: " + f.getPath(), expected.contains(f.getPath()));
        }
    }

    public static void extractZipFile(File target, String zip) throws IOException {
        ZipFile zipFile = new ZipFile(zip);
        Enumeration<? extends ZipEntry> zipEntryEnum = zipFile.entries();
        byte[] buffer = new byte[1024];

        while (zipEntryEnum.hasMoreElements()) {
            ZipEntry zipEntry = zipEntryEnum.nextElement();
            File file = new File(target, zipEntry.getName());
            assertTrue("File " + file.getPath() + " traverses beyond root", file.toPath().normalize().toAbsolutePath().startsWith(target.toPath().normalize().toAbsolutePath()));
            if (zipEntry.isDirectory()) {
                file.mkdirs();
            } else {
                new File(file.getParent()).mkdirs(); // create folder, if required
                BufferedInputStream bins = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                BufferedOutputStream bout = new BufferedOutputStream(new FileOutputStream(file));
                for (int len; (len = bins.read(buffer)) != -1; ) {
                    bout.write(buffer, 0, len);
                }
                bins.close();
                bout.close();
            }
        }
    }

    // best-effort recursive delete, never throws
    public static void delRec(File f) {
        if (f == null) {
            return;
        }
        try {
            if (f.isDirectory()) {
                for (File child : f.listFiles()) {
                    delRec(child);
                }
                f.delete();
            }
        } catch (Exception ignored) {
        }
    }

    public static ArrayList<File> flattenDirEntries(String prefix, File dir) {
        ArrayList<File> results = new ArrayList<>();
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                if (prefix.equals("")) {
                    results.addAll(flattenDirEntries(f.getName(), f));
                } else {
                    results.addAll(flattenDirEntries(prefix + File.separator + f.getName(), f));
                }
            } else if (f.isFile()) {
                if (prefix.equals("")) {
                    results.add(new File(f.getName()));
                } else {
                    results.add(new File(prefix, f.getName()));
                }
            } else {
                fail("Unexpected file type (not dir or reg file) " + f.getPath());
            }
        }
        return results;
    }

    public static void validateMainJar(File jar) throws IOException {
        ArrayList<File> expectedFiles = new ArrayList<>();
        expectedFiles.addAll(Arrays.asList(jarFiles));
        File tempDir = Files.createTempDirectory("lxc_packaging_tests_jar").toFile();
        extractZipFile(tempDir, jar.getAbsolutePath());
        for (File file : expectedFiles) {
            basicChecks(tempDir, file);
        }
    }
}
