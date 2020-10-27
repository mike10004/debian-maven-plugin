package io.github.mike10004.debutils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface that provides services related to deb files.
 */
public interface DebAnalyst {

    /**
     * Creates a new analyst with the default implementation.
     * @param debFile deb file pathname
     * @return an analyst
     */
    static DebAnalyst createNew(File debFile) {
        return new DpkgDebAnalyst(debFile);
    }

    /**
     * Extracts files from this deb to the given directory.
     * This is equivalent to {@code dpkg-deb --extract}.
     * @param persistentDir extraction directory
     * @return extraction instance, possibly from cache
     * @throws IOException on I/O error
     */
    DebExtraction extract(Path persistentDir) throws IOException;

    /**
     * Gets the pathname of the deb file this analyst analyzes.
     * @return pathname
     */
    File getDebFile();

    /**
     * Extracts description of contents of a deb file.
     * This is equivalent to {@code dpkg-deb --contents}.
     * @return contents instance
     */
    DebContents contents() throws DebUtilsException;

    /**
     * Extracts packaging files.
     * This is equivalent to {@code dpkg-deb --control}.
     * @return control instance
     * @throws DebUtilsException
     */
    DebControl control() throws DebUtilsException;

    DebControl control(Path scratchDir) throws DebUtilsException;

    /**
     * Extracts deb file information.
     * This is equivalent to {@code dpkg-deb --info}.
     * @return a deb info instance
     */
    DebInfo info() throws DebUtilsException;

}
