package dev.aether.math;
public record Vec3(double x, double y, double z) { public static final Vec3 ZERO = new Vec3(0, 0, 0); public Vec3 add(Vec3 o) { return new Vec3(x+o.x,y+o.y,z+o.z); } public Vec3 multiply(double n) { return new Vec3(x*n,y*n,z*n); } }
