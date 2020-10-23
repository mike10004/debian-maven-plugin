package io.github.mike10004.debutils;

import javax.annotation.Nullable;

/**
 * Interface that provides access to values from a deb control file.
 */
public interface DebInfo {

    /**
     * Gets the value of a field. Fields are the strings such as
     * {@code Package}, {@code Depends}, and so on in the control file.
     *
     * @param field field name (case-sensitive)
     * @return field value or null if not present
     */
    @Nullable
    String getValue(String field);

}
