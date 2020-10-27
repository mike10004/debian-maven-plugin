package io.github.mike10004.debutils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

class DebContentsLineParser {

    private static final Splitter splitter = Splitter.on(CharMatcher.whitespace())
            .limit(6)
            .omitEmptyStrings();

    public DebEntry parseEntry(String line) throws DebContentsLineParseException {
        try {
            List<String> parts = splitter.splitToList(line);
            String perms = parts.get(0);
            String ownership = parts.get(1).replace("/", ":");
            long size = Long.parseLong(parts.get(2));
            String datetime = String.format("%s %s", parts.get(3), parts.get(4));
            String name = StringUtils.removeStart(parts.get(5), ".");
            @Nullable String linkTarget = null;
            if (!perms.isEmpty() && perms.charAt(0) == 'l') {
                // this is imperfect, because " -> " is potentially a substring of a valid filename,
                // but that is such a bizarre edge case that maybe nobody will ever run into it
                String linkSeparator = " -> ";
                int linkSeparatorPos = name.indexOf(linkSeparator);
                if (linkSeparatorPos >= 0) {
                    linkTarget = name.substring(linkSeparatorPos + linkSeparator.length());
                    name = name.substring(0, linkSeparatorPos);
                } else {
                    LoggerFactory.getLogger(getClass()).debug("failed to parse link in deb contents text");
                }
            }
            return new DebEntry(name, perms, ownership, size, datetime, linkTarget);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new DebContentsLineParseException(e);
        }
    }

    static class DebContentsLineParseException extends DebUtilsException {
        public DebContentsLineParseException(String message) {
            super(message);
        }

        public DebContentsLineParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public DebContentsLineParseException(Throwable cause) {
            super(cause);
        }
    }
}
