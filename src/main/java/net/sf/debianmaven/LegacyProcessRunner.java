package net.sf.debianmaven;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class LegacyProcessRunner implements ProcessRunner {

    public static final String PARAM_VALUE = "legacy";

    private final Supplier<Log> logGetter;

    LegacyProcessRunner(Supplier<Log> logGetter) {
        this.logGetter = requireNonNull(logGetter);
    }

    private Log getLog() {
        return logGetter.get();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void runProcess(String[] cmd, NonzeroProcessExitAction nonzeroExitAction) throws IOException, MojoExecutionException {
        CommandLine cmdline = new CommandLine(cmd[0]);
        cmdline.addArguments(Arrays.copyOfRange(cmd, 1, cmd.length));

        getLog().info("Start process: "+cmdline);

        PumpStreamHandler streamHandler = new PumpStreamHandler(new LogOutputStream(getLog()));
        DefaultExecutor exec = new DefaultExecutor();
        exec.setStreamHandler(streamHandler);
        int exitval = exec.execute(cmdline);
        if (exitval != 0) {
            getLog().warn("Exit code "+exitval);
            nonzeroExitAction.perform(exitval, cmd);
        }

    }
}
