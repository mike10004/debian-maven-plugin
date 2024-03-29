package io.github.mike10004.debianmaven.tests;

import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.debutils.DebAnalyst;
import io.github.mike10004.debutils.DebContents;
import io.github.mike10004.debutils.DebControl;
import io.github.mike10004.debutils.DebEntry;
import io.github.mike10004.debutils.DebEntryType;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
        assertEquals("permissions on " + scriptEntry.name, PosixFilePermissions.fromString("rwxr-xr-x"), scriptEntry.getPermissions());
        assertEquals("type", DebEntryType.FILE, scriptEntry.getEntryType());
        DebEntry binEntry = contents.findEntryByName("/usr/bin/example-single-project");
        assertNotNull("bin entry", binEntry);
        assertEquals("permissions on " + binEntry.name, EnumSet.allOf(PosixFilePermission.class), binEntry.getPermissions());
        assertEquals("type", DebEntryType.LINK, binEntry.getEntryType());
        DebControl control = analyst.control();
        DebControl.PackagingFile postinstFile = control.getFileData("postinst");
        assertNotNull("postinst file", postinstFile);
        assertTrue("is executable", postinstFile.permissionSet.contains(PosixFilePermission.OWNER_EXECUTE));
    }

    @Test
    public void installAndRun() throws Exception {
        Assume.assumeFalse("docker tests are skipped", Tests.isSkipDockerTests());
        Path projectDir = Examples.getDirectory("example-single-project");
        File debFile = Examples.findMostRecentlyModifiedDebFile(projectDir.resolve("target"));
        String executable = "example-single-project";

        PackageTester.InstallResult<List<ContainerSubprocessResult<String>>> resultSet = PackageTester.onUbuntuJavaImage()
                .testPackageInstallAndExecute(debFile, container -> {
                    List<ContainerSubprocessResult<String>> executions = new ArrayList<>();
                    executions.add(container.executor().execute(executable));
                    executions.add(container.executor().execute("cat", "/usr/share/doc/example-single-project/README.txt"));
                    executions.add(container.executor().execute("ls", "-l", "/var/lib/example-single-project/var-subdir/conf/.foothold"));
                    return executions;
                });
        String postinstString = "I run after installation"; // see postinst script
        MatcherAssert.assertThat("dpkg output: \n\n" + resultSet.dpkgResult.stdout() + "\n\n" + resultSet.dpkgResult.stderr(), resultSet.dpkgResult.stdout(), CoreMatchers.containsString(postinstString));
        List<ContainerSubprocessResult<String>> executions = resultSet.additionalResult;
        executions.forEach(execution -> {
            if (execution.exitCode() != 0) {
                System.err.println(execution.stdout());
                System.err.println(execution.stderr());
            }
            assertEquals("exit code in " + execution, 0, execution.exitCode());
        });
        ContainerSubprocessResult<String> binExecResult = executions.get(0);
        assertEquals("message", "hello, world\n", binExecResult.stdout());
    }

}
