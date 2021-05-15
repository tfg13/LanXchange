package de.tobifleig.lxc.packaging;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

public final class PackagingTestCommon {

    /**
     * Basic checks for a file. It should exist and be non-empty.
     */
    static void basicChecks(File tempDir, File f) {
        File reHomed = new File(tempDir, f.getPath());
        assertTrue("File " + reHomed.getPath() + " missing", reHomed.isFile());
        assertNotEquals("File " + reHomed.getPath() + " unexpected zero size", 0, reHomed.length());
    }

    /**
     * Checks no other files are present than the expected ones
     */
    static void noOtherFiles(File tempDir, ArrayList<File> fileList) {
        HashSet<String> expected = new HashSet<>();
        for (File f : fileList) {
            expected.add(f.getPath());
        }
        for (File f : flattenDirEntries("", tempDir)) {
            assertTrue("Unexpected file: " + f.getPath(), expected.contains(f.getPath()));
        }
    }

    static void extractZipFile(File target, String zip) throws IOException {
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
    static void delRec(File f) {
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

    static ArrayList<File> flattenDirEntries(String prefix, File dir) {
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
}
