package io.github.mike10004.debianmaven;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.Arrays;

/**
 * Action to perform if a process exit code is nonzero.
 * @see ProcessRunner#runProcess(String[], NonzeroProcessExitAction)
 * @see AbstractDebianMojo#runProcess(String[], NonzeroProcessExitAction)
 */
public interface NonzeroProcessExitAction {

    void perform(int exitval, String[] cmdline) throws MojoExecutionException;

    static NonzeroProcessExitAction doNothing() {
        return (x, c) -> {};
    }

    static NonzeroProcessExitAction throwMojoExecutionException() {
        return  new NonzeroProcessExitAction() {
            @Override
            public void perform(int exitval, String[] cmdline) throws MojoExecutionException {
                throw new MojoExecutionException("Process returned non-zero exit code: " + exitval + " from command " + Arrays.toString(cmdline));
            }
        };
    }
}
