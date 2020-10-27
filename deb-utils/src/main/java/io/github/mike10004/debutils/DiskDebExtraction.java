package io.github.mike10004.debutils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DiskDebExtraction implements DebExtraction {

    private final Path extractionDir;
    private final Collection<Path> files;

    public DiskDebExtraction(Path extractionDir, Collection<File> files) {
        this.extractionDir = extractionDir;
        this.files = files.stream().map(File::toPath).collect(Collectors.toList());
    }

    @Override
    @Nullable
    public File findByInstalledPathname(String pathname) {
        return files.stream()
                .map(Path::toFile)
                .filter(f -> {
                    return Objects.equals(pathname, StringUtils.removeStart(f.getAbsolutePath(), FilenameUtils.normalizeNoEndSeparator(extractionDir.toString())));
                }).findFirst().orElse(null);
    }

    @Override
    public Path extractionDirectory() {
        return extractionDir;
    }

    @Override
    public Stream<String> installedPathnames() {
        return files.stream()
                .map(p -> {
                    Path relative = extractionDir.relativize(p);
                    return String.format("/%s", relative);
                });
    }

}
