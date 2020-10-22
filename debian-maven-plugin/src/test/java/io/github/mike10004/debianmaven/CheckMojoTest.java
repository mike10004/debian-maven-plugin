package io.github.mike10004.debianmaven;

import org.junit.Test;

import java.io.File;

public class CheckMojoTest {

    @Test
    public void executeDebMojo() throws Exception {
        String resourcePath = "/hello_2.10-1build1_amd64.deb";
        File debFile = new File(getClass().getResource(resourcePath).toURI());
        CheckMojo mojo = new CheckMojo() {
            @Override
            protected File getPackageFile() {
                return debFile;
            }
        };
        mojo.executeDebMojo(); // ok if no exception thrown
    }
}