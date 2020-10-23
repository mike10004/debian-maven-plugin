package io.github.mike10004.debutils;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class DiskDebExtractionTest {

    @Test
    public void findByInstalledPathname() {
        Path extractionDir = Path.of("/path", "to", "extdir");
        File f1 = extractionDir.resolve("bin").resolve("foo").toFile();
        File f2 = extractionDir.resolve("lib").resolve("bar").toFile();
        File f3 = extractionDir.resolve("baz").toFile();
        DiskDebExtraction d = new DiskDebExtraction(extractionDir, Arrays.asList(f1, f2, f3));
        assertEquals("f1", f1, d.findByInstalledPathname("/bin/foo").orElse(null));
        assertEquals(Arrays.asList("/bin/foo", "/lib/bar", "/baz"), d.installedPathnames().collect(Collectors.toList()));
    }
}