package io.github.mike10004.debianmaven;

import com.opencsv.CSVParserBuilder;
import com.opencsv.ICSVParser;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DefaultLinksLineParserTest {

    @Test
    public void parseLine() throws Exception {

        String text = "/most/normal /case/possible\n" +
                "/multiple/spaces       /between/paths\n" +
                "/oh/no/they\t/used/tabs\n" +
                "/oh/no/they\t\t/used/multiple/tabs\n" +
                "/path/to/something       \"/path/to/thing with spaces\"\n" +
                "# commented line\n" +
                "\n" +
                "\"/path/has spaces\" /path/has/no/spaces\n" +
                "\"/both/paths have\" \"/spaces/in them\"\n";
        LinkSpecification[] expecteds = {
                new LinkSpecification("/most/normal", "/case/possible"),
                new LinkSpecification("/multiple/spaces", "/between/paths"),
                new LinkSpecification("/oh/no/they", "/used/tabs"),
                new LinkSpecification("/oh/no/they", "/used/multiple/tabs"),
                new LinkSpecification("/path/to/something", "/path/to/thing with spaces"),
                null,
                null,
                new LinkSpecification("/path/has spaces", "/path/has/no/spaces"),
                new LinkSpecification("/both/paths have", "/spaces/in them"),
        };
        List<Triple<String, LinkSpecification, LinkSpecification>> failures = new ArrayList<>();
        LinksLineParser parser = new DefaultLinksLineParser();
        List<String> textLines = text.lines().collect(Collectors.toList());
        for (int i = 0; i < textLines.size(); i++) {
            String line = textLines.get(i);
            @Nullable LinkSpecification expected = expecteds[i];
            @Nullable LinkSpecification actual = parser.parseSpecification(line);
            if (!Objects.equals(expected, actual)) {
                failures.add(Triple.of(line, expected, actual));
            }
        }
        assertEquals("failures", Collections.emptyList(), failures);

    }

    @Test
    public void badLines() throws Exception {
        for (String line : new String[]{
                "/one/token",
                "/a/b/c /d/e/f # comment at end",
                "/a/b/c /d/e/f #comment",
                "/a/b/c /d/e/f /three/tokens",
        }) {
            try {
                new DefaultLinksLineParser().parseSpecification(line);
                fail("should have thrown " + LinksLineParser.InvalidLinkSpecificationException.class.getSimpleName());
            } catch (LinksLineParser.InvalidLinkSpecificationException ignore) {
            }
        }
    }
}
