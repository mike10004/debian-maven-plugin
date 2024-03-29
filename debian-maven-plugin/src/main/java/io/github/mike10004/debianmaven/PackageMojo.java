package io.github.mike10004.debianmaven;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
	 * Package filename.
	 * @parameter property="deb.package.filename"
	 * @since 1.0.9
	 */
	protected String packageFilename;

	/**
	 * Additional lines in control files. Each element must have
	 * children {@code <field>} and {@code <value>}, and you may include
	 * {@code <after>} to specify placement within the control file.
	 * @parameter
	 * @since 3.0
	 */
	protected ControlFileLine[] control;

	/**
	 * Other packaging files, such as {@code rules}, if needed.
	 * The file at each pathname will be copied to the {@code DEBIAN/}
	 * directory of the package. The filename will remain the same.
	 * See <a href="https://wiki.debian.org/Packaging/Intro">documentation</a> on adding the Debian packaging files.
	 * for examples of other packaging files.
	 * @parameter
	 * @since 3.0
	 */
	protected File[] packagingFiles;

	/**
	 * Files that declare symbolic links. Each line of each file
	 * should be of the form {@code /path/to/source /path/to/link},
	 * where each path is absolute.
	 *
	 * <p>
	 * This mimics the functionality of the links files in a {@code debbuild}
	 * source directory.
	 *
	 * @parameter
	 * @since 3.3
	 */
	protected File[] linksFiles;

	/**
	 * List of options to pass to the {@code dpkg-deb --build} command.
	 * These options are inserted between {@code dpkg-deb} and {@code --build package_file.deb}.
	 * @parameter
	 * @since 3.1
	 */
	protected String[] dpkgDebBuildOptions;

	/**
	 * List of environment variables pairs that will be provided to the {@code dpkg-deb --build} process.
	 * Example:
	 * <pre>
	 *     &lt;dpkgDebBuildEnvironment&gt;
	 *         &lt;variable&gt;
	 *             &lt;name&gt;FOO&lt;/name&gt;
	 *             &lt;value&gt;bar&lt;/value&gt;
	 *         &lt;/variable&gt;
	 *     &lt;/dpkgDebBuildEnvironment&gt;
	 * </pre>
	 * @parameter
	 * @since 3.1
	 */
	protected NameValuePair[] dpkgDebBuildEnvironment;

	/**
	 * Maven project object.
	 * 
	 * @parameter property="project"
	 */
	@SuppressWarnings("unused") // injected
	private MavenProject project;

	// services
	private final LinkGenerator linkGenerator;

	public PackageMojo(LinkGenerator linkGenerator) {
		this.linkGenerator = requireNonNull(linkGenerator, "linkGenerator");
	}

	public PackageMojo() {
		this(new FilesLinkGenerator());
	}

	private void generateCopyright() throws IOException
	{
		File targetDocDir = new File(stageDir, "usr/share/doc/" + packageName);
		//noinspection ResultOfMethodCallIgnored
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
	}

	private static void addIfValueNotNull(String field, @Nullable String value, Collection<ControlFileLine> lines) {
		requireNonNull(field, "field");
		if (value != null) {
			lines.add(new ControlFileLine(field, value, null));
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
		long installedSizeKb = 1 + FileUtils.sizeOfDirectory(stageDir) / 1024;
		addIfValueNotNull("Installed-Size", String.valueOf(installedSizeKb), lines);
		String value = null;
		if (maintainerName != null && maintainerEmail == null) {
			value = maintainerName;
		} else if (maintainerName == null && maintainerEmail != null) {
			value = String.format("<%s>", maintainerEmail);
		} else //noinspection ConstantConditions
			if (maintainerName != null && maintainerEmail != null) {
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
		try (PrintWriter out = new PrintWriter(new FileWriter(target))) {
			Collection<File> files = FileUtils.listFiles(stageDir, null, true);
			for (File f : files) {
				// check whether the file is a non-regular file
				if (!f.isFile())
					continue;

				// check whether the file is a possible link
				if (!f.getAbsolutePath().equals(f.getCanonicalPath()))
					continue;

				String fname = f.toString().substring(stageDir.toString().length() + 1);
				if (!fname.startsWith("DEBIAN")) {
					FileInputStream fis = new FileInputStream(f);
					String md5 = DigestUtils.md5Hex(fis);
					fis.close();

					out.printf("%s  %s\n", md5, fname);
				}
			}
		}
	}

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
				//noinspection ResultOfMethodCallIgnored
				target.getParentFile().mkdirs();

				String[] cmd = {"groff", "-man", "-Tascii", f.getPath()};
				byte[] processOutput = createProcessRunner().runProcessWithOutput(cmd, NonzeroProcessExitAction.throwMojoExecutionException());

				try (GZIPOutputStream os = new GZIPOutputStream(new FileOutputStream(target))) {
					os.write(processOutput);
				}

				npages++;
			}
		}

		if (npages == 0) {
			getLog().debug("No manual pages found in directory: " + source);
		}
	}

	@SuppressWarnings("CollectionAddAllCanBeReplacedWithConstructor")
	private void generatePackage() throws IOException, MojoExecutionException
	{
		List<String> cmd = new ArrayList<>();
		cmd.addAll(Arrays.asList("fakeroot", "--", "dpkg-deb"));
		if (dpkgDebBuildOptions != null) {
			getLog().info("using dpkg-deb options " + Arrays.toString(dpkgDebBuildOptions));
			cmd.addAll(Arrays.asList(dpkgDebBuildOptions));
		}
		cmd.addAll(Arrays.asList("--build", stageDir.toString(), getPackageFile().toString()));
		runProcess(cmd.toArray(new String[0]), buildDpkgDebBuildEnvironmentMap());
	}

	private Map<String, String> buildDpkgDebBuildEnvironmentMap() throws MojoExecutionException {
		Map<String, String> env = new LinkedHashMap<>();
		Set<String> nameUsed = new TreeSet<>();
		if (dpkgDebBuildEnvironment != null) {
			for (NameValuePair variable : dpkgDebBuildEnvironment) {
				String name = variable.getName();
				if (nameUsed.contains(name)) {
					throw new MojoExecutionException("names of variables in <dpkgDebBuildEnvironment> must be unique; duplicate: " + StringUtils.abbreviate(name, 256));
				}
				env.put(name, variable.getValue());
				nameUsed.add(name);
			}
		}
		return env;
	}

	protected void executeDebMojo() throws MojoExecutionException
	{
		File targetDebDir = new File(stageDir, "DEBIAN");
		try
		{
			FileUtils.deleteDirectory(targetDebDir);
			//noinspection ResultOfMethodCallIgnored
			targetDebDir.mkdirs();
			if (!targetDebDir.isDirectory()) {
				throw new MojoExecutionException("Unable to create directory: " + targetDebDir);
			}
			generateManPages();
			generateCopyright();
			generateConffiles(new File(targetDebDir, "conffiles"));
			generateControl(new File(targetDebDir, "control"));
			generateMd5Sums(new File(targetDebDir, "md5sums"));
			copyOtherPackagingFiles(targetDebDir.toPath());
			linkGenerator.generateLinks(linksFiles, stageDir.toPath());
			generatePackage();
		}
		catch (IOException e)
		{
			getLog().error(e.toString());
			throw new MojoExecutionException(e.toString());
		}
	}

	private void copyOtherPackagingFiles(Path destinationDir) throws IOException, MojoExecutionException {
		File[] files = packagingFiles;
		if (files == null) {
			return;
		}
		files = Arrays.copyOf(files, files.length);
		Set<String> filenameSet = new HashSet<>();
		for (File file : files) {
			String filename = file.getName();
			if (filenameSet.contains(filename)) {
				throw new MojoExecutionException("packaging files must have unique filenames (because they all get copied to the same directory); found duplicate " + StringUtils.abbreviate(filename, 128));
			}
			filenameSet.add(filename);
			Path destinationFile = destinationDir.resolve(filename);
			java.nio.file.Files.copy(file.toPath(), destinationFile, StandardCopyOption.COPY_ATTRIBUTES);
		}
	}

}
