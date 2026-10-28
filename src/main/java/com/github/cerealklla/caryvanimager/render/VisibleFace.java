package com.github.cerealklla.caryvanimager.render;

/**
 * One cube face ready to project + rasterize. Carries a flat {@code rgb} color (no alpha, opaque)
 * instead of BluepryntImager's {@code textureId} -- this program colors each cart part from an
 * average swatch color sampled off the species' own real texture file (see {@code
 * color.SpeciesSwatch}), not a UV-mapped texture, since the simplified stand-in geometry (see
 * {@code model.CartGeometry}) has no meaningful per-pixel UV layout to sample against.
 */
public record VisibleFace(Vec3 corner00, Vec3 corner10, Vec3 corner01, double brightness, int rgb) {
}
