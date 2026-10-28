package com.github.cerealklla.caryvanimager.render;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Projects a list of {@link VisibleFace}s through an {@link OrthographicCamera} and rasterizes them
 * into a fixed-size, Z-buffered {@link BufferedImage} (transparent background) -- same orthographic-
 * quad-as-affine-transform technique BluepryntImager's own {@code Rasterizer} uses (see its own doc),
 * simplified here to shade each face with a flat color (brightness-multiplied) instead of sampling a
 * UV-mapped texture, since this program's geometry has no meaningful per-pixel texture layout (see
 * {@code VisibleFace}'s own doc for why).
 */
public final class Rasterizer {

    private static final double MARGIN_FACTOR = 0.88;

    private final int width;
    private final int height;

    public Rasterizer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public BufferedImage render(List<VisibleFace> faces) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        double[] depthBuffer = new double[width * height];
        java.util.Arrays.fill(depthBuffer, Double.MAX_VALUE);

        if (faces.isEmpty()) {
            return image;
        }

        OrthographicCamera camera = defaultCamera();
        Transform2D transform = Transform2D.fit(faces, camera, width, height, MARGIN_FACTOR);

        for (VisibleFace face : faces) {
            rasterizeFace(image, depthBuffer, face, camera, transform);
        }
        return image;
    }

    /** A fixed, front-biased isometric-ish angle -- same default BluepryntImager uses, good enough for a small crafting-preview icon with no per-structure tuning needed. */
    public static OrthographicCamera defaultCamera() {
        return OrthographicCamera.of(35.0, 25.0, new Vec3(0, 0, -1));
    }

    private void rasterizeFace(BufferedImage image, double[] depthBuffer, VisibleFace face,
                                OrthographicCamera camera, Transform2D transform) {
        double[] p00 = transform.toPixel(camera, face.corner00());
        double[] p10 = transform.toPixel(camera, face.corner10());
        double[] p01 = transform.toPixel(camera, face.corner01());

        double d00 = camera.depth(face.corner00());
        double d10 = camera.depth(face.corner10());
        double d01 = camera.depth(face.corner01());

        double ax = p10[0] - p00[0], ay = p10[1] - p00[1];
        double bx = p01[0] - p00[0], by = p01[1] - p00[1];
        double det = ax * by - bx * ay;
        if (Math.abs(det) < 1e-9) {
            return;
        }
        double invDet = 1.0 / det;

        int minX = (int) Math.floor(Math.min(p00[0], Math.min(p10[0], Math.min(p01[0], p00[0] + ax + bx))));
        int maxX = (int) Math.ceil(Math.max(p00[0], Math.max(p10[0], Math.max(p01[0], p00[0] + ax + bx))));
        int minY = (int) Math.floor(Math.min(p00[1], Math.min(p10[1], Math.min(p01[1], p00[1] + ay + by))));
        int maxY = (int) Math.ceil(Math.max(p00[1], Math.max(p10[1], Math.max(p01[1], p00[1] + ay + by))));
        minX = Math.max(minX, 0);
        minY = Math.max(minY, 0);
        maxX = Math.min(maxX, width - 1);
        maxY = Math.min(maxY, height - 1);

        int shaded = shade(face.rgb(), face.brightness());

        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                double dx = px + 0.5 - p00[0];
                double dy = py + 0.5 - p00[1];
                double u = (dx * by - bx * dy) * invDet;
                double v = (ax * dy - dx * ay) * invDet;
                if (u < 0 || u > 1 || v < 0 || v > 1) {
                    continue;
                }
                double depth = d00 + u * (d10 - d00) + v * (d01 - d00);
                int index = py * width + px;
                if (depth >= depthBuffer[index]) {
                    continue;
                }
                depthBuffer[index] = depth;
                image.setRGB(px, py, shaded);
            }
        }
    }

    private static int shade(int rgb, double brightness) {
        int r = clampChannel((int) (((rgb >> 16) & 0xFF) * brightness));
        int g = clampChannel((int) (((rgb >> 8) & 0xFF) * brightness));
        int b = clampChannel((int) ((rgb & 0xFF) * brightness));
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
