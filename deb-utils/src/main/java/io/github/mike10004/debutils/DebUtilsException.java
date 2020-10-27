package io.github.mike10004.debutils;

public class DebUtilsException extends Exception {
    public DebUtilsException(String message) {
        super(message);
    }

    public DebUtilsException(String message, Throwable cause) {
        super(message, cause);
    }

    public DebUtilsException(Throwable cause) {
        super(cause);
    }
}
