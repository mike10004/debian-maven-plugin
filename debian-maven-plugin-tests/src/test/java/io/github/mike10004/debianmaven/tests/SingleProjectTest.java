package io.github.mike10004.debianmaven.tests;

import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.debutils.DebAnalyst;
import io.github.mike10004.debutils.DebContents;
import io.github.mike10004.debutils.DebControl;
import io.github.mike10004.debutils.DebEntry;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SingleProjectTest {

    @Test
    public void buildPackage() throws Exception {
        Path projectDir = Examples.getDirectory("example-single-project");
        File debFile = Examples.findMostRecentlyModifiedDebFile(projectDir.resolve("target"));
        assertTrue("deb file name " + debFile.getName(), debFile.getName().matches("^example-single-project_\\S+_all\\.deb$"));
        DebAnalyst analyst = DebAnalyst.createNew(debFile);
        DebContents contents = analyst.contents();
        DebEntry entry = contents.findEntryByName("/usr/bin/example-single-project");
        assertNotNull("entry", entry);
        assertEquals("permissions", "-rwxr-xr-x", entry.permissions);
        assertEquals("type", DebEntry.EntryType.FILE, entry.getEntryType());
        DebControl control = analyst.control();
        assertTrue("has rules file", control.getFilenames().anyMatch("rules"::equals));
    }

    @Test
    public void installAndRun() throws Exception {
        Assume.assumeFalse("docker tests are skipped", Tests.isSkipDockerTests());
        Path projectDir = Examples.getDirectory("example-single-project");
        File debFile = Examples.findMostRecentlyModifiedDebFile(projectDir.resolve("target"));
        String executable = "example-single-project";
        ContainerSubprocessResult<String> result = PackageTester.onUbuntuJavaImage().testPackageInstallAndExecute(debFile, executable);
        assertEquals("exit code from " + executable + ": " + result, 0, result.exitCode());
        assertEquals("message", "hello, world\n", result.stdout());
    }

}
