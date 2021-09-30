package io.github.mike10004.debianmaven;

import org.apache.maven.plugin.MojoExecutionException;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface LinkGenerator {
    void generateLinks(@Nullable File[] linksFiles, Path stageDir) throws IOException, MojoExecutionException;
}
