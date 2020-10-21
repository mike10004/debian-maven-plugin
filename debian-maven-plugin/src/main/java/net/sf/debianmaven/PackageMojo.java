package net.sf.debianmaven;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import static java.util.Objects.requireNonNull;

/**
 * Generates a Debian package.
 *
 * Uses Debian utilities: <a href="http://www.debian.org/doc/manuals/debian-faq/ch-pkgtools.en.html">dpkg-deb</a> and fakeroot.
 *
 * @goal package
 * @phase package
 * @requiresDependencyResolution
 */
public class PackageMojo extends AbstractDebianMojo
{
	/**
	 * Package priority.
	 * @required
	 * @parameter property="deb.package.priority" default-value="optional"
	 */
	protected String packagePriority;

	/**
	 * Package section.
	 * @required
	 * @parameter property="deb.package.section" default-value="contrib/utils"
	 */
	protected String packageSection;

	/**
	 * Package title.
	 * @required
	 * @parameter property="deb.package.title" default-value="${project.name}"
	 */
	protected String packageTitle;

	/**
	 * Package description.
	 * @required
	 * @parameter property="deb.package.description" default-value="${project.description}"
	 */
	protected String packageDescription;

	/**
	 * Package dependencies.
	 * @parameter property="deb.package.dependencies"
	 */
	protected String[] packageDependencies;

	/**
	 * Package conflicts.
	 * @parameter
	 */
	protected String[] packageConflicts;

	/**
	 * Project URL.
	 * @parameter property="${deb.project.url}" default-value="${project.organization.url}"
	 */
	protected String projectUrl;

	/**
	 * Project organization.
	 * @parameter property="deb.project.organization" default-value="${project.organization.name}"
	 */
	protected String projectOrganization;

	/**
	 * Jar to include.
	 * @parameter property="deb.include.jar"
	 */
	@Deprecated
	protected String includeJar;

	/**
	 * Jars to include.
	 * @parameter property="deb.includeJars"
	 */
	@Deprecated
	protected String[] includeJars;

	/**
	 * Flag that specifies whether to exclude all jars.
	 * @parameter property="deb.exclude.all-jars"
	 */
	@Deprecated
	protected String excludeAllJars;

	/**
	 * Artifacts to include.
	 * @parameter property="deb.includeArtifacts"
	 * @since 1.0.3
	 */
	@Deprecated
	protected Set<String> includeArtifacts;

	/**
	 * Artifacts to exclude.
	 * @parameter property="deb.excludeArtifacts"
	 * @since 1.0.3
	 */
	@Deprecated
	protected Set<String> excludeArtifacts;

	/**
	 * Flag that specifies whether to exclude all artifacts.
	 * @parameter property="deb.excludeAllArtifacts" default-value="false"
	 * @since 1.0.3
	 */
	@Deprecated
	protected boolean excludeAllArtifacts;

	/**
	 * Flag that specifies whether to exclude all dependencies.
	 * @parameter property="deb.excludeAllDependencies" default-value="false"
	 * @since 1.0.3
	 */
	@Deprecated
	protected boolean excludeAllDependencies;

	/**
	 * Flag that specifies whether to include attached artifacts.
	 * @parameter property="deb.includeAttachedArtifacts" default-value="true"
	 * @since 1.0.3
	 */
	@Deprecated
	protected boolean includeAttachedArtifacts;

	/**
	 * Package filename.
	 * @parameter property="deb.package.filename"
	 * @since 1.0.9
	 */
	protected String packageFilename;

	/**
	 * Additional lines in control files. Each element must have
	 * children {@code <field>} and {@code <value>}, and you may include
	 * {@link <after>} to specify placement within the control file.
	 */
	protected ControlFileLine[] control;

	/**
	 * Maven project object.
	 * 
	 * @parameter property="project"
	 */
	@SuppressWarnings("unused") // injected
	private MavenProject project;

	private File createTargetLibDir()
	{
		File targetLibDir = new File(stageDir, "usr/share/lib/" + packageName);
		targetLibDir.mkdirs();
		return targetLibDir;
	}

	private void createSymlink(File symlink, String target) throws MojoExecutionException, IOException
	{
		if (symlink.exists())
			//noinspection ResultOfMethodCallIgnored
			symlink.delete();

		runProcess(new String[]{"ln", "-s", target, symlink.toString()});
	}

