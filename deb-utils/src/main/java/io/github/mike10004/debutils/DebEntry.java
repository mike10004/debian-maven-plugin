package io.github.mike10004.debutils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

/**
 * Value class that represents an entry in a deb file.
 */
public class DebEntry {

    private static final Splitter splitter = Splitter.on(CharMatcher.whitespace()).omitEmptyStrings();

    /**
     * Entry name, expressed as an absolute path where this file would be
     * in the filesystem if the deb were installed.
     */
    public final String name;

    /**
     * Permissions expressed in symbolic notation with leading type character.
     * This is a 10-character string whose first character represents the entry
     * type (commonly {@code -}, {@code d}, or {@code l} for file, directory, or link),
     * followed by three character triplets representing owner, group, and world permissions.
     * Examples of triplets are {@code rwx}, {@code r-x}, {@code rw-}, and {@code ---}.
     */
    private final String permissions;

    /**
     * File ownership expressed as {@code owner:group}.
     * Examples: {@code root:root}, {@code root:staff}, {@code root:1056}.
     */
    public final String ownership;

    /**
     * File length in bytes.
     */
    public final long size;

    /**
     * Date time formatted as {@code yyyy-MM-dd HH:mm}.
     */
    public final String datetime;

    /**
     * Output line from {@code dpkg-deb --contents} from which this entry's fields were parsed.
     */
    private final String contentsLine;

    public DebEntry(String name, String permissions, String ownership, long size, String datetime, String contentsLine) {
        this.name = name;
        this.permissions = permissions;
        this.ownership = ownership;
        this.size = size;
        this.datetime = datetime;
        this.contentsLine = contentsLine;
    }

    @Nullable
    public static DebEntry fromLine(String line) {
        try {
            List<String> parts = splitter.splitToList(line);
            String perms = parts.get(0);
            String ownership = parts.get(1).replace("/", ":");
            long size = Long.parseLong(parts.get(2));
            String datetime = String.format("%s %s", parts.get(3), parts.get(4));
            String name = StringUtils.removeStart(parts.get(5), ".");
            return new DebEntry(name, perms, ownership, size, datetime, line);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            LoggerFactory.getLogger(DebEntry.class).error("failed to parse line " + line, e);
            return null;
        }
    }

    /**
     * Gets the permissions set of the entry.
     * @return permissions
     */
    public Set<PosixFilePermission> getPermissions() {
        String nineChar = this.permissions.substring(1);
        return PosixFilePermissions.fromString(nineChar);
    }

    @Override
    public String toString() {
        return contentsLine;
    }

    /**
     * Gets the entry type expressed a character. Typical values
     * would be {@code -} (file), {@code d} (directory), or {@code l} (link).
     * @return entry type character
     */
    public char getEntryTypeRaw() {
        return permissions.charAt(0);
    }

    /**
     * Gets the entry type.
     * @return entry type
     */
    public DebEntryType getEntryType() {
        return DebEntryType.from(getEntryTypeRaw());
    }
}
