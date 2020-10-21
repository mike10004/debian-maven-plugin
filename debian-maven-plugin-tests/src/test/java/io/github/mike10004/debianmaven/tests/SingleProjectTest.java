package io.github.mike10004.debianmaven.tests;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class SingleProjectTest {

    @Test
    public void buildPackage() throws Exception {
        Path projectDir = Examples.getDirectory("dmp-single-project");
        MavenRun run = MavenRunner.create().execute(projectDir, "clean", "package");
        List<File> debs = Files.walk(run.projectDirectory().resolve("target"), 1)
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".deb"))
                .collect(Collectors.toList());
        assertEquals("num debs", 1, debs.size());
    }
}
