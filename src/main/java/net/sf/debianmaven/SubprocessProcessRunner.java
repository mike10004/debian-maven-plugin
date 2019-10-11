package net.sf.debianmaven;

import org.apache.commons.exec.ExecuteException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class SubprocessProcessRunner implements ProcessRunner {

    public static final String PARAM_VALUE = "subprocess";

    private final Supplier<Log> logGetter;

    SubprocessProcessRunner(Supplier<Log> logGetter) {
        this.logGetter = requireNonNull(logGetter);
    }

    @Override
    public void runProcess(String[] cmd, NonzeroProcessExitAction nonzeroExitAction) throws ExecuteException, IOException, MojoExecutionException {

    }
}
