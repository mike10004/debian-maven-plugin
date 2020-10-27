package io.github.mike10004.debianmaven.tests;

import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.debutils.DebAnalyst;
import io.github.mike10004.debutils.DebContents;
import io.github.mike10004.debutils.DebControl;
import io.github.mike10004.debutils.DebEntry;
import io.github.mike10004.debutils.DebEntryType;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;

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
        DebEntry scriptEntry = contents.findEntryByName("/usr/share/example-single-project/example-single-project.sh");
        assertNotNull("script entry", scriptEntry);
        assertEquals("permissions", PosixFilePermissions.fromString("rwxr-xr-x"), scriptEntry.getPermissions());
        assertEquals("type", DebEntryType.FILE, scriptEntry.getEntryType());
        DebEntry binEntry = contents.findEntryByName("/usr/bin/example-single-project");
        assertNotNull("bin entry", binEntry);
        assertEquals("permissions", EnumSet.allOf(PosixFilePermission.class), binEntry.getPermissions());
        assertEquals("type", DebEntryType.LINK, binEntry.getEntryType());
        DebControl control = analyst.control();
        DebControl.PackagingFile f = control.getFileData("postinst");
        assertNotNull("postinst file", f);
        assertTrue("is executable", f.permissionSet.contains(PosixFilePermission.OWNER_EXECUTE));
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
