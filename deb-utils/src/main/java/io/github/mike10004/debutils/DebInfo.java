package io.github.mike10004.debutils;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class DebInfo {

    private final String text;
    private final List<String> lines;

    public DebInfo(String text) {
        this.text = text;
        lines = text.lines().collect(Collectors.toList());
    }

    @Nullable
    public String getValue(String field) {
        String prefix = " " + field + ":";
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(prefix)) {
                return concatenateValue(lines.subList(i, lines.size()), prefix);
            }
        }
        return null;
    }

    private static String concatenateValue(List<String> linesStartingAtField, String prefix) {
        if (linesStartingAtField.isEmpty()) {
            return "";
        }
        String firstLine = linesStartingAtField.get(0);
        StringBuilder b = new StringBuilder();
        String firstLineValue = StringUtils.removeStart(firstLine, prefix).stripLeading();
        b.append(firstLineValue);
        // then add remaining lines that are indented with two spaces, until a line is not indented with two spaces
        for (int i = 1; i < linesStartingAtField.size(); i++) {
            String line = linesStartingAtField.get(i);
            if (line.startsWith("  ")) {
                b.append(" ").append(line.stripLeading());
            } else {
                break;
            }
        }
        return b.toString();
    }

    public String getText() {
        return text;
    }
}
