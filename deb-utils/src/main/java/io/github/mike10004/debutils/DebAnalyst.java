package io.github.mike10004.debutils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

/**
 * Service class that analyzes a deb file. Instances of this class cache the
 * information they extract from a deb file, so repeated retrievals of an
 * index or a specific {@link DebEntry index entry} are faster. However, this
 * also means the cache may become invalid, if, for example, extracted files
 * are deleted or the deb file itself is updated.
 */
public class DebAnalyst {

    private final File debFile;
    private transient final TypedCache content;

    private DebAnalyst(File debFile) {
        this.debFile = requireNonNull(debFile, "debFile");
        content = new TypedCache();
    }

    @SuppressWarnings("unused")
    private static class Key<T> {
        private final String id;

        public Key(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key<?> key = (Key<?>) o;
            return Objects.equals(id, key.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    private static class TypedCache {

        private final Cache<Key<?>, Object> innerCache;

        public TypedCache() {
            innerCache = CacheBuilder.newBuilder().build();
        }

        @SuppressWarnings("unchecked")
        public <T> T fetch(Key<T> key, Callable<T> loader) throws ExecutionException {
            return (T) innerCache.get(key, loader);
        }
    }

    public static DebAnalyst createNew(File debFile) {
        DebAnalyst cache = new DebAnalyst(debFile);
        return cache;
    }

    private static final Key<List<DebEntry>> KEY_INDEX = new Key<>("index");
    private static final Key<DebExtraction> KEY_EXTRACTION = new Key<>("extraction");

    /**
     * Extracts this deb to the given directory.
     * This is the equivalent of {@code dpkg -x}.
     * This may retrieve an extraction
     * from a previous call, so make sure that the directory given the first
     * time hasn't been deleted. For example, do not use a temporary folder
     * that has been deleted between then and now.
     * @param persistentDir extraction directory
     * @return extraction
     * @throws IOException if any files
     */
    public DebExtraction extract(Path persistentDir) throws IOException {
        DebExtraction extraction = fetch(KEY_EXTRACTION, new Extractor(persistentDir));
        for (File f : extraction.files) {
            if (!f.isFile()) {
                throw new NoSuchFileException("this extraction has been deleted; it was in " + extraction.extractionDir);
            }
        }
        return extraction;
    }

    private class Extractor implements Callable<DebExtraction> {

        private final Path destination;

        public Extractor(Path destination) {
            this.destination = destination;
        }

        public DebExtraction call() throws IOException {
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
     * @return the deb file
     */
    public File getDebFile() {
        return debFile;
    }

    /**
     * Returns the first entry that is accepted by a predicate.
     * @param filter entry filter
     * @return entry or null if not found
     */
    @Nullable
    public DebEntry findEntry(Predicate<? super DebEntry> filter) {
        return index().stream().filter(filter).findFirst().orElse(null);
    }

    /**
     * Finds an entry by matching its name field. The name field is expressed
     * as an absolute pathname, where directory does not have a trailing slash.
     * @param name entry name; this always starts with {@code /}; if argument does not
     * start with {@code /}, no entry will be found
     * @return entry or null if not found
     * @see DebEntry#name
     */
    @Nullable
    public DebEntry findEntryByName(String name) {
        return findEntry(entry -> name.equals(entry.name));
    }

    private <T> T fetch(Key<T> key, Callable<T> loader) {
        try {
            return content.fetch(key, loader);
        } catch (ExecutionException e) {
            throw new RuntimeException("exception during cache loading", e.getCause());
        }
    }

    /**
     * Fetches the index of the deb file. This is the equivalent of {@code dpkg-deb --contents}.
     * @return list of entries
     */
    public List<DebEntry> index() {
        return fetch(KEY_INDEX, new IndexLoader());
    }

    private class IndexLoader implements Callable<List<DebEntry>> {

        @Override
        public List<DebEntry> call() throws CacheLoaderException, InterruptedException {
            String stdout;
            try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
                ProcessResult<String, String> result = Subprocess.running("dpkg-deb")
                        .arg("--contents")
                        .arg(debFile.getAbsolutePath())
                        .build()
                        .launcher(processTracker)
                        .outputStrings(Charset.defaultCharset())
                        .launch().await();
                checkState(result.exitCode() == 0, "nonzero exit %s: %s", result.exitCode(), result.content().stderr());
                stdout = result.content().stdout();
            } catch (RuntimeException e) {
                throw new CacheLoaderException(e);
            }
            List<DebEntry> entries = stdout.lines()
                    .map(DebEntry::fromLine)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            checkState(!entries.isEmpty(), "entries list is empty");
            return entries;
        }
    }

    private static class CacheLoaderException extends Exception {
        public CacheLoaderException(RuntimeException e) {
            super(e);
        }
    }
}
