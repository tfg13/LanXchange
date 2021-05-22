package de.tobifleig.lxc.packaging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;

import static de.tobifleig.lxc.packaging.PackagingTestCommon.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public abstract class AUpdateTest {

    static File[] updateFiles = new File[]{
            new File("update_helper.exe"),
            new File("v"),
            new File("tmp_update", "lxc.exe"),
    };


    public void TestUpdateMasterContents(String updateSig, String updatePkg, File tempDir) {
        ArrayList<File> expectedFiles = new ArrayList<>();
        expectedFiles.add(new File(updateSig));
        expectedFiles.add(new File(updatePkg));

        for (File file : expectedFiles) {
            basicChecks(tempDir, file);
        }

        noOtherFiles(tempDir, expectedFiles);
    }

    public void TestUpdatePkgContents(File tempDir) {
        ArrayList<File> expectedFiles = new ArrayList<>();
        expectedFiles.addAll(Arrays.asList(DesktopReleaseTest.coreFiles));
        expectedFiles.addAll(Arrays.asList(updateFiles));

        for (File file : expectedFiles) {
            basicChecks(tempDir, file);
        }

        noOtherFiles(tempDir, expectedFiles);
    }

    public void TestSignatureVerifies(String updateSig, String updatePkg, File tempDir, String pub) throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        // the check itself (this is more or less the same code that is used in the updater)
        KeyFactory fact = KeyFactory.getInstance("RSA");
        byte[] pubKeyBytes = Files.readAllBytes(Paths.get(pub));
        X509EncodedKeySpec priKeySpec = new X509EncodedKeySpec(pubKeyBytes);
        PublicKey pubKey = fact.generatePublic(priKeySpec);

        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(pubKey);

        byte[] dataBytes = Files.readAllBytes(new File(tempDir, updatePkg).toPath());
        sign.update(dataBytes);

        byte[] signBytes = Files.readAllBytes(new File(tempDir, updateSig).toPath());
        // signature ok?
        assertTrue("update signature verification failed", sign.verify(signBytes));
    }

    public void TestUpdateInternalVersionMatchesExternal(String updateVersion, File tempDir) throws IOException {
        byte[] externalVersionData = Files.readAllBytes(Paths.get(updateVersion));
        byte[] internalVersionData = Files.readAllBytes(new File(tempDir, "v").toPath());
        assertArrayEquals("Internal vs. External version data differs", internalVersionData, externalVersionData);
    }

    public void TestHardcodedUpdaterSizeLimits(String updateSig, String updatePkg, File tempDir1, File tempDir2) {
        assertTrue("updater max version info size violation", new File(tempDir2, "v").length() < 32);
        assertTrue("updater max signature size violation", new File(tempDir1, updateSig).length() < 1024 * 1024);
        assertTrue("updater max data size violation", new File(tempDir1, updatePkg).length() < 512 * 1024 * 1024);
    }

    public void TestDesktopJar(File tempDir) throws IOException {
        validateMainJar(new File(tempDir, "lanxchange.jar"));
    }

}
