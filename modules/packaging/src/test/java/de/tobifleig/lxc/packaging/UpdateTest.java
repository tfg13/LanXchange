package de.tobifleig.lxc.packaging;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;

import static de.tobifleig.lxc.packaging.PackagingTestCommon.*;
import static de.tobifleig.lxc.packaging.PackagingTestCommon.delRec;

import static org.junit.Assert.*;

public class UpdateTest {

    private static final String updateMaster = "update/update_master.zip";
    private static final String updateVersion = "update/v";
    private static final String updatePkg = "lxc.zip";
    private static final String updateSig = "lxc.sign";


    private static File tempDir1;
    private static File tempDir2;

    static File[] updateFiles = new File[]{
            new File("update_helper.exe"),
            new File("v"),
            new File("tmp_update", "lxc.exe"),
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        basicChecks(new File("."), new File(updateMaster));
        basicChecks(new File("."), new File(updateVersion));
        // extract to allow inspection
        UpdateTest.tempDir1 = Files.createTempDirectory("lxc_packaging_tests_update").toFile();
        extractZipFile(UpdateTest.tempDir1, updateMaster);
        UpdateTest.tempDir2 = Files.createTempDirectory("lxc_packaging_tests_update").toFile();
        extractZipFile(UpdateTest.tempDir2, UpdateTest.tempDir1 + File.separator + "lxc.zip");
    }

    @Test
    public void TestUpdateMasterContents() {
        ArrayList<File> expectedFiles = new ArrayList<>();
        expectedFiles.add(new File(updateSig));
        expectedFiles.add(new File(updatePkg));

        for (File file : expectedFiles) {
            basicChecks(UpdateTest.tempDir1, file);
        }

        noOtherFiles(UpdateTest.tempDir1, expectedFiles);
    }

    @Test
    public void TestUpdatePkgContents() {
        ArrayList<File> expectedFiles = new ArrayList<>();
        expectedFiles.addAll(Arrays.asList(DesktopReleaseTest.coreFiles));
        expectedFiles.addAll(Arrays.asList(updateFiles));

        for (File file : expectedFiles) {
            basicChecks(UpdateTest.tempDir2, file);
        }

        noOtherFiles(UpdateTest.tempDir2, expectedFiles);
    }

    @Test
    public void TestSignatureVerifies() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        // the check itself (this is more or less the same code that is used in the updater)
        KeyFactory fact = KeyFactory.getInstance("RSA");
        byte[] pubKeyBytes = Files.readAllBytes(Paths.get("lxc_updates.pub"));
        X509EncodedKeySpec priKeySpec = new X509EncodedKeySpec(pubKeyBytes);
        PublicKey pubKey = fact.generatePublic(priKeySpec);

        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(pubKey);

        byte[] dataBytes = Files.readAllBytes(new File(UpdateTest.tempDir1, updatePkg).toPath());
        sign.update(dataBytes);

        byte[] signBytes = Files.readAllBytes(new File(UpdateTest.tempDir1, updateSig).toPath());
        // signature ok?
        assertTrue("update signature verification failed", sign.verify(signBytes));
    }

    @Test
    public void TestUpdateInternalVersionMatchesExternal() throws IOException {
        byte[] externalVersionData = Files.readAllBytes(Paths.get(updateVersion));
        byte[] internalVersionData = Files.readAllBytes(new File(tempDir2, "v").toPath());
        assertArrayEquals("Internal vs. External version data differs", internalVersionData, externalVersionData);
    }

    @Test
    public void TestHardcodedUpdaterSizeLimits() {
        assertTrue("updater max version info size violation", new File(tempDir2, "v").length() < 32);
        assertTrue("updater max signature size violation", new File(tempDir1, updateSig).length() < 1024 * 1024);
        assertTrue("updater max data size violation", new File(tempDir1, updatePkg).length() < 512 * 1024 * 1024);
    }

    @AfterClass
    public static void afterClass() {
        delRec(UpdateTest.tempDir1);
        delRec(UpdateTest.tempDir2);
    }
}
