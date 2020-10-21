package io.github.mike10004.debianmaven.tests;

import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.Subprocess;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SubprocessMavenRunner implements MavenRunner {
    @Override
    public MavenRun execute(Path projectDirectory, Iterable<String> goals_) throws IOException {
        List<String> goals = StreamSupport.stream(goals_.spliterator(), false).collect(Collectors.toList());
        ProcessResult<String, String> result;
        try (ScopedProcessTracker tracker = new ScopedProcessTracker()) {
            result = Subprocess.running("mvn")
                    .from(projectDirectory.toFile())
                    .args(goals)
                    .build()
                    .launcher(tracker)
                    .outputStrings(Charset.defaultCharset())
                    .launch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted while waiting for process to terminate", e);
        }
        if (result.exitCode() != 0) {
            dumpLinesWithPrefix(result.content().stdout().lines(), "STDOUT: ", System.out);
            dumpLinesWithPrefix(result.content().stderr().lines(), "STDERR: ", System.err);
            throw new NonzeroMavenExitException(result.exitCode());
        }
        return new ResultMavenRun(projectDirectory, goals, result);
    }

    private static void dumpLinesWithPrefix(Stream<String> lines, String prefix, PrintStream out) {
        lines.forEach(line -> {
            out.print(prefix);
            out.println(line);
        });
    }

    private static class NonzeroMavenExitException extends RuntimeException {

        public NonzeroMavenExitException(int exitCode) {
            super(String.format("exit code %d", exitCode));
        }

    }

    private static class ResultMavenRun implements MavenRun {

        private final Path projectDirectory;
        private final List<String> goals;
        private final ProcessResult<String, String> result;

        public ResultMavenRun(Path projectDirectory, List<String> goals, ProcessResult<String, String> result) {
            this.projectDirectory = projectDirectory;
            this.goals = goals;
            this.result = result;
        }

        @Override
        public Path projectDirectory() {
            return projectDirectory;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", ResultMavenRun.class.getSimpleName() + "[", "]")
                    .add("projectDirectory=" + projectDirectory)
                    .add("goals=" + goals)
                    .add("result=" + result)
                    .toString();
        }
    }
}
