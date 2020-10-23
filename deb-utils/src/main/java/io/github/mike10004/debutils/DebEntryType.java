package io.github.mike10004.debutils;

/**
 * Enumeration of constants that represent deb entry types.
 */
public enum DebEntryType {
    FILE,
    DIRECTORY,
    LINK;

    public static DebEntryType from(char ch) {
        switch (ch) {
            case '-':
                return FILE;
            case 'd':
                return DIRECTORY;
            case 'l':
                return LINK;
        }
        throw new IllegalArgumentException(String.format("character: %s (expect one of {-, d, l})", ch));
    }
}
