package net.sf.debianmaven;

import java.io.File;

public abstract class AbstractRepreproMojo extends AbstractDebianMojo
{
	/**
	 * Repository directory pathname.
	 * @parameter property="deb.repository.location"
	 * @required
	 */
	protected File repository;

	/**
	 * Configuration directory pathname.
	 * @parameter property="deb.reprepro.config"
	 * @required
	 */
	protected File repreproConfigurationDir;
}
