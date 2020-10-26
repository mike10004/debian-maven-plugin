package io.github.mike10004.debianmaven;

import java.io.IOException;
import java.util.Collections;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Checks whether the generated package complies to style rules.
 *
 * Uses external utility: <a href="http://lintian.debian.org/">lintian</a>.
 *
 * @goal check
 * @phase package
 */
public class CheckMojo extends AbstractDebianMojo
{
	private void runLintian() throws IOException, MojoExecutionException
	{
		runProcess(new String[]{"lintian", getPackageFile().toString()}, Collections.emptyMap());
	}

	protected void executeDebMojo() throws MojoExecutionException
	{
		try
		{
			runLintian();
		}
		catch (IOException e)
		{
			getLog().error(e.toString());
			throw new MojoExecutionException(e.toString());
		}
	}
}
