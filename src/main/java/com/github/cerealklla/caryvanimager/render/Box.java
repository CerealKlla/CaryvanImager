package com.github.cerealklla.caryvanimager.render;

/**
 * One axis-aligned box in absolute world-space units, tagged with which part group it belongs to
 * (so the renderer can color it from the right species' swatch) -- see {@code model.CartGeometry}'s
 * own doc for why this program uses simplified hand-authored stand-in boxes instead of porting
 * Caryvan's real {@code EasyMeshBuilder}-based entity models verbatim.
 */
public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Group group) {

    public enum Group { BODY, WHEEL }

    public java.util.List<VisibleFace> faces(int rgb) {
        java.util.List<VisibleFace> faces = new java.util.ArrayList<>(6);
        for (Face face : Face.values()) {
            Vec3[] c = face.corners(this);
            faces.add(new VisibleFace(c[0], c[1], c[2], face.brightness, rgb));
        }
        return faces;
    }
}
