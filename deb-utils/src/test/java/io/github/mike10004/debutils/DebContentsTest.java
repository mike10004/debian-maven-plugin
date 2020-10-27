package io.github.mike10004.debutils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DebContentsTest {

    @Test
    public void contents() throws Exception {
        DebContents contents = DebAnalyst.createNew(Tests.getExampleSingleProjectDeb()).contents();
        DebEntry scriptEntry = contents.findEntryByName("/usr/share/example-single-project/example-single-project.sh");
        assertNotNull("script entry", scriptEntry);
        assertEquals("type of script entry " + scriptEntry, DebEntryType.FILE, scriptEntry.getEntryType());
        DebEntry binEntry = contents.findEntryByName("/usr/bin/example-single-project");
        assertNotNull("/usr/bin/example-single-project entry", binEntry);
        assertEquals("type of bin entry " + binEntry, DebEntryType.LINK, binEntry.getEntryType());
    }
}
