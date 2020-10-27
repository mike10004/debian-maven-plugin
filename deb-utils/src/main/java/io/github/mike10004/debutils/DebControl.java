package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Value class that represents a set of packaging files.
 * These are the files such as {@code control} and {@code rules}
 * that would be in the {@code /DEBIAN/} directory of a package file.
 * This class provides access to the text of each file.
 */
public interface DebControl {

    /**
     * Gets a list of filenames in the {@code DEBIAN} directory.
     * Each of these can be used to return a non-null value
     * from {@link #getFileText(String)}.
     * @return filenames stream
     */
    Stream<String> filenames();

    /**
     * Gets the text of a packaging file.
     * @param filename filename
     * @return file text
     */
    @Nullable
    default String getFileText(String filename) {
        @Nullable PackagingFile f = getFileData(filename);
        if (f == null) {
            return null;
        }
        return f.text;
    }

    /**
     * Gets the packaging file corresponding to a filename.
     * Packaging files are those found in the {@code DEBIAN/} directory.
     * @param filename filename
     * @return packaging file, or null if not found
     */
    @Nullable
    PackagingFile getFileData(String filename);

    class PackagingFile {

        public final String text;
        public final Set<PosixFilePermission> permissionSet;

        public PackagingFile(String text, Set<PosixFilePermission> permissionSet) {
            this.text = text;
            this.permissionSet = permissionSet;
        }
    }
}
