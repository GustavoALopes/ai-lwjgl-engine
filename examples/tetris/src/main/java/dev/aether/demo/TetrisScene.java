package dev.aether.demo;

import dev.aether.ecs.Entity;
import dev.aether.ecs.World;
import dev.aether.ecs.WorldSystem;
import dev.aether.math.Vec3;
import dev.aether.render.MeshRenderer;
import dev.aether.render.Material;
import dev.aether.render.PostProcessPulse;
import dev.aether.scene.Transform;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** ECS presentation adapter for {@link TetrisDemo}; it contains no OpenGL dependency. */
public final class TetrisScene implements WorldSystem {
    private final TetrisDemo game;
    private final List<Entity> cells = new ArrayList<>(TetrisDemo.WIDTH * TetrisDemo.HEIGHT);
    private final List<String> materials = new ArrayList<>(TetrisDemo.WIDTH * TetrisDemo.HEIGHT);
    private final List<Particle> particles = new ArrayList<>();
    private boolean installed;
    private Entity effectEntity;
    private double previousLockPulse;
    private double previousClearPulse;

    public TetrisScene(TetrisDemo game) { this.game = Objects.requireNonNull(game, "game"); }
    public TetrisDemo game() { return game; }

    /** Clears the current world entities and constructs a fresh visible Tetris board. */
    public void open(World world) {
        Objects.requireNonNull(world, "world");
        world.entities().forEach(world::destroy);
        game.reset();
        cells.clear();
        materials.clear();
        particles.clear();
        previousLockPulse = 0;
        previousClearPulse = 0;
        effectEntity = world.create("Tetris / Post Process");
        world.add(effectEntity, new PostProcessPulse(0));
        for (int row = 0; row < TetrisDemo.HEIGHT; row++) for (int column = 0; column < TetrisDemo.WIDTH; column++) {
            Entity cell = world.create("Tetris / Cell " + column + "," + row);
            // Keep the whole 20-row board within the editor's default orthographic framing.
            world.add(cell, new Transform(new Vec3((column - 4.5) * .78, (9.5 - row) * .78, 0), Vec3.ZERO, new Vec3(.36, .36, .22)));
            world.add(cell, new MeshRenderer("primitive/cube", "tetris/empty"));
            world.add(cell, new Material(colorFor("tetris/empty"), 0));
            cells.add(cell);
            materials.add("tetris/empty");
        }
        if (!installed) {
            world.addSystem(game);
            world.addSystem(this);
            installed = true;
        }
        sync(world, 0);
    }

    @Override public void update(World world, double deltaSeconds) { sync(world, deltaSeconds); }

    private void sync(World world, double deltaSeconds) {
        if (cells.isEmpty()) return;
        TetrisDemo.Snapshot snapshot = game.snapshot();
        if (effectEntity != null && world.exists(effectEntity)) world.add(effectEntity, new PostProcessPulse(snapshot.clearPulse()));
        if (snapshot.lockPulse() > .95 && previousLockPulse <= .95) emit(world, 16, false);
        if (snapshot.clearPulse() > .05 && previousClearPulse <= .05) emit(world, 110, true);
        previousLockPulse = snapshot.lockPulse();
        previousClearPulse = snapshot.clearPulse();
        updateParticles(world, deltaSeconds);
        int[][] displayed = snapshot.lockedCells();
        for (TetrisDemo.Cell cell : snapshot.activeCells()) if (cell.y() >= 0 && cell.y() < TetrisDemo.HEIGHT) displayed[cell.y()][cell.x()] = cell.type().ordinal() + 1;
        for (int row = 0; row < TetrisDemo.HEIGHT; row++) for (int column = 0; column < TetrisDemo.WIDTH; column++) {
            int index = row * TetrisDemo.WIDTH + column;
            String material = materialFor(displayed[row][column]);
            if (!material.equals(materials.get(index)) && world.exists(cells.get(index))) {
                world.add(cells.get(index), new MeshRenderer("primitive/cube", material));
                world.add(cells.get(index), new Material(colorFor(material), 0));
                materials.set(index, material);
            }
        }
    }
    private static String materialFor(int value) {
        return switch (value) {
            case 1 -> "tetris/cyan"; case 2 -> "tetris/yellow"; case 3 -> "tetris/purple";
            case 4 -> "tetris/green"; case 5 -> "tetris/red"; case 6 -> "tetris/blue";
            case 7 -> "tetris/orange"; default -> "tetris/empty";
        };
    }
    private static Vec3 colorFor(String material) {
        return switch (material) {
            case "tetris/blue" -> new Vec3(.08, .35, .95);
            case "tetris/orange" -> new Vec3(1, .25, .04);
            case "tetris/cyan" -> new Vec3(.05, .85, .9);
            case "tetris/yellow" -> new Vec3(.95, .8, .05);
            case "tetris/purple" -> new Vec3(.65, .2, .9);
            case "tetris/green" -> new Vec3(.15, .8, .3);
            case "tetris/red" -> new Vec3(.95, .12, .16);
            default -> new Vec3(.06, .08, .12);
        };
    }
    private void emit(World world, int count, boolean explosion) {
        for (int index = 0; index < count; index++) {
            double angle = index * 2.3999632297;
            double speed = (explosion ? .9 : .35) + (index % 7) * .08;
            Entity entity = world.create(explosion ? "Tetris / Line Burst" : "Tetris / Lock Spark");
            world.add(entity, new Transform(new Vec3(0, explosion ? 0 : -6.2, .3), Vec3.ZERO, new Vec3(.09, .09, .09)));
            world.add(entity, new MeshRenderer("primitive/cube", "tetris/particle"));
            world.add(entity, new Material(new Vec3(.3, .9, 1), 0));
            particles.add(new Particle(entity, Math.cos(angle) * speed, Math.sin(angle) * speed, explosion ? .8 : .32));
        }
    }
    private void updateParticles(World world, double deltaSeconds) {
        for (int index = particles.size() - 1; index >= 0; index--) {
            Particle particle = particles.get(index);
            particle.life -= deltaSeconds;
            if (particle.life <= 0) { world.destroy(particle.entity); particles.remove(index); continue; }
            if (world.exists(particle.entity)) world.get(particle.entity, Transform.class).ifPresent(transform -> world.add(particle.entity, transform.withPosition(transform.position().add(new Vec3(particle.vx * deltaSeconds, particle.vy * deltaSeconds, 0))).withScale(new Vec3(particle.life * .13, particle.life * .13, particle.life * .13))));
        }
    }
    private static final class Particle { private final Entity entity; private final double vx, vy; private double life; private Particle(Entity entity,double vx,double vy,double life){this.entity=entity;this.vx=vx;this.vy=vy;this.life=life;} }
}
