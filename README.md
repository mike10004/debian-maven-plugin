[![Maven Central](https://img.shields.io/maven-central/v/com.github.mike10004/debian-maven-plugin.svg)](https://repo1.maven.org/maven2/com/github/mike10004/debian-maven-plugin/)

# debian-maven-plugin

Maven plugin wrapper around Debian utilities for building `.deb` packages.

## Quick Start

Use Maven plugins to copy resources and dependencies into 
`${project.build.directory}/deb`. Everything in that directory will be 
included in the package. For example, you might include a directory structure 
like this:

* `deb`
    + `usr`
        + `bin`
            + `hello-world.sh`
        + `share`
            + `hello-world`
                + `data.txt`
    + `etc`
        + `hello-world`
            + `config.ini`

Then add a build plugin execution as follows:

    <plugin>
        <groupId>com.github.mike10004</groupId>
        <artifactId>debian-maven-plugin</artifactId>
        <version>3.1</version>
        <executions>
            <execution>
                <id>build-deb</id>
                <phase>package</phase>
                <goals>
                    <goal>package</goal>
                </goals>
                <configuration>
                    <packageName>hello-world</packageName>
                    <packageTitle>Hello World</packageTitle>
                    <packageRevision>1</packageRevision>
                    <projectUrl>https://example.com/</projectUrl>
                    <projectOrganization>Unorganized Developers</projectOrganization>
                    <maintainerName>Jane Doe</maintainerName>
                    <maintainerEmail>jane@doe.com</maintainerEmail>
                    <packageDependencies>
                        <package>default-jre-headless</package>
                    </packageDependencies>
                </configuration>
            </execution>
        </executions>
    </plugin>

## More examples

See the projects under the `debian-maven-plugin-examples` directory.

## Changelog

### 3.1

* remove support for automatic detection of included artifacts
* support arbitrary packaging files (e.g. `postinst`)
* support custom `dpkg-deb --build` arguments and environment variables

### 1.1.0

* avoid printing messages from executed processes at the *warn* level in build
  log; use `<processExecutionMode>legacy</processExecutionMode>` if you liked
  the old behavior

### 1.0.9 and lower

* Code forked from https://sourceforge.net/projects/debian-maven/ by
  [wowtor](https://sourceforge.net/u/wowtor/profile/)
