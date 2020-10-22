package com.example.multimodule.library;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Objects;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class Message {

    private final String content;

    public Message(String content) {
        this.content = requireNonNull(content);
    }

    public String quoteContent() {
        return String.format("\"%s\"", StringEscapeUtils.escapeJava(content));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Message.class.getSimpleName() + "[", "]")
                .add("content=" + (content == null ? "null" : "'" + content + "'"))
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(content, message.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
}
