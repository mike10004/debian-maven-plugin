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
        Path projectDir = Examples.getDirectory("example-single-project");
        File debFile = Examples.findMostRecentlyModifiedDebFile(projectDir.resolve("target"));
        assertTrue("deb file name " + debFile.getName(), debFile.getName().matches("^example-single-project_\\S+_all\\.deb$"));
        DebAnalyst analyst = DebAnalyst.createNew(debFile);
        DebEntry entry = analyst.findEntryByName("/usr/bin/example-single-project");
        assertNotNull("entry", entry);
        assertEquals("permissions", "-rwxr-xr-x", entry.permissions);
        assertEquals("type", DebEntry.EntryType.FILE, entry.getEntryType());
    }
}
