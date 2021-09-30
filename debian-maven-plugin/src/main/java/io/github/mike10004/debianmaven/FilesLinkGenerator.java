package io.github.mike10004.debianmaven;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Link generator implementation that uses {@link java.nio.file.Files}.
 */
public class FilesLinkGenerator implements LinkGenerator {

    private final FilesSymlinker symlinker;
    private final LinksLineParser linksLineParser;
    private final Charset encoding;

    public FilesLinkGenerator() {
        this(new DefaultLinksLineParser(), FilesSymlinker.createDefault(), StandardCharsets.UTF_8);
    }

    public FilesLinkGenerator(LinksLineParser linksLineParser,
                              FilesSymlinker symlinker,
                              Charset linksFileEncoding) {
        this.linksLineParser = requireNonNull(linksLineParser);
        this.symlinker = requireNonNull(symlinker);
        this.encoding = requireNonNull(linksFileEncoding);
    }

    @Override
    public void generateLinks(@Nullable File[] linksFiles, Path stageDir) throws IOException, MojoExecutionException {
        if (linksFiles == null || linksFiles.length == 0) {
            return;
        }
        List<String> linksFileTexts = new ArrayList<>();
        for (File linksFile : linksFiles) {
            linksFileTexts.add(readLinksFileText(linksFile));
        }
        List<LinkSpecification> specifications = new ArrayList<>();
        for (String linksFileText : linksFileTexts) {
            List<String> lines = linksFileText.lines().collect(Collectors.toUnmodifiableList());
            for (String line : lines) {
                @Nullable LinkSpecification specification = linksLineParser.parseSpecification(line);
                if (specification != null) {
                    specifications.add(specification);
                }
            }
        }
        for (LinkSpecification specification : specifications) {
            createLink(specification.sourcePath(), specification.linkPath(), stageDir);
        }
    }

    private String readLinksFileText(File linksFile) throws IOException {
        return java.nio.file.Files.readString(linksFile.toPath(), encoding);
    }

    public interface FilesSymlinker {

        void createSymlink(Path source, Path link, FileAttribute<?>...attrs) throws IOException;

        static FilesSymlinker createDefault() {
            return new DefaultFilesSymlinker();
        }
    }

    private static class DefaultFilesSymlinker implements  FilesSymlinker {

        @Override
        public void createSymlink(Path source, Path link, FileAttribute<?>... attrs) throws IOException {
            try {
                java.nio.file.Files.createSymbolicLink(link, source, attrs);
            } catch (FileAlreadyExistsException ignore) {
                if (java.nio.file.Files.isSymbolicLink(link) && source.equals(java.nio.file.Files.readSymbolicLink(link))) {
                    // then we have no work to do
                    return;
                }
                // try deleting and relinking; if that fails, let the build fail
                //noinspection ResultOfMethodCallIgnored
                link.toFile().delete();
                java.nio.file.Files.createSymbolicLink(link, source, attrs);
            }
        }
    }

    /**
     * Source paths are transformed to be relative to the link path.
     * @param absoluteSourcePath absolute source path
     * @param absoluteLinkPath absolute link path
     * @param stageDir deb filesystem root
     * @throws IOException
     */
    public void createLink(String absoluteSourcePath, String absoluteLinkPath, Path stageDir) throws IOException, MojoExecutionException {
        Path stagedSourcePath = relativizeSourcePath(absoluteSourcePath, absoluteLinkPath);
        Path stagedLinkPath = stageDir.resolve(removeRoot(absoluteLinkPath));
        ensureParentExists(stagedLinkPath);
        symlinker.createSymlink(stagedSourcePath, stagedLinkPath);
    }

    private static Path removeRoot(String absolutePathStr) {
        return Path.of("/").relativize(Path.of(absolutePathStr));
    }

    private void ensureParentExists(Path linkPath) throws IOException {
        Path parent = linkPath.getParent();
        File parentFile = parent.toFile();
        if (!parentFile.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            parentFile.mkdirs();
        }
    }

    private static class InvalidLinkException extends MojoExecutionException {
        public InvalidLinkException(String link) {
            super("links must be absolute: " + StringUtils.abbreviate(link, 512));
        }
    }

    /**
     * Relativizes a source path against the link path.
     * The exception is where they share no common ancestor (except /),
     * in which case the return value is an absolute path.
     * @param absoluteSourcePathStr
     * @param absoluteLinkPathStr
     * @return
     * @throws IOException
     * @throws MojoExecutionException
     */
    public Path relativizeSourcePath(String absoluteSourcePathStr, String absoluteLinkPathStr) throws IOException, MojoExecutionException {
        absoluteSourcePathStr = FilenameUtils.normalizeNoEndSeparator(absoluteSourcePathStr, true);
        absoluteLinkPathStr = FilenameUtils.normalizeNoEndSeparator(absoluteLinkPathStr, true);
        Path absoluteSourcePath = Path.of(absoluteSourcePathStr);
        if (!absoluteSourcePath.isAbsolute()) {
            throw new InvalidLinkException(absoluteSourcePathStr);
        }
        Path absoluteLinkPath = Path.of(absoluteLinkPathStr);
        if (!absoluteLinkPath.isAbsolute()) {
            throw new InvalidLinkException(absoluteLinkPathStr);
        }
        if (!haveCommonAncestor(absoluteSourcePath, absoluteLinkPath)) {
            return absoluteSourcePath;
        }
        Path relativized = absoluteLinkPath.relativize(absoluteSourcePath);
        /*
         * This doesn't quite do the job we want because its semantics
         * are different from symlinks
         */
        relativized = stripFirstDotDot(relativized);
        return relativized;
    }

    private static Path stripFirstDotDot(Path p) {
        String pathStr = p.toString();
        pathStr = StringUtils.removeStart(pathStr, "../");
        return Path.of(pathStr);
    }

    private static boolean haveCommonAncestor(Path a, Path b) {
        Path a1 = a.iterator().next();
        Path b1 = b.iterator().next();
        return a1.equals(b1);
    }
}
