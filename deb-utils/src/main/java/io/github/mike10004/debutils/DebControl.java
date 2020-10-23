package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Stream;

public class DebControl {

    private final Map<String, String> fileTextMap;

    public DebControl(Map<String, String> fileTextMap) {
        this.fileTextMap = Map.copyOf(fileTextMap);
    }

    public Stream<String> getFilenames() {
        return fileTextMap.keySet().stream();
    }

    @Nullable
    public String getFileText(String filename) {
        return fileTextMap.get(filename);
    }
}
