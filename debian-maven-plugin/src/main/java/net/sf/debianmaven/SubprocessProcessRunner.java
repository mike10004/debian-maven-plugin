package net.sf.debianmaven;

import io.github.mike10004.subprocess.ProcessMonitor;
import io.github.mike10004.subprocess.ProcessResult;
import io.github.mike10004.subprocess.ScopedProcessTracker;
import io.github.mike10004.subprocess.StreamContent;
import io.github.mike10004.subprocess.StreamContext;
import io.github.mike10004.subprocess.StreamControl;
import io.github.mike10004.subprocess.Subprocess;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class SubprocessProcessRunner implements ProcessRunner {

    public static final String PARAM_VALUE = "subprocess";

    private final Supplier<Log> logGetter;
    private final Charset processOutputEncoding;
    private final Duration processTimeout;
    private final BiConsumer<Log, String> processStdoutConsumer;
    private final BiConsumer<Log, String> processStderrConsumer;

    public SubprocessProcessRunner(Supplier<Log> logGetter) {
        this(logGetter, Charset.defaultCharset(), getDefaultProcessTimeout());
    }

    public SubprocessProcessRunner(Supplier<Log> logGetter, Charset processOutputEncoding, Duration processTimeout) {
        this(logGetter, processOutputEncoding, processTimeout, defaultProcessLineConsumer(), defaultProcessLineConsumer());
    }

    public SubprocessProcessRunner(Supplier<Log> logGetter, Charset processOutputEncoding, Duration processTimeout, BiConsumer<Log, String> processStdoutConsumer, BiConsumer<Log, String> processStderrConsumer) {
        this.logGetter = requireNonNull(logGetter);
        this.processOutputEncoding = requireNonNull(processOutputEncoding);
        this.processTimeout = requireNonNull(processTimeout);
        this.processStdoutConsumer = requireNonNull(processStdoutConsumer);
        this.processStderrConsumer = requireNonNull(processStderrConsumer);
    }

    public static Duration getDefaultProcessTimeout() {
        return Duration.ofMinutes(15);
    }

    private static class FixedControlStreamContext implements StreamContext<StreamControl, Void, Void> {

        private final StreamControl streamControl;

        private FixedControlStreamContext(StreamControl streamControl) {
            this.streamControl = streamControl;
        }

        @Override
        public StreamControl produceControl() throws IOException {
            return streamControl;
        }

        @Override
        public final StreamContent<Void, Void> transform(int exitCode, StreamControl context) {
            return StreamContent.absent();
        }

    }

    private static class PipePair {
        public final PipedOutputStream output;
        public final PipedInputStream input;
        public PipePair() throws IOException {
            input = new PipedInputStream();
            output = new PipedOutputStream(input);
        }
    }

    private static class TailingStreamControl implements StreamControl {

        private final PipePair stdoutPipe;
        private final PipePair stderrPipe;

        public TailingStreamControl() throws IOException {
            stdoutPipe = new PipePair();
            stderrPipe = new PipePair();
        }

        @Override
        public OutputStream openStdoutSink() throws IOException {
            return stdoutPipe.output;
        }

        @Override
        public OutputStream openStderrSink() {
            return stderrPipe.output;
        }

        @Nullable
        @Override
        public InputStream openStdinSource() {
            return null;
        }

        public InputStream getStdoutPipe() {
            return stdoutPipe.input;
        }

        public InputStream getStderrPipe() {
            return stderrPipe.input;
        }

    }

    private static BiConsumer<Log, String> defaultProcessLineConsumer() {
        return Log::info;
    }

    @Override
    public void runProcess(String[] cmd, NonzeroProcessExitAction nonzeroExitAction) throws IOException, MojoExecutionException {
        if (cmd.length < 1) {
            throw new IllegalArgumentException("command must have at least one element (executable)");
        }
        String executable = cmd[0];
        List<String> executableArgs = Arrays.asList(cmd).subList(1, cmd.length);
        TailingStreamControl tailingStreamControl = new TailingStreamControl();
        FixedControlStreamContext ctx = new FixedControlStreamContext(tailingStreamControl);
        ProcessResult<Void, Void> result;
        try (ScopedProcessTracker processTracker = new ScopedProcessTracker()) {
            ProcessMonitor<Void, Void> monitor = Subprocess.running(executable)
                    .args(executableArgs)
                    .build()
                    .launcher(processTracker)
                    .output(ctx)
                    .launch();
            Thread stdoutThread = new Thread(() -> {
                ProcessStreamDrinker d = new ProcessStreamDrinker(tailingStreamControl::getStdoutPipe, line -> processStdoutConsumer.accept(logGetter.get(), line));
                d.drink();
            });
            Thread stderrThread = new Thread(() -> {
                ProcessStreamDrinker d = new ProcessStreamDrinker(tailingStreamControl::getStderrPipe, line -> processStderrConsumer.accept(logGetter.get(), line));
                d.drink();
            });
            stdoutThread.start();
            stderrThread.start();
            doWithTimeout(stdoutThread::join, false);
            doWithTimeout(stderrThread::join, false);
            result = doWithTimeoutAndReturn(t -> {
                return monitor.await(t, TimeUnit.MILLISECONDS);
            }, true, null);
        }
        int exitCode = result.exitCode();
        if (exitCode != 0) {
            logGetter.get().warn("process exit code: " + exitCode);
            nonzeroExitAction.perform(exitCode, cmd);
        }
    }

    private interface BlockingAction<T> {
        T perform(long millis) throws TimeoutException, InterruptedException;
    }

    private interface BlockingRunnable {
        void perform(long millis) throws TimeoutException, InterruptedException;
    }

    @SuppressWarnings("SameParameterValue")
    private void doWithTimeout(BlockingRunnable action, boolean throwOnPrematureTermination) throws MojoExecutionException {
        //noinspection RedundantCast
        doWithTimeoutAndReturn(new BlockingAction<Void>() {

            @Override
            public Void perform(long millis) throws TimeoutException, InterruptedException {
                action.perform(millis);
                return null;
            }
        }, throwOnPrematureTermination, (Void) null);
    }

    private <T> T doWithTimeoutAndReturn(BlockingAction<T> action, boolean throwOnPrematureTermination, T valueIfNoThrow) throws MojoExecutionException {
        try {
            return action.perform(processTimeout.toMillis());
        } catch (InterruptedException | TimeoutException e) {
            if (throwOnPrematureTermination) {
                throw new MojoExecutionException("process execution timed out or was interrupted", e);
            } else {
                logGetter.get().warn("terminated early from " + action);
                return valueIfNoThrow;
            }
        }
    }

    private interface IOSupplier<T> {
        T openStream() throws IOException;
    }

    private final class ProcessStreamDrinker {

        private final IOSupplier<InputStream> inputProvider;
        private final Consumer<String> lineConsumer;

        private ProcessStreamDrinker(IOSupplier<InputStream> inputProvider, Consumer<String> lineConsumer) {
            this.inputProvider = inputProvider;
            this.lineConsumer = lineConsumer;
        }

        public void drink() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputProvider.openStream(), processOutputEncoding))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineConsumer.accept(line);
                }
            } catch (IOException e) {
                logGetter.get().warn(e);
            }
        }

    }

}
