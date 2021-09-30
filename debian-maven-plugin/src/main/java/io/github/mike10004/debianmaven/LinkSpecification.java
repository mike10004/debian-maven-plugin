package io.github.mike10004.debianmaven;

import org.apache.commons.lang3.tuple.MutablePair;

public class LinkSpecification extends MutablePair<String, String> {
    public LinkSpecification(String source, String link) {
        super(source, link);
    }

    public String sourcePath() {
        return getLeft();
    }

    public String linkPath() {
        return getRight();
    }
}
