package io.github.mike10004.debianmaven;

import org.apache.maven.plugin.MojoExecutionException;

import javax.annotation.Nullable;

public interface LinksLineParser {

    /**
     * Parses a link specification from a line of text in a links file.
     * @param line line
     * @return link specification, or null if the line is commented out
     */
    @Nullable
    LinkSpecification parseSpecification(String line) throws InvalidLinkSpecificationException;

    class InvalidLinkSpecificationException extends MojoExecutionException {
        public InvalidLinkSpecificationException(String message) {
            super(message);
        }

        public InvalidLinkSpecificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}

