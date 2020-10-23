package io.github.mike10004.debutils;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Interface that provides access to output of extraction of a deb file.
 */
public interface DebExtraction {

    /**
     * Finds a file in the extracted file set by matching
     * against the pathname it would have when the deb package is
     * installed on a system.
     *
     * For example, if the deb file installs {@code /usr/bin/foo},
     * then the argument {@code "/usr/bin/foo"} would return the corresponding
     * file rooted in the {@link #getExtractionDir() extraction directory}.
     *
     * @param pathname pathname of installed file
     * @return file or empty optional if not found
     */
    Optional<File> findByInstalledPathname(String pathname);

    /**
     * Gets the pathname of the deb file from which this extraction
     * was produced.
     * @return deb file
     */
    File getDebFile();

    /**
     * Gets the pathname of the directory where files were extracted.
     * @return extraction directory
     */
    Path getExtractionDir();

    /**
     * Gets the set of extracted files. This includes only pathnames
     * that represents files and links (no directories).
     * @return file set
     */
    Collection<File> getFiles();
}
