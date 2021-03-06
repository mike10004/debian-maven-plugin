package io.github.mike10004.debianmaven;

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Cleans up all files generated by this plugin.
 *
 * @goal clean
 * @phase clean
 */
public class CleanMojo extends AbstractDebianMojo
{
	protected void executeDebMojo() throws MojoExecutionException
	{
		try
		{
			FileUtils.deleteDirectory(stageDir);
			getPackageFile().delete();
		}
		catch (IOException e)
		{
			throw new MojoExecutionException(e.toString());
		}
	}
}
