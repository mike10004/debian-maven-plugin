package io.github.mike10004.debianmaven.tests;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExamplesTest {

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getDirectory() throws Exception {
        Path dir = Examples.getDirectory("example-single-project");
        assertTrue("is directory: " + dir, dir.toFile().isDirectory());
    }

    @Test
    public void findMostRecentlyModifiedFile() throws Exception {
        File dir = temporaryFolder.newFolder();
        long delayMs = 50;
        File f1 = new File(dir, "a");
        File f2 = new File(dir, "b");
        File f3 = new File(dir, "c");
        Charset charset = StandardCharsets.UTF_8;
        for (File f : Arrays.asList(f1, f2, f3)) {
            java.nio.file.Files.writeString(f.toPath(), f.getName(), charset);
            Thread.sleep(delayMs);
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(f2, true), charset)) {
            writer.write("\nappendage");
        }
        File actual = Examples.findMostRecentlyModifiedFile(dir.toPath(), f -> true);
        assertEquals("of all", f2, actual);
        actual = Examples.findMostRecentlyModifiedFile(dir.toPath(), f -> !f.getName().contains("b"));
        assertEquals("with filter", f3, actual);
    }

    @Test
    public void findMostRecentlyModifiedFile_emptyDir() throws Exception {
        try {
            Examples.findMostRecentlyModifiedFile(temporaryFolder.newFolder().toPath(), f -> false);
            fail("should have thrown " + NoSuchFileException.class);
        } catch (NoSuchFileException ignore) {
        }
    }
}