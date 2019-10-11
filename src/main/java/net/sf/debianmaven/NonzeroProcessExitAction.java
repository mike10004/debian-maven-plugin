package net.sf.debianmaven;

import org.apache.commons.exec.CommandLine;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Action to perform if a process exit code is nonzero.
 * @see #runProcess(String[], NonzeroProcessExitAction)
 */
public interface NonzeroProcessExitAction {

    void perform(int exitval, CommandLine cmdline) throws MojoExecutionException;

    static NonzeroProcessExitAction doNothing() {
        return (x, c) -> {};
    }

    static NonzeroProcessExitAction throwMojoExecutionException() {
        return  new NonzeroProcessExitAction() {
            @Override
            public void perform(int exitval, CommandLine cmdline) throws MojoExecutionException {
                throw new MojoExecutionException("Process returned non-zero exit code: "+cmdline);
            }
        };
    }
}
