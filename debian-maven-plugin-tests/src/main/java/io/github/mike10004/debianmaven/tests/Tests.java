package io.github.mike10004.debianmaven.tests;

class Tests {

    private Tests() {

    }

    public static boolean isSkipDockerTests() {
        String value = System.getProperty("debian-maven-plugin-tests.docker.skip");
        return Boolean.parseBoolean(value);
    }
}
