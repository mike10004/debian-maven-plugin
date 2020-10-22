package io.github.mike10004.debutils;

import com.google.common.io.Files;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DebAnalystTest {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void index() throws Exception {
        File debFile = new File(getClass().getResource("/hello_2.10-1build1_amd64.deb").toURI());
        DebAnalyst cache = DebAnalyst.createNew(debFile);
        List<DebEntry> entries = cache.contents().index();
        assertFalse("no entries", entries.isEmpty());
        entries.forEach(entry -> {
            assertTrue("starts with /: " + entry.name, entry.name.startsWith("/"));
            assertTrue("permissions exist: " + entry.permissions, entry.permissions.matches(".*[^-].*"));
            assertEquals("permissions chars: " + entry.permissions, "-rwx------".length(), entry.permissions.length());
            assertEquals("directory entries' names end in /: " + entry.name, entry.getEntryType() == DebEntry.EntryType.DIRECTORY, entry.name.endsWith("/"));
        });
        DebEntry bin = cache.contents().findEntryByName("/usr/bin/hello");
        assertNotNull("/usr/bin/hello", bin);
        assertEquals("permissions", Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
                ), bin.parsePermissions());
        DebEntry binDir = cache.contents().findEntryByName("/usr/bin/");
        assertNotNull("/usr/bin/", binDir);
        assertEquals("bin dir", binDir.getEntryType(), DebEntry.EntryType.DIRECTORY);
    }

    @Test
    public void extract() throws Exception {
        File debFile = new File(getClass().getResource("/hello_2.10-1build1_amd64.deb").toURI());
        DebAnalyst cache = DebAnalyst.createNew(debFile);
        Path persistentDir = temporaryFolder.newFolder().toPath();
        DebExtraction extraction = cache.extract(persistentDir);
        File copyrightDestFile = extraction.findByPathname("/usr/share/doc/hello/copyright").orElseThrow();
        String contents = Files.asCharSource(copyrightDestFile, UTF_8).read();
        assertTrue("has correct contents", contents.contains("GNU General Public License"));
    }
}
