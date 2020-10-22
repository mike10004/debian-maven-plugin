package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class DebContents {

    private final List<DebEntry> index;

    public DebContents(List<DebEntry> index) {
        this.index = List.copyOf(index);
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

    public List<DebEntry> index() {
        return index;
    }
}
