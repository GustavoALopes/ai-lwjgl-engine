package dev.aether.physics;
import dev.aether.ecs.Component; import dev.aether.math.Vec3; import java.util.Objects;
public record RigidBody(double mass,Vec3 velocity,boolean useGravity) implements Component { public RigidBody{if(!Double.isFinite(mass)||mass<=0)throw new IllegalArgumentException("mass must be positive");Objects.requireNonNull(velocity);} public RigidBody withVelocity(Vec3 v){return new RigidBody(mass,v,useGravity);} }
