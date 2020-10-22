package io.github.mike10004.debutils;

import org.junit.Test;

import static org.junit.Assert.*;

public class DebInfoTest {

    @Test
    public void getValue() {
        String text = " new Debian package, version 2.0.\n" +
                " size 267648 bytes: control archive=9012 bytes.\n" +
                "     370 bytes,    12 lines      control              \n" +
                "   40147 bytes,   303 lines      md5sums              \n" +
                " Package: example-multimodule-deb\n" +
                " Version: 3.0+202010221611\n" +
                " Section: contrib/utils\n" +
                " Priority: optional\n" +
                " Architecture: all\n" +
                " Depends: default-jre-headless\n" +
                " Build-Depends: build-essential, some, other,\n" +
                "  dependency, packages\n" +
                " Installed-Size: Installed-Size: 1069\n" +
                " Maintainer: Jane Doe <jane@doe.com>\n" +
                " Homepage: https://example.com/\n" +
                " Description: Example Multimodule Project\n" +
                "  This is an example of a multimodule project\n";
        DebInfo info = new DebInfo(text);
        assertEquals("Package", "example-multimodule-deb", info.getValue("Package"));
        assertEquals("Homepage", "https://example.com/", info.getValue("Homepage"));
        assertEquals("Build-Depends", "build-essential, some, other, dependency, packages", info.getValue("Build-Depends"));
        assertEquals("Description", "Example Multimodule Project This is an example of a multimodule project", info.getValue("Description"));
    }
}