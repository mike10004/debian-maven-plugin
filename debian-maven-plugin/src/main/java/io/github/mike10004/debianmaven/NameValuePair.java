package io.github.mike10004.debianmaven;

import java.util.Objects;
import java.util.StringJoiner;

public class NameValuePair {
    private String name;
    private String value;

    public NameValuePair() {
    }

    public NameValuePair(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NameValuePair)) return false;
        NameValuePair that = (NameValuePair) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", NameValuePair.class.getSimpleName() + "(", ")")
                .add((name == null ? "null" : "'" + name + "'"))
                .add((value == null ? "null" : "'" + value + "'"))
                .toString();
    }
}
