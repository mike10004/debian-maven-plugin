package io.github.mike10004.debianmaven.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Predicate;


class Examples {

    private Examples() {

    }

    public static Path getDirectory(String exampleName) {
        File thisProjectDir = new File(System.getProperty("user.dir"));
        if (!"debian-maven-plugin-tests".equals(thisProjectDir.getName())) {
            throw new IllegalStateException("not in expected directory (tests)");
        }
        Path parentProjectDir = thisProjectDir.toPath().getParent();
        return parentProjectDir
                .resolve("debian-maven-plugin-examples")
                .resolve(exampleName);
    }

    public static File findMostRecentlyModifiedDebFile(Path directory) throws IOException {
        return findMostRecentlyModifiedFile(directory, debFileFilter());
    }

    private static Predicate<File> debFileFilter() {
        return new Predicate<>() {
            @Override
            public boolean test(File f) {
                return f.getName().toLowerCase().endsWith(".deb");
            }

            @Override
            public String toString() {
                return "Predicate<filename ends with .deb>";
            }
        };
    }

    /**
     * Finds a directory's most-recently modified file that matches a filter.
     * Does not recurse into subdirectories.
     * @param directory directory
     * @param filter filter
     * @returnthe file
     * @throws IOException if directory does not exist or cannot be read
     * @throws NoSuchFileException if directory does not exist or zero files in the directory match the filter
     */
    public static File findMostRecentlyModifiedFile(Path directory, Predicate<? super File> filter) throws IOException {
        return java.nio.file.Files.walk(directory, 1)
                .map(Path::toFile)
                .filter(File::isFile)
                .filter(filter)
                .min(lastModifiedDescending())
                .orElseThrow(() -> new NoSuchFileException("directory " + directory + " contains zero files accepted by filter  " + filter));
    }

    private static Comparator<File> lastModifiedDescending() {
        return Comparator.comparing(f -> -f.lastModified());
    }
}
