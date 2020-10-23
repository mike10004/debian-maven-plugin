package io.github.mike10004.debutils;

import com.google.common.io.Files;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        DebContents contents = DebAnalyst.createNew(debFile).contents();
        List<DebEntry> entries = contents.index();
        assertFalse("no entries", entries.isEmpty());
        entries.forEach(entry -> {
            assertTrue("starts with /: " + entry.name, entry.name.startsWith("/"));
            assertFalse("permissions exist", entry.getPermissions().isEmpty());
            assertEquals("directory entries' names end in /: " + entry.name, entry.getEntryType() == DebEntryType.DIRECTORY, entry.name.endsWith("/"));
        });
        DebEntry bin = contents.findEntryByName("/usr/bin/hello");
        assertNotNull("/usr/bin/hello", bin);
        assertEquals("permissions", Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.GROUP_EXECUTE,
                PosixFilePermission.OTHERS_READ,
                PosixFilePermission.OTHERS_EXECUTE
                ), bin.getPermissions());
        DebEntry binDir = contents.findEntryByName("/usr/bin/");
        assertNotNull("/usr/bin/", binDir);
        assertEquals("bin dir", binDir.getEntryType(), DebEntryType.DIRECTORY);
    }

    @Test
    public void extract() throws Exception {
        File debFile = new File(getClass().getResource("/hello_2.10-1build1_amd64.deb").toURI());
        Path persistentDir = temporaryFolder.newFolder().toPath();
        DebExtraction extraction = DebAnalyst.createNew(debFile).extract(persistentDir);
        File copyrightDestFile = extraction.findByPathname("/usr/share/doc/hello/copyright").orElseThrow();
        String contents = Files.asCharSource(copyrightDestFile, UTF_8).read();
        assertTrue("has correct contents", contents.contains("GNU General Public License"));
    }

    @Test
    public void control() throws Exception {
        File debFile = new File(getClass().getResource("/hello_2.10-1build1_amd64.deb").toURI());
        Path persistentDir = temporaryFolder.newFolder().toPath();
        DebControl control = DebAnalyst.createNew(debFile).control(persistentDir);
        assertEquals("filenames", Arrays.asList("control", "md5sums"), control.filenames().collect(Collectors.toList()));
        String controlText = control.getFileText("control");
        assertNotNull("control file present", controlText);
        assertTrue("contains Package: line", controlText.contains("Package: hello"));
    }

}
