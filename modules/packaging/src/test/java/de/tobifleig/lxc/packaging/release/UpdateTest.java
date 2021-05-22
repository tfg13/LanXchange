package de.tobifleig.lxc.packaging.release;

import de.tobifleig.lxc.packaging.AUpdateTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import static de.tobifleig.lxc.packaging.PackagingTestCommon.*;

public class UpdateTest extends AUpdateTest {

    private static final String updateMaster = "update/update_master.zip";
    private static final String updateVersion = "update/v";
    private static final String updatePkg = "lxc.zip";
    private static final String updateSig = "lxc.sign";

    private static File tempDir1;
    private static File tempDir2;

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
        super.TestUpdateMasterContents(updateSig, updatePkg, tempDir1);
    }

    @Test
    public void TestUpdatePkgContents() {
        super.TestUpdatePkgContents(tempDir2);
    }

    @Test
    public void TestSignatureVerifies() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, InvalidKeyException, SignatureException {
        super.TestSignatureVerifies(updateSig, updatePkg, tempDir1, "lxc_updates.pub");
    }

    @Test
    public void TestUpdateInternalVersionMatchesExternal() throws IOException {
        super.TestUpdateInternalVersionMatchesExternal(updateVersion, tempDir2);
    }

    @Test
    public void TestHardcodedUpdaterSizeLimits() {
        super.TestHardcodedUpdaterSizeLimits(updateSig, updatePkg, tempDir1, tempDir2);
    }

    @Test
    public void TestDesktopJar() throws IOException {
        super.TestDesktopJar(tempDir2);
    }

    @AfterClass
    public static void afterClass() {
        delRec(UpdateTest.tempDir1);
        delRec(UpdateTest.tempDir2);
    }
}
