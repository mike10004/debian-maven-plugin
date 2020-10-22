package io.github.mike10004.debutils;

import com.google.common.io.Files;
import org.apache.commons.lang3.StringEscapeUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DebAnalystTest {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void index() throws Exception {
        File debFile = new File(getClass().getResource("/hello_2.10-1build1_amd64.deb").toURI());
        DebAnalyst cache = DebAnalyst.createNew(debFile);
        List<DebEntry> entries = cache.index();
        assertFalse("no entries", entries.isEmpty());
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
