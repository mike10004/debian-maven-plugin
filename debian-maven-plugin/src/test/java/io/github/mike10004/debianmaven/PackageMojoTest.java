package io.github.mike10004.debianmaven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PackageMojoTest {

    private static Random random;

    @ClassRule
    public static final TemporaryFolder tempFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() {
        random = new Random(PackageMojoTest.class.getName().hashCode());
    }

    @Test
    public void executeDebMojo_subprocessProcessRunner() throws Exception {
        PackageMojo mojo = new PackageMojo();
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

    @org.junit.Test
    public void getPackageFile() {
        PackageMojo mojo = new UnitTestPackageMojo("3.4.5");
        mojo.packageName = "foo";
        mojo.packageRevision = "9";
        mojo.packageArchitecture = "amd64";
        String packageFilename = mojo.getPackageFile().getName();
        assertEquals("package filename", "foo_3.4.5-9_amd64.deb", packageFilename);
    }

    @Test
    public void executeDebMojo_conflicts() throws MojoExecutionException, IOException {
        PackageMojo mojo = new UnitTestPackageMojo("1.2.3");
        String packageName = "foo";
        mojo.packageName = packageName;
        mojo.packageRevision = "1";
        File targetDir = tempFolder.newFolder();
        File stageDir = java.nio.file.Files.createTempDirectory(targetDir.toPath(), "DebianMavenPluginTest").toFile();
        mojo.stageDir = stageDir;
        mojo.packageArchitecture = "all";
        mojo.packageDependencies = new String[] { "bar" };
        mojo.packageConflicts = new String[] { "baz" };
        mojo.maintainerName = "Bartholomew J. Simpson";
        mojo.maintainerEmail = "bsimpson@springfield.net";
        mojo.packageDescription = "The fooest of foos";
        mojo.packageTitle = "The Foo package";
        mojo.targetDir = targetDir;
        Path usrShareFile = stageDir.toPath().resolve("usr").resolve("share").resolve(packageName).resolve("run.sh");
        usrShareFile.toFile().getParentFile().mkdirs();
        java.nio.file.Files.write(usrShareFile, Arrays.asList("#!/bin/bash", "echo \"hello, world\""), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        mojo.executeDebMojo();
        File controlFile = stageDir.toPath().resolve("DEBIAN").resolve("control").toFile();
        assertTrue("control exists", controlFile.length() > 0);
        List<String> controlLines = java.nio.file.Files.readAllLines(controlFile.toPath());
        String conflictsLine = controlLines.stream()
                .filter(line -> line.startsWith("Conflicts"))
                .findFirst().orElseThrow(() -> new AssertionError("Conflicts line not present among " + controlLines));
        assertEquals("conflicts line", "Conflicts: baz", conflictsLine);
    }

    @Test
    public void executeDebMojo_arbitraryControlLines() throws MojoExecutionException, IOException {
        PackageMojo mojo = new UnitTestPackageMojo("1.2.3");
        mojo.packageName = "foo";
        mojo.packageRevision = "1";
        File targetDir = tempFolder.newFolder();
        File stageDir = java.nio.file.Files.createTempDirectory(targetDir.toPath(), "DebianMavenPluginTest").toFile();
        mojo.stageDir = stageDir;
        mojo.packageArchitecture = "all";
        mojo.packageDependencies = new String[] { "kay" };
        mojo.packageConflicts = new String[] { "haw" };
        mojo.maintainerName = "Bartholomew J. Simpson";
        mojo.maintainerEmail = "bsimpson@springfield.net";
        mojo.packageDescription = "The fooest of foos";
        mojo.packageTitle = "The Foo package";
        mojo.targetDir = targetDir;
        mojo.control = new ControlFileLine[] {
                new ControlFileLine("Foo", "bar", "Architecture"),
                new ControlFileLine("Baz", "gaw", null),
        };
        mojo.executeDebMojo();
        File controlFile = stageDir.toPath().resolve("DEBIAN").resolve("control").toFile();
        assertTrue("control exists", controlFile.length() > 0);
        String controlText = java.nio.file.Files.readString(controlFile.toPath(), Charset.defaultCharset());
        assertTrue("expected line in correct place in \n"  + controlText, controlText.contains("\nArchitecture: all\nFoo: bar\n"));
        assertTrue("expected line at end of \n" + controlText, controlText.stripTrailing().endsWith("\nBaz: gaw"));
        String installedSizeLine = controlText.lines().filter(line -> line.startsWith("Installed-Size:")).findFirst().orElseThrow();
        assertTrue("expect correct format in " + installedSizeLine, installedSizeLine.matches("^Installed-Size: \\d+$"));
    }

    private void configureMojoDefaultly(UnitTestPackageMojo mojo) throws IOException {
        mojo.packageName = "foo";
        mojo.packageRevision = "1";
        File targetDir = tempFolder.newFolder();
        mojo.stageDir = java.nio.file.Files.createTempDirectory(targetDir.toPath(), "DebianMavenPluginTest").toFile();
        mojo.packageArchitecture = "all";
        mojo.packageDependencies = new String[] { "kay" };
        mojo.packageConflicts = new String[] { "haw" };
        mojo.maintainerName = "Bartholomew J. Simpson";
        mojo.maintainerEmail = "bsimpson@springfield.net";
        mojo.packageDescription = "The fooest of foos";
        mojo.packageTitle = "The Foo package";
        mojo.targetDir = targetDir;
    }

    @Test
    public void executeDebMojo_dpkgDebBuildOptions() throws MojoExecutionException, IOException {
        LogBucket bucket = new LogBucket();
        UnitTestPackageMojo mojo = new UnitTestPackageMojo("1.2.3", () -> bucket);
        configureMojoDefaultly(mojo);
        mojo.dpkgDebBuildOptions = new String[]{"--verbose", "--debug"};
        mojo.executeDebMojo();
        String loggedContent = bucket.dump();
        System.out.println("logged content:");
        System.out.println(loggedContent);
    }

    private static class UnitTestPackageMojo extends PackageMojo {

        private final String packageVersionOverride;
        private final Supplier<Log> logSupplier;

        public UnitTestPackageMojo(String packageVersion) {
            this(packageVersion, null);
        }

        public UnitTestPackageMojo(String packageVersion, Supplier<Log> logSupplier) {
            super();
            packageVersionOverride = Objects.requireNonNull(packageVersion);
            this.logSupplier = logSupplier;
        }

        @Override
        public String getPackageVersion() {
            return packageVersionOverride;
        }

        @Override
        protected ProcessRunner createProcessRunner() {
            if (logSupplier == null) {
                return super.createProcessRunner();
            } else {
                return new SubprocessProcessRunner(logSupplier);
            }
        }
    }
}