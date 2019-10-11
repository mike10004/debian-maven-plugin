package net.sf.debianmaven;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

public abstract class AbstractDebianMojo extends AbstractMojo
{
	private static final String SKIP_DEB_PROPERTY = "skipDeb";
	private static final String RUN_DEB_PROPERTY = "runDeb";

	/**
	 * Package name.
	 * @parameter property="deb.package.name" default-value="${project.artifactId}"
	 */
	protected String packageName;

	/**
	 * Package version.
	 * @parameter property="deb.package.version" default-value="${project.version}"
	 */
	private String packageVersion;

	/**
	 * Package revision.
	 * @parameter property="deb.package.revision" default-value="1"
	 */
	protected String packageRevision;

	/**
	 * Package architecture.
	 * @parameter property="deb.package.architecture" default-value="all"
	 */
	protected String packageArchitecture;

	/**
	 * Maintainer name.
	 * @parameter property="deb.maintainer.name" default-value="${project.developers[0].name}"
	 */
	protected String maintainerName;

	/**
	 * Maintainer email.
	 * @parameter property="deb.maintainer.email" default-value="${project.developers[0].email}"
	 */
	protected String maintainerEmail;

	/**
	 * Source directory pathname.
	 * @parameter default-value="${basedir}/src/deb"
	 */
	protected File sourceDir;

	/**
	 * Target directory pathname.
	 * @parameter default-value="${basedir}/target"
	 */
	protected File targetDir;

	/**
	 * Staging directory pathname.
	 * @parameter default-value="${basedir}/target/deb"
	 */
	protected File stageDir;

	/**
	 * Snapshot revision file.
	 * @parameter property="deb.package.snapshotRevFile"
	 * @since 1.0.5
	 */
	private File snapshotRevisionFile = null;

	/**
	 * Process execution mode. Use {@code legacy} for the old behavior or {@code subprocess}
	 * to use a modern library for subprocess execution.
	 * @parameter property="deb.process.executionMode" default-value="subprocess"
	 */
	protected String processExecutionMode;

	private static final DateFormat DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");

	/**
	 * Snapshot revision string.
	 * @parameter
	 * @since 1.0.9
	 */
	private String snapshotRevision = null;

	protected String processVersion(String version)
	{
		if (snapshotRevision == null)
		{
			Date revtime = snapshotRevisionFile != null
					? new Date(snapshotRevisionFile.lastModified())
					: new Date();

			snapshotRevision = "+" + DEFAULT_SNAPSHOT_TIMESTAMP_FORMAT.format(revtime);
		}

		return version.replaceAll("-SNAPSHOT", snapshotRevision);
	}

	protected String[] processVersion(String[] versions)
	{
		String[] result = new String[versions.length];
		for (int i=0 ; i<versions.length ; i++)
			result[i] = processVersion(versions[i]);

		return result;
	}

	protected String getPackageVersion()
	{
		return processVersion(packageVersion);
	}

	protected File getPackageFile()
	{
		return new File(targetDir, String.format("%s_%s-%s_all.deb", packageName, getPackageVersion(), packageRevision));
	}

	/**
	 * Runs a process and throws a mojo execution exception if the process exit code is nonzero.
	 * @param cmd the command line
	 * @throws ExecuteException
	 * @throws IOException
	 * @throws MojoExecutionException
	 */
	protected void runProcess(String[] cmd) throws ExecuteException, IOException, MojoExecutionException {
		runProcess(cmd, NonzeroProcessExitAction.throwMojoExecutionException());
	}

	protected void runProcess(String[] cmd, @SuppressWarnings("SameParameterValue") NonzeroProcessExitAction nonzeroExitAction) throws ExecuteException, IOException, MojoExecutionException
	{
		createProcessRunner().runProcess(cmd, nonzeroExitAction);
	}

	protected ProcessRunner createProcessRunner() {
		String mode = processExecutionMode;
		if (LegacyProcessRunner.PARAM_VALUE.equalsIgnoreCase(mode)) {
			return new LegacyProcessRunner(this::getLog);
		}
		if (mode != null && !mode.isEmpty() && !SubprocessProcessRunner.PARAM_VALUE.equalsIgnoreCase(mode)) {
			getLog().warn("processExecutionMode parameter value is not recognized; using default");
		}
		return new SubprocessProcessRunner(this::getLog);
	}

	protected abstract void executeDebMojo() throws MojoExecutionException;

	public final void execute() throws MojoExecutionException
	{
		if (System.getProperties().containsKey(RUN_DEB_PROPERTY))
		{
			getLog().info("debian-maven execution forced (-DrunDeb)");
			executeDebMojo();
		}
		else if (System.getProperties().containsKey(SKIP_DEB_PROPERTY))
			getLog().info("debian-maven execution skipped (-DskipDeb)");
		else if (!System.getProperty("os.name").equals("Linux"))
			getLog().warn("debian-maven execution skipped (non-linux OS)");
		else
			executeDebMojo();
	}

	void setPackageVersion(String packageVersion) {
		this.packageVersion = packageVersion;
	}
}
