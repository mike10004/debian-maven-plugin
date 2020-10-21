package net.sf.debianmaven;

import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        afters.values().stream()
                .sorted(Comparator.comparing(Pair::getLeft))
                .map(Pair::getRight)
                .forEach(ordered::add);
        return ordered;
    }
}
