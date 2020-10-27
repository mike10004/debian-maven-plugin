package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

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
     * file rooted in the {@link #extractionDirectory() extraction directory}.
     *
     * @param pathname pathname of installed file
     * @return file or null if not found
     */
    @Nullable
    File findByInstalledPathname(String pathname);

    /**
     * Gets the pathname of the directory where files were extracted.
     * @return extraction directory
     */
    Path extractionDirectory();

    /**
     * Streams a list of absolute pathnames that represent the installed locations
     * of files in the deb.
     * @return pathname stream
     */
    Stream<String> installedPathnames();
}
