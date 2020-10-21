package net.sf.debianmaven;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import java.util.Random;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class LegacyProcessRunnerTest extends ProcessRunnerTestBase {

    @Override
    protected ProcessRunner createRunner(Supplier<Log> logGetter) {
        return new LegacyProcessRunner(logGetter);
    }

    @Test
    public void testEcho() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        String arg = String.valueOf(random.nextLong());
        String[] cmd = {"echo", arg};
        String output = runProcess(cmd);
        String expectedOutput = String.format(
                "[INFO] Start process: echo %s%n" +
                "[WARNING] %s%n",
                arg, arg);
        assertEquals("output", expectedOutput, output);
    }


}