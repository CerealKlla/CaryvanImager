package com.github.cerealklla.caryvanimager.render;

import java.util.List;

/** Fits the projected (unscaled, camera-space) bounding box of a scene into a target pixel canvas, centered with a margin. Verbatim port of BluepryntImager's own class -- pure geometry, no texture dependency either way. */
public final class Transform2D {

    private final double scale;
    private final double worldCenterX;
    private final double worldCenterY;
    private final double imageCenterX;
    private final double imageCenterY;

    private Transform2D(double scale, double worldCenterX, double worldCenterY, double imageCenterX, double imageCenterY) {
        this.scale = scale;
        this.worldCenterX = worldCenterX;
        this.worldCenterY = worldCenterY;
        this.imageCenterX = imageCenterX;
        this.imageCenterY = imageCenterY;
    }

    public static Transform2D fit(List<VisibleFace> faces, OrthographicCamera camera, int width, int height, double marginFactor) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (VisibleFace face : faces) {
            Vec3 corner11 = face.corner10().add(face.corner01()).add(face.corner00().scale(-1));
            for (Vec3 corner : new Vec3[]{face.corner00(), face.corner10(), face.corner01(), corner11}) {
                double sx = camera.screenX(corner);
                double sy = camera.screenY(corner);
                minX = Math.min(minX, sx);
                maxX = Math.max(maxX, sx);
                minY = Math.min(minY, sy);
                maxY = Math.max(maxY, sy);
            }
        }

        double boundsWidth = Math.max(1e-6, maxX - minX);
        double boundsHeight = Math.max(1e-6, maxY - minY);
        double scale = marginFactor * Math.min(width / boundsWidth, height / boundsHeight);

        return new Transform2D(scale, (minX + maxX) / 2.0, (minY + maxY) / 2.0, width / 2.0, height / 2.0);
    }

    public double[] toPixel(OrthographicCamera camera, Vec3 point) {
        double sx = camera.screenX(point);
        double sy = camera.screenY(point);
        return new double[]{(sx - worldCenterX) * scale + imageCenterX, (sy - worldCenterY) * scale + imageCenterY};
    }
}
