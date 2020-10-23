package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Stream;

class BufferedDebControl implements DebControl {

    private final Map<String, String> fileTextMap;

    public BufferedDebControl(Map<String, String> fileTextMap) {
        this.fileTextMap = Map.copyOf(fileTextMap);
    }

    @Override
    public Stream<String> filenames() {
        return fileTextMap.keySet().stream();
    }

    @Override
    @Nullable
    public String getFileText(String filename) {
        return fileTextMap.get(filename);
    }
}
