package io.github.mike10004.debutils;

import javax.annotation.Nullable;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Value class that represents an entry in a deb file.
 */
public class DebEntry {

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
     * Target of symbolic link, if this entry represents a symbolic link.
     */
    @Nullable
    public final String linkTarget;

    public DebEntry(String name, String permissions, String ownership, long size, String datetime, @Nullable String linkTarget) {
        this.name = name;
        this.permissions = permissions;
        this.ownership = ownership;
        this.size = size;
        this.datetime = datetime;
        this.linkTarget = linkTarget;
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
        return new StringJoiner(", ", DebEntry.class.getSimpleName() + "[", "]")
                .add("name=" + (name == null ? "null" : "'" + name + "'"))
                .add("permissions=" + (permissions == null ? "null" : "'" + permissions + "'"))
                .add("ownership=" + (ownership == null ? "null" : "'" + ownership + "'"))
                .add("size=" + size)
                .add("datetime=" + (datetime == null ? "null" : "'" + datetime + "'"))
                .toString();
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
     * Gets the entry type. I've never seen an entry that is not one of the
     * types enumerated in {@link DebEntryType}, and if this entry is such an
     * unenumerated type, this method will throw an exception. If you need to examine
     * type but it is not one of the enumerated types, use {@link #getEntryTypeRaw()}.
     * @return entry type
     * @throws IllegalArgumentException if entry is not one of the enumerated types
     */
    public DebEntryType getEntryType() {
        return DebEntryType.from(getEntryTypeRaw());
    }

}
