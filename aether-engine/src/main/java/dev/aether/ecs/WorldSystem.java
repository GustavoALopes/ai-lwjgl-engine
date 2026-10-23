package dev.aether.ecs;

@FunctionalInterface
public interface WorldSystem {
    void update(World world, double deltaSeconds);
}
