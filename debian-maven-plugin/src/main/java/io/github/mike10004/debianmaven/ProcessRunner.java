package io.github.mike10004.debianmaven;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.util.Map;

public interface ProcessRunner {

    default void runProcess(String[] cmd, @SuppressWarnings("SameParameterValue") NonzeroProcessExitAction nonzeroExitAction) throws IOException, MojoExecutionException {
        runProcess(cmd, Collections.emptyMap(), nonzeroExitAction);
    }
    void runProcess(String[] cmd, Map<String, String> env, @SuppressWarnings("SameParameterValue") NonzeroProcessExitAction nonzeroExitAction) throws IOException, MojoExecutionException;

    default byte[] runProcessWithOutput(String[] cmd, @SuppressWarnings("SameParameterValue") NonzeroProcessExitAction nonzeroExitAction) throws IOException, MojoExecutionException {
        return runProcessWithOutput(cmd, Collections.emptyMap(), nonzeroExitAction);
    }

    byte[] runProcessWithOutput(String[] cmd, Map<String, String> env, @SuppressWarnings("SameParameterValue") NonzeroProcessExitAction nonzeroExitAction) throws IOException, MojoExecutionException;

}
