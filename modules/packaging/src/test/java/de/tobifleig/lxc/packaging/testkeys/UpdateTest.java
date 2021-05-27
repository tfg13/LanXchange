package de.tobifleig.lxc.packaging.testkeys;

import com.sun.net.httpserver.*;
import de.tobifleig.lxc.LXC;
import de.tobifleig.lxc.packaging.AUpdateTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;

import static de.tobifleig.lxc.packaging.PackagingTestCommon.*;
import static org.junit.Assert.*;

public class UpdateTest extends AUpdateTest {

    private static final String desktopRelease = "releases/stable/lxc.zip";
    private static final String updateMaster = "update/update_master.zip";
    private static final String updateVersion = "update/v";
    private static final String updatePkg = "lxc.zip";
    private static final String updateSig = "lxc.sign";

    private static File tempDir1;
    private static File tempDir2;
    private static File tempDir3;

    @BeforeClass
    public static void beforeClass() throws Exception {
        basicChecks(new File("."), new File(updateMaster));
        basicChecks(new File("."), new File(updateVersion));
        // extract to allow inspection
        UpdateTest.tempDir1 = Files.createTempDirectory("lxc_packaging_tests_update").toFile();
        extractZipFile(UpdateTest.tempDir1, updateMaster);
        UpdateTest.tempDir2 = Files.createTempDirectory("lxc_packaging_tests_update").toFile();
        extractZipFile(UpdateTest.tempDir2, UpdateTest.tempDir1 + File.separator + "lxc.zip");
        UpdateTest.tempDir3 = Files.createTempDirectory("lxc_packaging_tests_update").toFile();
        extractZipFile(UpdateTest.tempDir3, desktopRelease);
        // overwrite test key in desktopRelease
        Files.copy(new File("modules/packaging/res/env/lxc_updates.pub").toPath(), new File(UpdateTest.tempDir3,"lxc_updates_test.pub").toPath());
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
        // generate test keys?
        super.TestSignatureVerifies(updateSig, updatePkg, tempDir1, "modules/packaging/res/env/lxc_updates.pub");
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

    @Test
    public void TestFullUpdate() throws IOException, InterruptedException, NoSuchAlgorithmException {
        basicChecks(tempDir3, new File("lxc.exe"));
        // spin up local fake update host
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        List<String> servedFiles = Arrays.asList(new String[]{"v", "update_master.zip"});
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                URI uri = httpExchange.getRequestURI();
                OutputStream out = httpExchange.getResponseBody();
                String name = new File(uri.getPath()).getName();
                if (!servedFiles.contains(name)) {
                    httpExchange.sendResponseHeaders(404, 0);
                    out.write("404".getBytes());
                    out.close();
                    return;
                }
                File file = null;
                switch (name) {
                    case "v":
                        file = new File(updateVersion);
                        break;
                    case "update_master.zip":
                        file = new File(updateMaster);
                        break;
                }
                httpExchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                httpExchange.sendResponseHeaders(200, file.length());
                out.write(Files.readAllBytes(file.toPath()));
                out.close();
            }
        });
        server.start();
        // run update
        int exitCode = Runtime.getRuntime().exec(
                new String[]{
                        new File(tempDir3, "lxc.exe").getAbsolutePath(),
                        "-unsafe_updates",
                        "-unsafe_url_override", "localhost:8080",
                        "-unsafe_disable_tls",
                        "-unsafe_update_sig_pubkey", "lxc_updates_test.pub",
                }
        ).waitFor();
        assertEquals(0, exitCode);
        server.stop(0);
        // wait a bit for the new exe to start, then assert the update actually worked
        // this is not ideal
        Thread.sleep(2000);
        List<String> logData = Files.readAllLines(new File(tempDir3, "lxc.log").toPath());
        assertTrue("unable to prove update happened!", logData.stream().anyMatch(s -> s.contains("This is LanXchange " + LXC.versionString + " (" + LXC.versionId + ")")));
    }

    @AfterClass
    public static void afterClass() {
        delRec(UpdateTest.tempDir1);
        delRec(UpdateTest.tempDir2);
        delRec(UpdateTest.tempDir3);
    }
}
