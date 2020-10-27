package io.github.mike10004.debutils;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class DebContentsTest {

    @Test
    public void contents() throws Exception {
        DebContents contents = DebAnalyst.createNew(Tests.getGnuHelloDeb()).contents();
        DebEntry entry = contents.findEntryByName("/usr/bin/hello");
        assertNotNull("/usr/bin/hello entry", entry);
    }
}
