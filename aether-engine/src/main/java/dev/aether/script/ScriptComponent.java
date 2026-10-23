package dev.aether.script;

import dev.aether.ecs.Component;
import java.util.Objects;

public record ScriptComponent(Script script) implements Component {
    public ScriptComponent {
        Objects.requireNonNull(script);
    }
}
