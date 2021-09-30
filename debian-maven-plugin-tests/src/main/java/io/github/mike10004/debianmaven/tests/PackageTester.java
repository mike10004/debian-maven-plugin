package io.github.mike10004.debianmaven.tests;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientConfig;
import io.github.mike10004.containment.ContainerCreator;
import io.github.mike10004.containment.ContainerParametry;
import io.github.mike10004.containment.ContainerSubprocessResult;
import io.github.mike10004.containment.ContainmentException;
import io.github.mike10004.containment.StartableContainer;
import io.github.mike10004.containment.StartedContainer;
import io.github.mike10004.containment.dockerjava.DefaultDjDockerManager;
import io.github.mike10004.containment.dockerjava.DjContainerCreator;
import io.github.mike10004.containment.dockerjava.DjDockerManager;
import io.github.mike10004.containment.dockerjava.DjManualContainerMonitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Service class that facilitates testing of a debian package file.
 */
public class PackageTester {

    public static final String UBUNTU_JAVA_IMAGE_NAME = "debian-maven-plugin-tests-ubuntujava";

    private final String dockerImageName;

    public PackageTester(String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }

    public static PackageTester onUbuntuJavaImage() {
        return new PackageTester(UBUNTU_JAVA_IMAGE_NAME);
    }

    public static class InstallResult<T> {

        public final ContainerSubprocessResult<String> dpkgResult;
        public final T additionalResult;

        public InstallResult(ContainerSubprocessResult<String> dpkgResult, T additionalResult) {
            this.dpkgResult = dpkgResult;
            this.additionalResult = additionalResult;
        }
    }

    public interface ContainerClient<T> {

        T useContainer(StartedContainer container) throws ContainmentException, IOException;

    }

    public <T> InstallResult<T> testPackageInstallAndExecute(File debFile, ContainerClient<T> containerClient) throws ContainmentException, IOException {
        DockerClientConfig clientConfig = Tests.dockerClientConfigBuilder().build();
        DjManualContainerMonitor containerMonitor = new DjManualContainerMonitor();
        DjDockerManager dockerManager = new DefaultDjDockerManager(clientConfig, containerMonitor);
        ContainerParametry parametry = ContainerParametry.builder(dockerImageName)
                .commandToWaitIndefinitely()
                .build();
        try (ContainerCreator containerCreator = new DjContainerCreator(dockerManager);
             StartableContainer startable = containerCreator.create(parametry);
             StartedContainer container = startable.start()) {
            ContainerSubprocessResult<String> dpkgResult = installDebFile(container, debFile);
            T clientResult = containerClient.useContainer(container);
            return new InstallResult<>(dpkgResult, clientResult);
        } finally {
            try (DockerClient client = Tests.dockerClientBuilder(clientConfig).build()) {
                containerMonitor.stopAll(client, new DjManualContainerMonitor.ContainerActionErrorListener() {
                    @Override
                    public void accept(String s, Exception e) {
                        System.err.println(s);
                        System.err.println(e.toString());
                    }
                });
            }
        }
    }

    private ContainerSubprocessResult<String> installDebFile(StartedContainer container, File debFile) throws ContainmentException, IOException {
        container.copier().copyToContainer(debFile, "/tmp/");
        String pathInContainer = Path.of("/tmp").resolve(debFile.getName()).toString();
        ContainerSubprocessResult<String> result = ExecutingContainerClient.from("apt", "install", "--yes", pathInContainer).useContainer(container);
        if (result.exitCode() != 0) {
            throw new NonzeroExitException(result);
        }
        return result;
    }

    public static class NonzeroExitException extends RuntimeException {
        public final ContainerSubprocessResult<?> result;

        public NonzeroExitException(ContainerSubprocessResult<?> result) {
            this.result = result;
        }
    }

    private static class ExecutingContainerClient implements ContainerClient<ContainerSubprocessResult<String>> {

        private final String executable;
        private final String[] args;

        public ExecutingContainerClient(String executable, String[] args) {
            this.executable = executable;
            this.args = args;
        }

        public static ExecutingContainerClient from(String executable, String...args) {
            return new ExecutingContainerClient(executable, args);
        }

        @Override
        public ContainerSubprocessResult<String> useContainer(StartedContainer container) throws ContainmentException, IOException {
            return container.executor().execute(executable, args);
        }
    }

}
