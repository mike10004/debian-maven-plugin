package io.github.mike10004.debianmaven.tests;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

public class Examples {

    private Examples() {

    }

    public static Path getDirectory(String exampleName) {
        File thisProjectDir = new File(System.getProperty("user.dir"));
        if (!"debian-maven-plugin-tests".equals(thisProjectDir.getName())) {
            throw new IllegalStateException("not in expected directory (tests)");
        }
        return thisProjectDir.toPath()
                .getParent()
                .resolve(exampleName);
    }

    public static File findNewest(Path directory, Predicate<? super File> filter) throws IOException {
        return java.nio.file.Files.walk(directory, 1)
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(filter)
                .min(lastModifiedDescending())
                .orElseThrow();
    }

    private static Comparator<File> lastModifiedDescending() {
        return Comparator.comparing(f -> -f.lastModified());
    }
}
