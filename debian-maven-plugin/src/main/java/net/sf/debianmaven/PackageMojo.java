package net.sf.debianmaven;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
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

		if (npages == 0)
			getLog().info("No manual pages found in directory: "+source);
	}

	private void generatePackage() throws IOException, MojoExecutionException
	{
		runProcess(new String[]{"fakeroot", "--", "dpkg-deb", "--build", stageDir.toString(), getPackageFile().toString()});
	}

	protected void executeDebMojo() throws MojoExecutionException
	{
		File targetDebDir = new File(stageDir, "DEBIAN");
		if (!targetDebDir.exists() && !targetDebDir.mkdirs())
			throw new MojoExecutionException("Unable to create directory: "+targetDebDir);

		try
		{
			generateManPages();
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
