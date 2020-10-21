package net.sf.debianmaven;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

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