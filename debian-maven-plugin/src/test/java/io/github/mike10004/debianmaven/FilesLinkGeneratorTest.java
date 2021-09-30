package io.github.mike10004.debianmaven;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class FilesLinkGeneratorTest {

    @Test
    public void relativizeSourcePath() throws Exception {
        List<Pair<RelativizeTestCase, Path>> failures = new ArrayList<>();
        for (RelativizeTestCase originalTestCase : new RelativizeTestCase[] {
                RelativizeTestCase.of("/files/are/sibling-source", "/files/are/sibling-link", "sibling-source"),
                RelativizeTestCase.of("/etc/tomcat9", "/var/lib/tomcat9/conf", "/etc/tomcat9"),
                RelativizeTestCase.of("/var/log/tomcat9", "/var/lib/tomcat9/logs", "../../log/tomcat9"),
                RelativizeTestCase.of("/a/b/c/d", "/a/x/y", "../b/c/d"),
                RelativizeTestCase.of("/a/b/c", "/a/x/y/z", "../../b/c"),

        }) {
            for (RelativizeTestCase testCase : new RelativizeTestCase[] {
                    originalTestCase,
                    originalTestCase.appendSeparatorToLink(),
                    originalTestCase.appendSeparatorToSource(),
                    originalTestCase.appendSeparatorsToBoth(),
            }) {
                Path actual = new FilesLinkGenerator().relativizeSourcePath(testCase.sourcePath(), testCase.linkPath());
                if (!testCase.expected().equals(actual)) {
                    failures.add(Pair.of(testCase, actual));
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(failures.size()).append(" failures:").append(System.lineSeparator());
        for (Pair<RelativizeTestCase, Path> failure : failures) {
            sb.append(failure.getLeft().sourcePath())
                    .append(' ')
                    .append(failure.getLeft().linkPath())
                    .append(" expected ")
                    .append(failure.getLeft().expected())
                    .append(" was ")
                    .append(failure.getRight())
                    .append(System.lineSeparator());
        }
        assertTrue(sb.toString(), failures.isEmpty());
    }

    private static class RelativizeTestCase extends MutableTriple<String, String, Path> {

        public RelativizeTestCase(String sourcePath, String linkPath, Path expected) {
            super(sourcePath, linkPath, expected);
        }

        public RelativizeTestCase appendSeparatorsToBoth() {
            return new RelativizeTestCase(sourcePath() + "/", linkPath() + "/", expected());
        }

        public RelativizeTestCase appendSeparatorToSource() {
            return new RelativizeTestCase(sourcePath() + "/", linkPath(), expected());
        }

        public RelativizeTestCase appendSeparatorToLink() {
            return new RelativizeTestCase(sourcePath(), linkPath() + "/", expected());
        }

        public Path expected() {
            return getRight();
        }

        public String linkPath() {
            return getMiddle();
        }

        public String sourcePath() {
            return getLeft();
        }

        public static RelativizeTestCase of(String sourcePath, String linkPath, String expected) {
            return new RelativizeTestCase(sourcePath, linkPath, Path.of(expected));
        }
    }

    @ClassRule
    public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createLinks() throws IOException, MojoExecutionException {
        File linksFile = temporaryFolder.newFile();
        java.nio.file.Files.writeString(linksFile.toPath(),
                "/etc/tomcat9                                /var/lib/tomcat9/conf\n" +
                "/var/cache/tomcat9                          /var/lib/tomcat9/work\n" +
                "/var/log/tomcat9                            /var/lib/tomcat9/logs\n" +
                "# this is a comment\n" +
                "/usr/share/doc/tomcat9-common/README.Debian /usr/share/doc/tomcat9/README.Debian\n",
                StandardCharsets.UTF_8);
        Path stageDir = temporaryFolder.newFolder().toPath();

        new FilesLinkGenerator().generateLinks(new File[]{linksFile}, stageDir);

        List<Pair<String, String>> expectedLinkAndSourceList = List.of(
                Pair.of("var/lib/tomcat9/conf", "/etc/tomcat9"),
                Pair.of("var/lib/tomcat9/work", "../../cache/tomcat9"),
                Pair.of("var/lib/tomcat9/logs", "../../log/tomcat9"),
                Pair.of("usr/share/doc/tomcat9/README.Debian", "../tomcat9-common/README.Debian")
        );

        for (Pair<String, String> expectedLinkAndSource : expectedLinkAndSourceList) {
            String relativePathToLink = expectedLinkAndSource.getLeft();
            Path actualLink = stageDir.resolve(relativePathToLink);
            Path actualTarget = java.nio.file.Files.readSymbolicLink(actualLink);
            String expectedTarget = expectedLinkAndSource.getRight();
            assertEquals("target", expectedTarget, actualTarget.toString());
        }

    }
}