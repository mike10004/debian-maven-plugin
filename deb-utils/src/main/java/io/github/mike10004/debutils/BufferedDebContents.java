package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

class BufferedDebContents implements DebContents {

    private final List<DebEntry> index;

    public BufferedDebContents(List<DebEntry> index) {
        this.index = List.copyOf(index);
    }

    @Override
    @Nullable
    public DebEntry findEntry(Predicate<? super DebEntry> filter) {
        return index().stream().filter(filter).findFirst().orElse(null);
    }

    @Override
    public List<DebEntry> index() {
        return index;
    }
}
