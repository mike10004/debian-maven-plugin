package io.github.mike10004.debianmaven.tests;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
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
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;

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

    public ContainerSubprocessResult<String> testPackageInstallAndExecute(File debFile, String executable, String... args) throws ContainmentException, IOException {
        DockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DjManualContainerMonitor containerMonitor = new DjManualContainerMonitor();
        DjDockerManager dockerManager = new DefaultDjDockerManager(clientConfig, containerMonitor);
        ContainerParametry parametry = ContainerParametry.builder(dockerImageName)
                .commandToWaitIndefinitely()
                .build();
        try (ContainerCreator containerCreator = new DjContainerCreator(dockerManager);
             StartableContainer startable = containerCreator.create(parametry);
             StartedContainer container = startable.start()) {
            installDebFile(container, debFile);
            return execute(container, executable, args);
        } finally {
            try (DockerClient client = DockerClientBuilder.getInstance(clientConfig).build()) {
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

    private void installDebFile(StartedContainer container, File debFile) throws ContainmentException, IOException {
        container.copier().copyToContainer(debFile, "/tmp/");
        String pathInContainer = Path.of("/tmp").resolve(debFile.getName()).toString();
        execute(container, "apt", "install", "--yes", pathInContainer);
    }

    private ContainerSubprocessResult<String> execute(StartedContainer container, String executable, String... args) throws ContainmentException {
        return container.executor().execute(executable, args);
    }
}