	private void writeIncludeFile(File targetLibDir, String artifactId, String version, Collection<String> dependencies) throws IOException, MojoExecutionException
	{
		if (dependencies == null) {
			dependencies = Collections.emptySet();
		}

		File deplist = new File(targetLibDir, String.format("%s-%s.inc", artifactId, version));
		try (FileWriter out = new FileWriter(deplist)) {
			out.write(String.format("artifacts=%s\n", StringUtils.join(new HashSet<>(dependencies), ":")));
		}

		File symlink = new File(targetLibDir, String.format("%s.inc", artifactId));
		createSymlink(symlink, deplist.getName());
		getLog().info("wrote " + deplist.getName() + " with symlink " + symlink.getName() + " in " + targetLibDir);
	}

	private boolean includeArtifact(Artifact a)
	{
		boolean doExclude = excludeArtifacts != null && (a.getDependencyTrail() == null || Collections.disjoint(a.getDependencyTrail(), excludeArtifacts));
		if (doExclude)
			return false;

		if (includeArtifacts == null)
			return true;

		if (a.getDependencyTrail() == null)
			return true;

		return Collections.disjoint(a.getDependencyTrail(), includeArtifacts);
	}

	private File copyArtifact(Artifact a, File targetLibDir) throws IOException, MojoExecutionException
	{
		if (a.getFile() == null)
			throw new MojoExecutionException(String.format("No file was built for required artifact: %s:%s:%s", a.getGroupId(), a.getArtifactId(), a.getVersion()));

		getLog().info(String.format("Artifact: %s", a.getFile().getPath()));
		File src = a.getFile();
		File trg = new File(targetLibDir, src.getName());
		FileUtils.copyFile(src, trg);

		//TODO: which version should we use? trying both versions for now...
		String linkname = src.getName().replaceFirst("-"+a.getBaseVersion(), "");
		if (linkname.equals(src.getName()))
			linkname = linkname.replaceFirst("-"+a.getVersion(), "");

		if (!linkname.equals(src.getName()))
			createSymlink(new File(targetLibDir, linkname), a.getFile().getName());

		return trg;
	}

	@SuppressWarnings("unchecked")
	private void copyAttachedArtifacts() throws IOException, MojoExecutionException
	{
		if (!includeAttachedArtifacts)
		{
			getLog().info("Skipping attached project artifacts.");
			return;
		}

		getLog().info("Copying attached project artifacts.");
		File targetLibDir = createTargetLibDir();

		for (Artifact a : (Collection<Artifact>)project.getAttachedArtifacts())
			copyArtifact(a, targetLibDir);
	}

	@SuppressWarnings("unchecked")
	private void copyArtifacts() throws IOException, MojoExecutionException
	{
		if (excludeAllArtifacts)
		{
			getLog().info("Skipping regular project artifacts and dependencies.");
			return;
		}

		File targetLibDir = createTargetLibDir();

		Collection<Artifact> artifacts = new ArrayList<>();

		// consider the current artifact only if it exists (e.g. pom, war packaging generates no artifact)
		if (project.getArtifact().getFile() != null) {
			getLog().info("copying regular project artifact: " + project.getArtifact());
			artifacts.add(project.getArtifact());
		} else {
			getLog().info("file is not present in " + project.getArtifact());
		}

		if (excludeAllDependencies)
			getLog().info("Copying regular project artifacts but not dependencies.");
		else
		{
			getLog().info("Copying " + project.getArtifacts().size() + " regular project artifacts and dependencies.");
			for (Artifact a : (Collection<Artifact>)project.getArtifacts())
			{
				if (a.getScope().equals("runtime") || a.getScope().equals("compile"))
					artifacts.add(a);
			}
		}

		/*
		 * TODO: this code doesn't work as it should due to limitations of Maven API; see also:
		 * http://jira.codehaus.org/browse/MNG-4831
		 */

		Map<String,Artifact> ids = new HashMap<>();
		for (Artifact a : artifacts)
			ids.put(a.getId(), a);

		ArrayListValuedHashMap<Artifact,String> deps = new ArrayListValuedHashMap<>();
		for (Artifact a : artifacts)
		{
			if (includeArtifact(a))
			{
				File trg = copyArtifact(a, targetLibDir);

				if (a.getDependencyTrail() != null)
				{
					// if dependency is not already among artifacts to be included, add to deps collection
					for (String id : a.getDependencyTrail())
					{
						Artifact depending = ids.get(id);
						if (depending != null)
							deps.put(depending, trg.getPath().substring(stageDir.getPath().length()));
					}
				}
			}
		}

		for (Artifact a : artifacts)
		{
			if (includeArtifact(a)) {
				writeIncludeFile(targetLibDir, a.getArtifactId(), a.getVersion(), deps.get(a));
			} else {
				getLog().debug("excluded: " + a);
			}
		}
	}

