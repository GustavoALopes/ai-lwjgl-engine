package dev.aether;

import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.scene.Tags;
import dev.aether.scene.Transform;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldMetadataTest {
    @Test void entityMetadataCanBeRenamedAndInspectedInComponentRegistrationOrder() {
        World world = new World();
        Entity entity = world.create("Crate");
        Tags tags = new Tags(new LinkedHashSet<>(List.of("prop", "breakable")));
        world.add(entity, tags);
        world.add(entity, Transform.identity());

        world.rename(entity, "Wooden Crate");

        assertEquals("Wooden Crate", world.nameOf(entity));
        assertEquals(List.of(tags, Transform.identity()), world.componentsOf(entity));
        assertEquals(List.of("prop", "breakable"), List.copyOf(tags.values()));
        assertThrows(UnsupportedOperationException.class, () -> tags.values().add("mutated"));
    }

    @Test void namesAndTagsRejectBlankValues() {
        World world = new World();
        assertThrows(IllegalArgumentException.class, () -> world.create(" "));
        Entity entity = world.create("Valid");
        assertThrows(IllegalArgumentException.class, () -> world.rename(entity, ""));
        assertThrows(IllegalArgumentException.class, () -> new Tags(Set.of("")));
    }
}
