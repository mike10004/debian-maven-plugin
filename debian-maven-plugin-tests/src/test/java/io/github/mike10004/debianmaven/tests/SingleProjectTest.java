package io.github.mike10004.debianmaven.tests;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class SingleProjectTest {

    @Test
    public void buildPackage() throws Exception {
        File thisProjectDir = new File(System.getProperty("user.dir"));
        assertEquals("dirname", "debian-maven-plugin-tests", thisProjectDir.getName());
    }
}
