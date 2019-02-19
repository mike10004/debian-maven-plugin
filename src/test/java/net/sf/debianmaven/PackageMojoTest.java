package net.sf.debianmaven;

import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;

public class PackageMojoTest {

    @org.junit.Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @org.junit.Test
    public void getPackageFile() {
        PackageMojo mojo = new UnitTestPackageMojo("3.4.5");
        mojo.packageName = "foo";
        mojo.packageRevision = "9";
        mojo.packageArchitecture = "amd64";
        String packageFilename = mojo.getPackageFile().getName();
        assertEquals("package filename", "foo_3.4.5-9_amd64.deb", packageFilename);
    }

    @org.junit.Test
    public void executeDebMojo_conflicts() throws MojoExecutionException, IOException {
        PackageMojo mojo = new UnitTestPackageMojo("1.2.3");
        String packageName = "foo";
        mojo.packageName = packageName;
        mojo.packageRevision = "1";
        File stageDir = temporaryFolder.newFolder();
        File targetDir = temporaryFolder.getRoot();
        mojo.stageDir = stageDir;
        mojo.includeAttachedArtifacts = false;
        mojo.excludeAllArtifacts = true;
        mojo.packageArchitecture = "all";
        mojo.packageDependencies = new String[] { "bar" };
        mojo.packageConflicts = new String[] { "baz" };
        mojo.maintainerName = "Bartholomew J. Simpson";
        mojo.maintainerEmail = "bsimpson@springfield.net";
        mojo.packageDescription = "The fooest of foos";
        mojo.packageTitle = "The Foo package";
        mojo.targetDir = targetDir;
        Path usrShareFile = stageDir.toPath().resolve("usr").resolve("share").resolve(packageName).resolve("run.sh");
        //noinspection ResultOfMethodCallIgnored
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

    private static class UnitTestPackageMojo extends PackageMojo {

        private String packageVersionOverride;

        public UnitTestPackageMojo(String packageVersion) {
            super();
            packageVersionOverride = Objects.requireNonNull(packageVersion);

        }

        @Override
        public String getPackageVersion() {
            return packageVersionOverride;
        }

        @Override
        protected ExecuteStreamHandler createStreamHandler() {
            return new PumpStreamHandler(new LogOutputStream(getLog()), System.err);
        }
    }
}