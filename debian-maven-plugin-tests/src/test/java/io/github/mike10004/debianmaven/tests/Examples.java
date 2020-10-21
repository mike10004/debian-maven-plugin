package io.github.mike10004.debianmaven.tests;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public class Examples {

    private Examples() {

    }

    public static Path getDirectory(String exampleName) {
        URL resource = Examples.class.getResource(String.format("/examples/%s/pom.xml", exampleName));
        try {
            return new File(resource.toURI()).getAbsoluteFile().getParentFile().toPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
