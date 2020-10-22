package io.github.mike10004.debianmaven.tests;

import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.debutils.DebAnalyst;
import io.github.mike10004.debutils.DebInfo;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultimoduleProjectTest {

    /**
     * This is an integration test that the custom control lines work.
     */
    @Test
    public void hasBuildDependsLine() throws Exception {
        Path projectDir = Examples.getDirectory("example-multimodule-project")
                .resolve("example-multimodule-deb");
        File debFile = Examples.findMostRecentlyModifiedDebFile(projectDir.resolve("target"));
        assertTrue("deb file name " + debFile.getName(), debFile.getName().matches("^example-multimodule-project_\\S+_all\\.deb$"));
        DebAnalyst analysis = DebAnalyst.createNew(debFile);
        DebInfo info = analysis.info();
        assertEquals("Build-Depends", "build-essential", info.getValue("Build-Depends"));
    }

    @Test
    public void installAndRun() throws Exception {
        Path projectDir = Examples.getDirectory("example-multimodule-project")
                .resolve("example-multimodule-deb");
        File debFile = Examples.findMostRecentlyModifiedDebFile(projectDir.resolve("target"));
        String executable = "example-multimodule-project";
        ContainerSubprocessResult<String> result = PackageTester.onUbuntuJavaImage().testPackageInstallAndExecute(debFile, executable);
        assertEquals("exit code from " + executable + ": " + result, 0, result.exitCode());
        assertEquals("message", "\"hello, world\"\n", result.stdout());
    }

}
