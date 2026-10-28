package com.github.cerealklla.caryvanimager.render;

/** Minimal immutable 3D double vector -- just enough math for the orthographic projection below. Verbatim copy of BluepryntImager's own class, same reasoning. */
public record Vec3(double x, double y, double z) {
    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 scale(double s) {
        return new Vec3(x * s, y * s, z * s);
    }

    public double dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    public double length() {
        return Math.sqrt(dot(this));
    }

    public Vec3 normalize() {
        double len = length();
        return len == 0 ? this : scale(1.0 / len);
    }
}
