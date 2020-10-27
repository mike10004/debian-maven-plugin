package io.github.mike10004.debutils;

import org.junit.Test;

import static org.junit.Assert.*;

public class DebContentsLineParserTest {

    @Test
    public void parseEntry() throws Exception {
        String line = "lrwxrwxrwx root/root         0 2020-10-27 17:44 ./usr/bin/example-single-project -> ../share/example-single-project/example-single-project.sh";
        DebEntry entry = new DebContentsLineParser().parseEntry(line);
        assertEquals("entry name", "/usr/bin/example-single-project", entry.name);
        assertEquals("entry linkTarget", "../share/example-single-project/example-single-project.sh", entry.linkTarget);
    }
}