package io.github.mike10004.debutils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class DebExtraction {

    public final File debFile;
    public final Path extractionDir;
    public final Collection<File> files;

    public DebExtraction(File debFile, Path extractionDir, Collection<File> files) {
        this.debFile = debFile;
        this.extractionDir = extractionDir;
        this.files = files;
    }

    public Optional<File> findByPathname(String pathname) {
        return files.stream()
                .filter(f -> {
                    return Objects.equals(pathname, StringUtils.removeStart(f.getAbsolutePath(), extractionDir.toString()));
                }).findFirst();
    }
}
