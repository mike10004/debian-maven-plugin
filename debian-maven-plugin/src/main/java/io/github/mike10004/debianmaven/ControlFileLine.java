package io.github.mike10004.debianmaven;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ControlFileLine {

    private String field;
    private String value;
    private String after;

    public ControlFileLine() {
    }

    public ControlFileLine(String field, String value, String after) {
        this.field = field;
        this.value = value;
        this.after = after;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public static List<ControlFileLine> sorted(List<ControlFileLine> lines) {
        // The integer of the pair records the original ordering of the lines.
        // We don't currently use this value, because we throw an exception
        // if there is an unsatisfied <after> element, but in the future we
        // may want to allow unsatisifed <after> conditions to be ignored, in
        // which case we would want to append the lines in the original order
        // in which they were specified.

        ArrayListValuedHashMap<String, Pair<Integer, ControlFileLine>> afters = new ArrayListValuedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            ControlFileLine line = lines.get(i);
            if (line.getAfter() != null) {
                afters.put(line.getAfter(), Pair.of(i, line));
            }
        }
        List<ControlFileLine> ordered = new ArrayList<>();
        lines.stream().filter(line -> line.getAfter() == null)
                .forEach(line -> {
                    ordered.add(line);
                    List<Pair<Integer, ControlFileLine>> afterThisLine = afters.get(line.getField());
                    afterThisLine.forEach(pair -> {
                        ordered.add(pair.getRight());
                    });
                    afters.remove(line.getField());
                });
        if (!afters.isEmpty()) {
            throw new AfterNotSatisfiedException(afters.keySet());
        }
        return ordered;
    }

    private static class AfterNotSatisfiedException extends IllegalArgumentException {

        public AfterNotSatisfiedException(Collection<String> specifiedAfters) {
            super(String.format("control line <after> conditions were specified (%s) but fields are not present; note that field matching is case-sensitive; do not use <after> element unless you are certain the control file contains that field", specifiedAfters.stream().map(specifiedAfter -> StringUtils.abbreviate(specifiedAfter, 128)).collect(Collectors.joining(", "))));
        }
    }

}
