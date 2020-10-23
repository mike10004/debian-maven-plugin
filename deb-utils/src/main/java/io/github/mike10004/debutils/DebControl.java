package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Value class that represents a set of packaging files.
 * These are the files such as {@code control} and {@code rules}
 * that would be in the {@code /DEBIAN/} directory of a package file.
 * This class provides access to the text of each file.
 */
public class DebControl {

    private final Map<String, String> fileTextMap;

    public DebControl(Map<String, String> fileTextMap) {
        this.fileTextMap = Map.copyOf(fileTextMap);
    }

    public Stream<String> filenames() {
        return fileTextMap.keySet().stream();
    }

    @Nullable
    public String getFileText(String filename) {
        return fileTextMap.get(filename);
    }
}
