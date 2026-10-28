package com.github.cerealklla.caryvanimager.render;

/**
 * One of a box's six faces. {@code corners(box)} gives the face's origin corner and its two edge
 * directions (u-axis, v-axis) -- exactly what an orthographic projection of a planar quad needs (no
 * perspective correction). {@code brightness} mirrors vanilla Minecraft's own classic flat per-face
 * block-light multipliers, same values BluepryntImager's own {@code Face} enum uses.
 *
 * <p>Simplified from BluepryntImager's own version: that one works in cell-local fractions (a block
 * grid, where a shape box is always a sub-region of a unit cell); this program's boxes are already
 * absolute world-space coordinates (hand-authored cart geometry, not a voxel grid), so there's no
 * cell-origin indirection to carry.
 */
public enum Face {
    UP(0, 1, 0, 1.0),
    DOWN(0, -1, 0, 0.5),
    NORTH(0, 0, -1, 0.8),
    SOUTH(0, 0, 1, 0.8),
    WEST(-1, 0, 0, 0.6),
    EAST(1, 0, 0, 0.6);

    public final int nx;
    public final int ny;
    public final int nz;
    public final double brightness;

    Face(int nx, int ny, int nz, double brightness) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.brightness = brightness;
    }

    /** @return {corner00, corner10, corner01} for this face of {@code box}. */
    public Vec3[] corners(Box box) {
        return switch (this) {
            case UP -> new Vec3[]{v(box.minX(), box.maxY(), box.minZ()), v(box.maxX(), box.maxY(), box.minZ()), v(box.minX(), box.maxY(), box.maxZ())};
            case DOWN -> new Vec3[]{v(box.minX(), box.minY(), box.minZ()), v(box.maxX(), box.minY(), box.minZ()), v(box.minX(), box.minY(), box.maxZ())};
            case NORTH -> new Vec3[]{v(box.minX(), box.minY(), box.minZ()), v(box.maxX(), box.minY(), box.minZ()), v(box.minX(), box.maxY(), box.minZ())};
            case SOUTH -> new Vec3[]{v(box.minX(), box.minY(), box.maxZ()), v(box.maxX(), box.minY(), box.maxZ()), v(box.minX(), box.maxY(), box.maxZ())};
            case WEST -> new Vec3[]{v(box.minX(), box.minY(), box.minZ()), v(box.minX(), box.minY(), box.maxZ()), v(box.minX(), box.maxY(), box.minZ())};
            case EAST -> new Vec3[]{v(box.maxX(), box.minY(), box.minZ()), v(box.maxX(), box.minY(), box.maxZ()), v(box.maxX(), box.maxY(), box.minZ())};
        };
    }

    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }
}
