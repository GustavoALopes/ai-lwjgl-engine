package dev.aether.ecs;

@FunctionalInterface
public interface WorldSystem {
    void update(World world, double deltaSeconds);

    /** Releases system-owned state when its world closes. */
    default void close(World world) { }
}
