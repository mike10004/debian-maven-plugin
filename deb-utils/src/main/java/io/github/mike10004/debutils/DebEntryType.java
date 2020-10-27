package io.github.mike10004.debutils;

/**
 * Enumeration of constants that represent deb entry types.
 */
public enum DebEntryType {
    FILE,
    DIRECTORY,
    LINK;

    /**
     * Gets the entry type from the character in the deb contents index.
     * @param ch character
     * @return entry type
     * @throws IllegalArgumentException if character does not correspond to an enumerated entry type
     */
    public static DebEntryType from(char ch) throws IllegalArgumentException {
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
