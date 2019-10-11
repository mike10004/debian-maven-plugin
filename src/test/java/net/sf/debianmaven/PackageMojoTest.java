package net.sf.debianmaven;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class PackageMojoTest {

    private static Random random;

    @ClassRule
    public static final TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() {
        random = new Random(PackageMojoTest.class.getName().hashCode());
    }

    @Test
    public void executeDebMojo_legacyProcessRunner() throws Exception {
        PackageMojo mojo = new PackageMojo();
        mojo.processExecutionMode = LegacyProcessRunner.PARAM_VALUE;
        testExecuteDebMojo(mojo);
    }

    @Test
    public void executeDebMojo_subprocessProcessRunner() throws Exception {
        PackageMojo mojo = new PackageMojo();
        mojo.processExecutionMode = SubprocessProcessRunner.PARAM_VALUE;
        Log log = new SystemStreamLog() {
            @Override
            public void warn(CharSequence content) {
                super.warn(content);
                Assert.fail("Log.warn invoked");
            }

            @Override
            public void warn(CharSequence content, Throwable error) {
                super.warn(content, error);
                Assert.fail("Log.warn invoked");
            }

            @Override
            public void warn(Throwable error) {
                super.warn(error);
                Assert.fail("Log.warn invoked");
            }
        };
        mojo.setLog(log);
        testExecuteDebMojo(mojo);
    }

    private void testExecuteDebMojo(PackageMojo m) throws Exception {
        File targetDir = java.nio.file.Files.createTempDirectory(tempFolder.getRoot().toPath(), "maven-project-root").resolve("target").toFile();
        targetDir.mkdirs();
        File sourceDir = new File(getClass().getResource("/package-mojo-test/parent/usr/bin/hello-world").toURI())
                .getParentFile().getParentFile().getParentFile();

        m.setPackageVersion("0." + random.nextInt(Integer.MAX_VALUE - 1));
        m.packageName = "hello-world";
        m.packageArchitecture = "all";
        m.maintainerName = "Wonder Woman";
        m.maintainerEmail = "diana@paradiseisland.net";
        m.excludeAllArtifacts = true;
        m.packageDescription = "this is a test";
        m.packagePriority = "optional";
        m.packageSection = "contrib/utils";
        m.packageTitle = "hello-world";
        m.packageDescription = "testing the package mojo";
        m.targetDir = targetDir;
        m.stageDir = targetDir.toPath().resolve("deb").toFile();
        m.sourceDir = sourceDir;

        m.executeDebMojo();
        List<File> debFiles = java.nio.file.Files.walk(targetDir.toPath()).filter(p -> p.getFileName().toString().endsWith(".deb")).map(Path::toFile).collect(Collectors.toList());
        assertFalse("no .deb files created in " + targetDir, debFiles.isEmpty());
        assertEquals("expect exactly one deb file in target dir; found: " + debFiles, 1, debFiles.size());
        File debFile = debFiles.get(0);
        File expectedDebFile = m.getPackageFile();
        assertEquals("deb file not where expected", expectedDebFile.getCanonicalFile(), debFile.getCanonicalFile());
    }
}