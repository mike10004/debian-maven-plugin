package io.github.mike10004.debianmaven.tests;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.JerseyDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import io.github.mike10004.containment.dockerjava.DockerClientBuilder;
import org.apache.commons.text.Builder;

class Tests {

    private Tests() {

    }

    public static boolean isSkipDockerTests() {
        String value = System.getProperty("debian-maven-plugin-tests.docker.skip");
        return Boolean.parseBoolean(value);
    }

    public static DefaultDockerClientConfig.Builder dockerClientConfigBuilder() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder();
    }

    public static DockerClientBuilder dockerClientBuilder(DockerClientConfig clientConfig) {
        DockerHttpClient httpClient = new JerseyDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .build();
        return DockerClientBuilder.getInstance(clientConfig).withDockerHttpClient(httpClient);
    }
}
