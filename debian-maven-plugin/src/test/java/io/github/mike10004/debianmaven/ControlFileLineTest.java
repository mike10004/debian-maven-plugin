package io.github.mike10004.debianmaven;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;


public class ControlFileLineTest {

    @Test
    public void sorted() {
        ControlFileLine foo = new ControlFileLine("foo", "bar", null);
        ControlFileLine baz = new ControlFileLine("baz", "gaw", null);
        ControlFileLine haw = new ControlFileLine("haw", "jay", "foo");
        ControlFileLine kay = new ControlFileLine("kay", "jay", null);
        List<ControlFileLine> sorted = ControlFileLine.sorted(Arrays.asList(foo, baz, haw, kay));
        assertEquals("sorted", Arrays.asList("foo", "haw", "baz", "kay"), sorted.stream().map(ControlFileLine::getField).collect(Collectors.toList()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sorted_exceptionIfAfterNotPresent() {
        ControlFileLine foo = new ControlFileLine("foo", "bar", null);
        ControlFileLine baz = new ControlFileLine("baz", "gaw", "pip");
        ControlFileLine haw = new ControlFileLine("haw", "jay", null);
        ControlFileLine.sorted(Arrays.asList(foo, baz, haw));
    }
}