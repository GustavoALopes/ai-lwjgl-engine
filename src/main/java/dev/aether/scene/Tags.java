package dev.aether.scene;

import dev.aether.ecs.Component;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Immutable, insertion-ordered labels used to organize entities in editor tooling. */
public record Tags(Set<String> values) implements Component {
    public Tags {
        Objects.requireNonNull(values, "values");
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Tags must not contain blank values");
            }
            copy.add(value);
        }
        values = Collections.unmodifiableSet(copy);
    }

    public static Tags none() { return new Tags(Set.of()); }
    public Tags withAdded(String value) { return new Tags(append(value)); }
    public Tags withRemoved(String value) { LinkedHashSet<String> copy = new LinkedHashSet<>(values); copy.remove(value); return new Tags(copy); }

    private Set<String> append(String value) { LinkedHashSet<String> copy = new LinkedHashSet<>(values); copy.add(value); return copy; }
}