	private void generateCopyright() throws IOException
	{
		File targetDocDir = new File(stageDir, "usr/share/doc/" + packageName);
		targetDocDir.mkdirs();
		File copyrightFile = new File(targetDocDir, "copyright");
		if (!copyrightFile.exists())
		{
			PrintWriter out = new PrintWriter(new FileWriter(copyrightFile));
			out.println(packageName);
			out.println(projectUrl);
			out.println();
			out.printf("Copyright %d %s\n", Calendar.getInstance().get(Calendar.YEAR), projectOrganization);
			out.println();
			out.println("The entire code base may be distributed under the terms of the GNU General");
			out.println("Public License (GPL).");
			out.println();
			out.println("See /usr/share/common-licenses/GPL");
			out.close();
		}
	}

	@Override
    protected File getPackageFile()
    {
        String filename = this.packageFilename;
        String packageArchitecture = this.packageArchitecture;
        if (packageArchitecture == null) {
        	packageArchitecture = "all";
		}
        if (filename == null) {
            filename = String.format("%s_%s-%s_%s.deb", packageName, getPackageVersion(), packageRevision, packageArchitecture);
        }
        return new File(targetDir, filename);
    }

    private void generateControl(File target) throws IOException {
		getLog().info("Generating control file: " + target);
		List<ControlFileLine> lines = new ArrayList<>(generateKnownControlLines());
		if (control != null) {
			lines.addAll(Arrays.asList(control));
		}
		lines = ControlFileLine.sorted(lines);
		try (PrintWriter out = new PrintWriter(new FileWriter(target))) {
			for (ControlFileLine line : lines) {
				out.println(String.format("%s: %s", line.getField(), line.getValue()));
			}
		}
		return;
	}

	private static void addIfValueNotNull(String field, @Nullable String value, Collection<ControlFileLine> lines) {
		addIfValueNotNull(field, value, null, lines);
	}

	private static void addIfValueNotNull(String field, @Nullable String value, @Nullable String after, Collection<ControlFileLine> lines) {
		requireNonNull(field, "field");
		if (value != null) {
			lines.add(new ControlFileLine(field, value, after));
		}
	}

    private List<ControlFileLine> generateKnownControlLines() throws IOException
	{
		List<ControlFileLine> lines = new ArrayList<>();
		addIfValueNotNull("Package", requireNonNull(packageName, "packageName"), lines);
		addIfValueNotNull("Version", requireNonNull(getPackageVersion()), lines);
		addIfValueNotNull("Section", packageSection, lines);
		addIfValueNotNull("Priority", packagePriority, lines);
		addIfValueNotNull("Architecture", requireNonNull(packageArchitecture), lines);
		if (packageDependencies != null && packageDependencies.length > 0) {
			addIfValueNotNull("Depends", StringUtils.join(processVersion(packageDependencies), ", "), lines);
		}
		if (packageConflicts != null && packageConflicts.length > 0) {
			addIfValueNotNull("Conflicts", StringUtils.join(processVersion(packageConflicts), ", "), lines);
		}
		addIfValueNotNull("Installed-Size", String.format("Installed-Size: %d", 1 + FileUtils.sizeOfDirectory(stageDir) / 1024), lines);
		String value = null;
		if (maintainerName != null && maintainerEmail == null) {
			value = maintainerName;
		} else if (maintainerName == null && maintainerEmail != null) {
			value = String.format("<%s>", maintainerEmail);
		} else if (maintainerName != null && maintainerEmail != null) {
			value = String.format("%s <%s>", maintainerName, maintainerEmail);
		}
		addIfValueNotNull("Maintainer", value, lines);
		addIfValueNotNull("Homepage", projectUrl, lines);
		addIfValueNotNull("Description", formatDescription(packageTitle, packageDescription), lines);
		return lines;
	}

