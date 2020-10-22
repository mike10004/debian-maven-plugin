package io.github.mike10004.debianmaven.tests;

import io.github.mike10004.debutils.DebAnalyst;
import io.github.mike10004.debutils.DebEntry;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SingleProjectTest {

    @Test
    public void buildPackage() throws Exception {
        File thisProjectDir = new File(System.getProperty("user.dir"));
        assertEquals("dirname", "debian-maven-plugin-tests", thisProjectDir.getName());
        Path buildDir = thisProjectDir.toPath()
                .getParent()
                .resolve("example-single-project")
                .resolve("target");
        File debFile = Examples.findMostRecentlyModifiedDebFile(buildDir);
        DebAnalyst analyst = DebAnalyst.createNew(debFile);
        DebEntry entry = analyst.findEntryByName("/usr/bin/dmp-example-single-project");
        assertNotNull("entry", entry);
        //assertEquals("permissions", "rwxr-xr-x", entry.permissions);
        assertEquals("type", DebEntry.EntryType.FILE, entry.getEntryType());
        //assertTrue("executable", entry.parsePermissions().containsAll(Set.of(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE)));
    }
}
