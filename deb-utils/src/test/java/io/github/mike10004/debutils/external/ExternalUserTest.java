package io.github.mike10004.debutils.external;

import io.github.mike10004.debutils.DebAnalyst;
import io.github.mike10004.debutils.DebUtilsException;
import io.github.mike10004.debutils.Tests;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Tests of compile-time constraints we want to impose.
 * One such constraint is that we do not want to throw package-private exceptions.
 */
public class ExternalUserTest {

    @Test
    public void noPackagePrivateExceptions() throws DebUtilsException {
        assertFalse(DebAnalyst.createNew(Tests.getGnuHelloDeb()).contents().index().isEmpty());
    }
}