	private String formatDescription(String packageTitle, String description) {
		if (packageTitle == null) {
			packageTitle = "Package Title";
		}
		if (packageTitle.length() > 60) {
			getLog().warn("Package title will be truncated to the upper limit of 60 characters.");
			packageTitle = packageTitle.substring(0, 60);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(packageTitle);
		if (description != null) {
			sb.append(System.lineSeparator());
			String descFormatted = packageDescription.trim();
			descFormatted = descFormatted.replaceAll("\\s+", " ");
			sb.append(' ').append(descFormatted.stripTrailing());
		}
		return sb.toString();
	}

	private void generateConffiles(File target) throws IOException
	{
		List<String> conffiles = new ArrayList<>();

		File configDir = new File(stageDir, "etc");
		if (configDir.exists())
		{
			Collection<File> files = FileUtils.listFiles(configDir, null, true);
			for (File f : files)
			{
				if (f.isFile())
					conffiles.add(f.toString().substring(stageDir.toString().length()));
			}
		}

		if (conffiles.size() > 0)
		{
			PrintWriter out = new PrintWriter(new FileWriter(target));
			for (String fname : conffiles)
				out.println(fname);
			out.close();
		}
	}

	private void generateMd5Sums(File target) throws IOException
	{
		PrintWriter out = new PrintWriter(new FileWriter(target));
		
		Collection<File> files = FileUtils.listFiles(stageDir, null, true);
		for (File f : files)
		{
			// check whether the file is a non-regular file
			if (!f.isFile())
				continue;
			
			// check whether the file is a possible link
			if (!f.getAbsolutePath().equals(f.getCanonicalPath()))
				continue;

			String fname = f.toString().substring(stageDir.toString().length() + 1);
			if (!fname.startsWith("DEBIAN"))
			{
				FileInputStream fis = new FileInputStream(f);
				String md5 = DigestUtils.md5Hex(fis);
				fis.close();
				
				out.printf("%s  %s\n", md5, fname);
			}
		}
		
		out.close();
	}

	/**
	 * TODO: implement using {@link SubprocessProcessRunner}
	 */
	private void generateManPages() throws MojoExecutionException, IOException
	{
		File source = new File(sourceDir, "man");
		if (!source.exists())
		{
			getLog().info("No manual page directory found: "+source);
			return;
		}

		int npages = 0;
		Collection<File> files = FileUtils.listFiles(source, null, true);
		for (File f : files)
		{
			if (f.isFile() && f.getName().matches(".*[.][1-9]$"))
			{
				char section = f.getName().charAt(f.getName().length()-1);
				File target = new File(stageDir, String.format("usr/share/man/man%c/%s.gz", section, f.getName()));
				target.getParentFile().mkdirs();

				CommandLine cmdline = new CommandLine("groff");
				cmdline.addArguments(new String[]{"-man", "-Tascii", f.getPath()});

				getLog().info("Start process: "+cmdline);

				try (GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(target))) {
					PumpStreamHandler streamHandler = new PumpStreamHandler(os, new LogOutputStream(getLog()));
					DefaultExecutor exec = new DefaultExecutor();
					exec.setWorkingDirectory(f.getParentFile());
					exec.setStreamHandler(streamHandler);
					int exitval = exec.execute(cmdline);
					if (exitval == 0)
						getLog().info("Manual page generated: " + target.getPath());
					else {
						getLog().warn("Exit code " + exitval);
						throw new MojoExecutionException("Process returned non-zero exit code: " + cmdline);
					}
				}

				npages++;
			}
		}

		if (npages == 0)
			getLog().info("No manual pages found in directory: "+source);
	}

	private void generatePackage() throws IOException, MojoExecutionException
	{
		runProcess(new String[]{"fakeroot", "--", "dpkg-deb", "--build", stageDir.toString(), getPackageFile().toString()});
	}

	private void checkDeprecated(boolean haveParameter, String paramName) throws MojoExecutionException
	{
		if (haveParameter)
			throw new MojoExecutionException("Deprecated parameter used: "+paramName);
	}

	protected void executeDebMojo() throws MojoExecutionException
	{
		checkDeprecated(includeJar != null, "includeJar");
		checkDeprecated(includeJars != null && includeJars.length > 0, "includeJars");
		checkDeprecated(excludeAllJars != null, "excludeAllJars");

		File targetDebDir = new File(stageDir, "DEBIAN");
		if (!targetDebDir.exists() && !targetDebDir.mkdirs())
			throw new MojoExecutionException("Unable to create directory: "+targetDebDir);

		try
		{
			generateManPages();
			copyAttachedArtifacts();
			copyArtifacts();
			generateCopyright();
			generateConffiles(new File(targetDebDir, "conffiles"));
			generateControl(new File(targetDebDir, "control"));
			generateMd5Sums(new File(targetDebDir, "md5sums"));
			generatePackage();
		}
		catch (IOException e)
		{
			getLog().error(e.toString());
			throw new MojoExecutionException(e.toString());
		}
	}
}
