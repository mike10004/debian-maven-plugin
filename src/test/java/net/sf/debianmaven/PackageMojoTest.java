package net.sf.debianmaven;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class PackageMojoTest {

    @Test
    public void executeDebMojo() throws Exception {
        File targetDir = java.nio.file.Files.createTempDirectory("maven-project-root").resolve("target").toFile();
        targetDir.mkdirs();
        File sourceDir = new File(getClass().getResource("/package-mojo-test/parent/usr/bin/hello-world").toURI())
                .getParentFile().getParentFile().getParentFile();

        PackageMojo m = new PackageMojo() {
            @Override
            protected String getPackageVersion() {
                return "0.1";
            }
        };
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
        m.processExecutionMode = LegacyProcessRunner.PARAM_VALUE;

        m.executeDebMojo();
        List<File> debFiles = java.nio.file.Files.walk(targetDir.toPath()).filter(p -> p.getFileName().toString().endsWith(".deb")).map(Path::toFile).collect(Collectors.toList());
        assertFalse("no .deb files created in " + targetDir, debFiles.isEmpty());
        assertEquals("expect exactly one deb file in target dir; found: " + debFiles, 1, debFiles.size());
        File debFile = debFiles.get(0);
        File expectedDebFile = m.getPackageFile();
        assertEquals("deb file not where expected", expectedDebFile.getCanonicalFile(), debFile.getCanonicalFile());
    }
}