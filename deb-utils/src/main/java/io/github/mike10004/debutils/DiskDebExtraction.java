package io.github.mike10004.debutils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

class DiskDebExtraction implements DebExtraction {

    private final File debFile;
    private final Path extractionDir;
    private final Collection<File> files;

    public DiskDebExtraction(File debFile, Path extractionDir, Collection<File> files) {
        this.debFile = debFile;
        this.extractionDir = extractionDir;
        this.files = files;
    }

    @Override
    public Optional<File> findByInstalledPathname(String pathname) {
        return files.stream()
                .filter(f -> {
                    return Objects.equals(pathname, StringUtils.removeStart(f.getAbsolutePath(), FilenameUtils.normalizeNoEndSeparator(extractionDir.toString())));
                }).findFirst();
    }

    @Override
    public File getDebFile() {
        return debFile;
    }

    @Override
    public Path getExtractionDir() {
        return extractionDir;
    }

    @Override
    public Collection<File> getFiles() {
        return files;
    }
}
