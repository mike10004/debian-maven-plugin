package io.github.mike10004.debianmaven;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.Assert;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

public abstract class ProcessRunnerTestBase {

    protected void runProcess(LogBucket logBucket, String[] cmdline) throws IOException, MojoExecutionException {
        runProcess(logBucket, cmdline, failOnNonzeroExit());
    }

    protected void runProcess(LogBucket logBucket, String[] cmdline, NonzeroProcessExitAction nonzeroAction) throws IOException, MojoExecutionException {
        ProcessRunner runner = createRunner(logBucket);
        runner.runProcess(cmdline, nonzeroAction);
    }

    private static NonzeroProcessExitAction failOnNonzeroExit() {
        return new NonzeroProcessExitAction() {
            @Override
            public void perform(int exitval, String[] cmdline) throws MojoExecutionException {
                Assert.fail(String.format("exit %s from %s", exitval, Arrays.toString(cmdline)));
            }
        };
    }

    protected String runProcess(String[] cmdline) throws IOException, MojoExecutionException {
        return runProcess(cmdline, failOnNonzeroExit());
    }

    protected String runProcess(String[] cmdline, NonzeroProcessExitAction nonzeroAction) throws IOException, MojoExecutionException {
        LogBucket bucket = new LogBucket();
        runProcess(bucket, cmdline, nonzeroAction);
        return bucket.dump();
    }

    protected ProcessRunner createRunner(LogBucket logBucket) {
        return createRunner(() -> logBucket);
    }
    protected abstract ProcessRunner createRunner(Supplier<Log> logGetter);

}
