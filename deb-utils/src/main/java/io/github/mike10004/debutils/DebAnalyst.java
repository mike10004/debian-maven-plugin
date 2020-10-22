package io.github.mike10004.debutils;

import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * Service class that analyzes a deb file.
 */
public class DebAnalyst {

    private final File debFile;

    private DebAnalyst(File debFile) {
        this.debFile = requireNonNull(debFile, "debFile");
    }

    /**
     * Creates a new instance.
     * @param debFile deb file pathname
     * @return an analyst
     */
    public static DebAnalyst createNew(File debFile) {
        return new DebAnalyst(debFile);
    }

    /**
     * Extracts files from this deb to the given directory.
     * This is the equivalent of {@code dpkg-deb --extract}.
     * @param persistentDir extraction directory
     * @return extraction instance, possibly from cache
     * @throws IOException on I/O error
     */
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
            return new DebExtraction(debFile, destination, files);
        }


    }

    /**
     * Gets the pathname of the deb file this analyst analyzes.
     * @return pathname
     */
    public File getDebFile() {
        return debFile;
    }

    /**
     * Extracts description of contents of a deb file.
     * This is the equivalent of {@code dpkg-deb --contents}.
     * @return contents instance, possibly from cache
     */
    public DebContents contents() throws DpkgDebException {
        return new IndexLoader().call();
    }

    private static DebContents createIndex(ProcessResult<String, String> result) {
        String stdout = result.content().stdout();
        List<DebEntry> entries = stdout.lines()
                .map(DebEntry::fromLine)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new DebContents(entries);
    }

    private class IndexLoader extends DpkgDebLoader<DebContents> {

        public IndexLoader() {
            super(Arrays.asList("--contents", debFile.getAbsolutePath()), DebAnalyst::createIndex);
        }

    }

    public static class DpkgDebException extends Exception  {
        public DpkgDebException(String message) {
            super(message);
        }

        public DpkgDebException(String message, Throwable cause) {
            super(message, cause);
        }

        public DpkgDebException(Throwable cause) {
            super(cause);
        }
    }

    private static class DpkgDebLoader<T> implements Callable<T> {

        private final List<String> args;
        private final Function<ProcessResult<String, String>, T> transform;

        public DpkgDebLoader(List<String> args, Function<ProcessResult<String, String>, T> transform) {
            this.args = requireNonNull(args);
            this.transform = requireNonNull(transform);
        }

        @Override
        public T call() throws DpkgDebException {
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

    /**
     * Extracts deb file information. This is equivalent to {@code dpkg-deb --info}.
     * @return a deb info instance, possibly from cache
     */
    public DebInfo info() throws DpkgDebException {
        return new InfoLoader().call();
    }

    private static DebInfo createInfo(ProcessResult<String, String> result) {
        return new DebInfo(result.content().stdout());
    }

    private class InfoLoader extends DpkgDebLoader<DebInfo> {
        public InfoLoader() {
            super(Arrays.asList("--info", debFile.getAbsolutePath()), DebAnalyst::createInfo);
        }
    }
}
