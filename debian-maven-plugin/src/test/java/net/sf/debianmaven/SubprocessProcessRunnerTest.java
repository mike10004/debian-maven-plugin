package net.sf.debianmaven;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import java.util.Random;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class SubprocessProcessRunnerTest extends ProcessRunnerTestBase {

    @Override
    protected ProcessRunner createRunner(Supplier<Log> logGetter) {
        return new SubprocessProcessRunner(logGetter);
    }

    @Test
    public void testEcho() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        testEcho(random);
    }

    private void testEcho(Random random) throws Exception {
        String arg = String.valueOf(random.nextLong());
        String[] cmd = {"echo", arg};
        String output = runProcess(cmd);
        String expectedOutput = String.format("[INFO] %s%n", arg);
        assertEquals("output", expectedOutput, output);
    }

    @Test // in case of race condition in the implementation
    public void testEchoManyTimes() throws Exception {
        Random random = new Random(getClass().getName().hashCode());
        for (int i = 0; i < 1000; i++) {
//            System.out.format("%ntrial %s%n", i  + 1);
            testEcho(random);
        }
    }

}