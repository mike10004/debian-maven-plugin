package io.github.mike10004.debutils;

import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * Analyst implementation that uses {@code dpkg-deb}.
 */
class DpkgDebAnalyst implements DebAnalyst {

    private static final Logger log = LoggerFactory.getLogger(DpkgDebAnalyst.class);

    private final File debFile;

    public DpkgDebAnalyst(File debFile) {
        this.debFile = requireNonNull(debFile, "debFile");
    }

    @Override
    public DebExtraction extract(Path persistentDir) throws IOException {
        DebExtraction extraction = new Extractor(persistentDir).extract();
        return extraction;
    }

    private class Extractor {

        private final Path destination;

        public Extractor(Path destination) {
            this.destination = destination;
        }

        public DebExtraction extract() throws IOException {
            Subprocess s = Subprocess.running("dpkg")
                    .arg("--extract")
                    .arg(debFile.getAbsolutePath())
                    .arg(destination.toString())
                    .build();
            ProcessResult<String, String> presult;
            try (ScopedProcessTracker tracker = new ScopedProcessTracker()) {
                presult = s.launcher(tracker)
                        .outputStrings(Charset.defaultCharset())
                        .launch().await(30, TimeUnit.SECONDS);
            } catch (TimeoutException | InterruptedException e) {
                throw new RuntimeException("failed to await result of dpkg --extract", e);
            }
            if (presult.exitCode() != 0) {
                throw new IOException(String.format("exit code %s from dpkg --extract: %s", presult.exitCode(), presult.content().stderr()));
            }
            Collection<File> files = FileUtils.listFiles(destination.toFile(), null, true);
            return new DiskDebExtraction(destination, files);
        }


    }

    @Override
    public File getDebFile() {
        return debFile;
    }

    @Override
    public DebContents contents() throws DebUtilsException {
        return new IndexLoader().call();
    }

    private static class DpkgDebException extends DebUtilsException {

        @SuppressWarnings("unused")
        public DpkgDebException(String message) {
            super(message);
        }

        @SuppressWarnings("unused")
        public DpkgDebException(String message, Throwable cause) {
            super(message, cause);
        }

        public DpkgDebException(Throwable cause) {
            super(cause);
        }
    }

    @Override
    public DebControl control() throws DebUtilsException {
        Path tempdir = null;
        try {
            tempdir = java.nio.file.Files.createTempDirectory("deb-control-output");
            return control(tempdir);
        } catch (IOException e) {
            throw new DpkgDebException(e);
        } finally {
            if (tempdir != null) {
                try {
                    FileUtils.deleteDirectory(tempdir.toFile());
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).warn("failed to delete temporary directory at " + tempdir);
                }
            }
        }
    }

    @Override
    public DebControl control(Path scratchDir) throws DebUtilsException {
        return new ControlLoader(scratchDir).call();
    }

    private static DebContents createIndex(ProcessResult<String, String> result) {
        String stdout = result.content().stdout();
        DebContentsLineParser lineParser = new DebContentsLineParser();
        List<DebEntry> entries = stdout.lines()
                .map(line -> {
                    try {
                        return lineParser.parseEntry(line);
                    } catch (DebUtilsException e) {
                        log.debug("failed to parse line from contents output: {}", StringUtils.abbreviate(line, 512));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new BufferedDebContents(entries);
    }

    private class IndexLoader extends DpkgDebLoader<DebContents> {

        public IndexLoader() {
            super(Arrays.asList("--contents", debFile.getAbsolutePath()), DpkgDebAnalyst::createIndex);
        }

    }

    private class ControlLoader extends DpkgDebLoader<DebControl> {

        public ControlLoader(Path outputDir) {
            super(Arrays.asList("--control", debFile.getAbsolutePath(), outputDir.toString()), result -> {
                return extractControl(outputDir, result);
            });
        }

    }

    private interface ThrowingFunction<F, T, X extends Throwable> {
        T apply(F input) throws X;
    }

    private static Charset controlFileCharset() {
        return StandardCharsets.UTF_8;
    }

    private static DebControl fromOutputDir(Path outputDir) throws IOException {
        Map<String, DebControl.PackagingFile> fileMap = new HashMap<>();
        for (Path p : java.nio.file.Files.list(outputDir).toArray(Path[]::new)) {
            String text = java.nio.file.Files.readString(p, controlFileCharset());
            Set<PosixFilePermission> permissions = java.nio.file.Files.getPosixFilePermissions(p);
            DebControl.PackagingFile f = new DebControl.PackagingFile(text, permissions);
            fileMap.put(p.getFileName().toString(), f);
        }
        return new BufferedDebControl(fileMap);
    }

    @SuppressWarnings("unused")
    private static DebControl extractControl(Path outputDir, ProcessResult<String, String> result) throws DebUtilsException {
        try {
            return fromOutputDir(outputDir);
        } catch (IOException e) {
            throw new DpkgDebException(e);
        }
    }

    private static class DpkgDebLoader<T> implements Callable<T> {

        private final List<String> args;
        private final ThrowingFunction<ProcessResult<String, String>, T, DebUtilsException> transform;

        public DpkgDebLoader(List<String> args, ThrowingFunction<ProcessResult<String, String>, T, DebUtilsException> transform) {
            this.args = requireNonNull(args);
            this.transform = requireNonNull(transform);
        }

        @Override
        public T call() throws DebUtilsException {
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                ProcessResult<String, String> result = Subprocess.running("dpkg-deb")
                        .args(args)
                        .build()
                        .launcher(processTracker)
                        .outputStrings(Charset.defaultCharset())
                        .launch().await();
                checkState(result.exitCode() == 0, "nonzero exit %s: %s", result.exitCode(), result.content().stderr());
                return transform.apply(result);
            } catch (InterruptedException | RuntimeException e) {
                throw new DpkgDebException(e);
            }
        }

    }


    @Override
    public DebInfo info() throws DebUtilsException {
        return new InfoLoader().call();
    }

    private static DebInfo createInfo(ProcessResult<String, String> result) {
        return new BufferedDebInfo(result.content().stdout());
    }

    private class InfoLoader extends DpkgDebLoader<DebInfo> {
        public InfoLoader() {
            super(Arrays.asList("--info", debFile.getAbsolutePath()), DpkgDebAnalyst::createInfo);
        }
    }
}
