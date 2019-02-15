package net.sf.debianmaven;

import static org.junit.Assert.*;

public class PackageMojoTest {

    @org.junit.Test
    public void getPackageFile() {
        PackageMojo mojo = new PackageMojo() {
            @Override
            protected String getPackageVersion() {
                return "3.4.5";
            }
        };
        mojo.packageName = "foo";
        mojo.packageRevision = "9";
        mojo.packageArchitecture = "amd64";
        String packageFilename = mojo.getPackageFile().getName();
        assertEquals("package filename", "foo_3.4.5-9_amd64", packageFilename);
    }
}