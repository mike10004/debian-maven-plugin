# debian-maven-plugin

Maven plugin wrapper around Debian utilities for building `.deb` packages.

## Quick Start

If your project artifact is the one you want to be packaged:

    <plugin>
        <groupId>com.github.mike10004</groupId>
        <artifactId>debian-maven-plugin</artifactId>
        <version>1.1.0</version>
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
                    <excludeAllDependencies>true</excludeAllDependencies>
                    <packageDependencies>
                        <package>default-jre-headless</package>
                    </packageDependencies>
                </configuration>
            </execution>
        </executions>
    </plugin>

Otherwise, use other Maven plugins for copying resources and dependencies to
stage everything in `${project.build.directory}/deb`, and everything in that
directory will be included in the package. For example, you might stage a
directory structure like this:

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

## Changelog

### 1.1.0

* avoid printing messages from executed processes at the *warn* level in build
  log; use `<processExecutionMode>legacy</processExecutionMode>` if you liked
  the old behavior

### 1.0.9 and lower

* Code forked from https://sourceforge.net/projects/debian-maven/ by
  [wowtor](https://sourceforge.net/u/wowtor/profile/)
