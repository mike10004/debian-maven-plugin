package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.stream.Stream;

class BufferedDebControl implements DebControl {

    private final Map<String, PackagingFile> fileMap;

    public BufferedDebControl(Map<String, PackagingFile> fileMap) {
        this.fileMap = Map.copyOf(fileMap);
    }

    @Override
    public Stream<String> filenames() {
        return fileMap.keySet().stream();
    }

    @Override
    @Nullable
    public PackagingFile getFileData(String filename) {
        return fileMap.get(filename);
    }
}
