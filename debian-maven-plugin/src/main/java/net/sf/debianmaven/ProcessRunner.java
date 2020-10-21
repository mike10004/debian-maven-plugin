package net.sf.debianmaven;

import org.apache.commons.exec.ExecuteException;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;

public interface ProcessRunner {

    void runProcess(String[] cmd, @SuppressWarnings("SameParameterValue") NonzeroProcessExitAction nonzeroExitAction) throws ExecuteException, IOException, MojoExecutionException;

    byte[] runProcessWithOutput(String[] cmd, @SuppressWarnings("SameParameterValue") NonzeroProcessExitAction nonzeroExitAction) throws ExecuteException, IOException, MojoExecutionException;

}
