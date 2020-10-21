package io.github.mike10004.debianmaven.tests;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface MavenRunner {

    default MavenRun execute(Path projectDirectory, String goal, String...moreGoals) throws IOException {
        return execute(projectDirectory, Stream.concat(Stream.of(goal), Arrays.stream(moreGoals)).collect(Collectors.toList()));
    }

    MavenRun execute(Path projectDirectory, Iterable<String> goals) throws IOException;

    static MavenRunner create() {
        return new SubprocessMavenRunner();
    }
}
