package io.github.mike10004.debianmaven;

import com.opencsv.CSVParserBuilder;
import com.opencsv.ICSVParser;
import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.plexus.util.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;

public class DefaultLinksLineParser implements LinksLineParser {

    public static final String COMMENT_INDICATOR = "#";

    private final String commentIndicator;
    private final ICSVParser lineTokenizer;

    public DefaultLinksLineParser() {
        this("#", createDefaultTokenizer());
    }

    private DefaultLinksLineParser(String commentIndicator, ICSVParser lineTokenizer) {
        this.commentIndicator = commentIndicator;
        this.lineTokenizer = lineTokenizer;
    }

    private static ICSVParser createDefaultTokenizer() {
        return new CSVParserBuilder()
                .withSeparator(' ')
                .withQuoteChar('\"')
                .withEscapeChar('\\')
                .withIgnoreLeadingWhiteSpace(true)
                .build();
    }

    private String cleanLine(String dirtyLine) {
        return dirtyLine.replace('\t', ' ');
    }

    @Nullable
    public LinkSpecification parseSpecification(String line) throws InvalidLinkSpecificationException {
        if (line == null || line.isBlank()) {
            return null;
        }
        line = line.stripLeading();
        if (line.isBlank()) {
            return null;
        }
        if (line.startsWith(commentIndicator)) {
            return null;
        }
        line = cleanLine(line);
        String[] tokens;
        try {
            tokens = lineTokenizer.parseLine(line);
        } catch (IOException e) {
            throw new InvalidLinkSpecificationException("tokenization failed", e);
        }
        if (tokens.length == 0) {
            return null;
        }
        tokens = Arrays.stream(tokens).filter(token -> {
            return token != null && !token.isBlank();
        }).toArray(String[]::new);
        if (tokens.length != 2) {
            throw new InvalidLinkSpecificationException("invalid number of tokens (" + tokens.length + ") on line: \"" + StringEscapeUtils.escapeJava(StringUtils.abbreviate(line, 512)) + "\"");
        }
        return new LinkSpecification(tokens[0], tokens[1]);
    }
}
