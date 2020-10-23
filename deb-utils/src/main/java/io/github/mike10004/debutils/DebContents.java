package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Interface of a value class that represents a listing of the contents of a deb file.
 */
public interface DebContents {
    /**
     * Returns the first entry that is accepted by a predicate.
     * @param filter entry filter
     * @return entry or null if not found
     */
    @Nullable
    DebEntry findEntry(Predicate<? super DebEntry> filter);

    /**
     * Finds an entry by matching its name field. The name field is expressed
     * as an absolute pathname, where directory does not have a trailing slash.
     * @param name entry name; this always starts with {@code /}; if argument does not
     * start with {@code /}, no entry will be found
     * @return entry or null if not found
     * @see DebEntry#name
     */
    @Nullable
    default DebEntry findEntryByName(String name) {
        return findEntry(entry -> name.equals(entry.name));
    }

    /**
     * Gets the list of entries.
     * @return list of entries
     */
    List<DebEntry> index();
}
