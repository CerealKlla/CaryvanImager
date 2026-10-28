package com.github.cerealklla.caryvanimager.render;

/**
 * A fixed orthographic (parallel-projection) camera -- verbatim copy of BluepryntImager's own class
 * (see its own doc for the full math reasoning), reused as-is since the projection math doesn't
 * depend on what kind of geometry (block cells vs. entity cuboids) is being viewed.
 */
public final class OrthographicCamera {
    public final Vec3 forward;
    public final Vec3 right;
    public final Vec3 up;

    private OrthographicCamera(Vec3 forward, Vec3 right, Vec3 up) {
        this.forward = forward;
        this.right = right;
        this.up = up;
    }

    public static OrthographicCamera of(double azimuthDeg, double elevationDeg, Vec3 frontViewDirection) {
        double az = Math.toRadians(azimuthDeg);
        double el = Math.toRadians(elevationDeg);

        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 lateral = worldUp.cross(frontViewDirection).normalize();
        Vec3 horizontal = frontViewDirection.scale(Math.cos(az)).add(lateral.scale(Math.sin(az))).normalize();
        Vec3 forward = horizontal.scale(Math.cos(el)).add(new Vec3(0, -1, 0).scale(Math.sin(el))).normalize();

        Vec3 right = forward.cross(worldUp).normalize();
        Vec3 up = right.cross(forward).normalize();
        return new OrthographicCamera(forward, right, up);
    }

    public double screenX(Vec3 point) {
        return point.dot(right);
    }

    public double screenY(Vec3 point) {
        return -point.dot(up);
    }

    public double depth(Vec3 point) {
        return point.dot(forward);
    }
}
