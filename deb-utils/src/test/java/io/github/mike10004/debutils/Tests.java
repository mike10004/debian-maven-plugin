package io.github.mike10004.debutils;

import java.io.File;
import java.net.URISyntaxException;

public class Tests {

    private Tests() {
    }

    public static File getGnuHelloDeb() {
        try {
            return new File(Tests.class.getResource("/hello_2.10-1build1_amd64.deb").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
