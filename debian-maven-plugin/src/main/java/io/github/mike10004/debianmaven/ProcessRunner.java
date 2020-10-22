package io.github.mike10004.debianmaven;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;

public interface ProcessRunner {

    void runProcess(String[] cmd, @SuppressWarnings("SameParameterValue") NonzeroProcessExitAction nonzeroExitAction) throws IOException, MojoExecutionException;

    byte[] runProcessWithOutput(String[] cmd, @SuppressWarnings("SameParameterValue") NonzeroProcessExitAction nonzeroExitAction) throws IOException, MojoExecutionException;

}
