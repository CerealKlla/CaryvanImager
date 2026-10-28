package com.github.cerealklla.caryvanimager.model;

import java.util.ArrayList;
import java.util.List;

import com.github.cerealklla.caryvanimager.render.Box;

/**
 * One node of a real cart model's part hierarchy -- a direct, minimal re-implementation of the
 * semantics Caryvan's own {@code client.renderer.entity.model.EasyMeshBuilder} wraps around
 * vanilla's {@code PartPose}/{@code ModelPart} system (confirmed by reading that class and its real
 * decompiled vanilla source directly, not assumed): a part has its own pivot offset {@code (x,y,z)}
 * relative to its PARENT's local space, its own rotation {@code (xRot,yRot,zRot)} in radians applied
 * about its own pivot in X-then-Y-then-Z order (vanilla's real order, confirmed via {@code
 * ModelPart#translateAndRotate}'s {@code new Quaternionf().rotationZYX(zRot, yRot, xRot)} -- JOML's
 * {@code rotationZYX} applies X first, then Y, then Z, to a raw vector), zero or more boxes
 * authored in ITS OWN local space (exactly as a real model's {@code addBox(x,y,z,w,h,d)} call would
 * be), and zero or more children.
 *
 * <p>{@link CartGeometry} walks a tree of these to emit real, correctly-posed {@code VisibleFace}s
 * for each cart type's actual geometry (shafts, slatted sides, octagonal spoked wheels) instead of
 * {@code CartGeometry}'s own earlier hand-authored placeholder boxes.
 */
final class PartNode {

    final double x;
    final double y;
    final double z;
    final double xRot;
    final double yRot;
    final double zRot;
    final List<LocalBox> boxes = new ArrayList<>();
    final List<PartNode> children = new ArrayList<>();
    PartNode parent;

    /** One box in this part's own local (pre-rotation, pre-translation) space. */
    record LocalBox(double x, double y, double z, double w, double h, double d, Box.Group group) {
    }

    PartNode() {
        this(0, 0, 0, 0, 0, 0);
    }

    PartNode(final double x, final double y, final double z) {
        this(x, y, z, 0, 0, 0);
    }

    PartNode(final double x, final double y, final double z, final double xRot, final double yRot, final double zRot) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xRot = xRot;
        this.yRot = yRot;
        this.zRot = zRot;
    }

    PartNode box(final double x, final double y, final double z, final double w, final double h, final double d, final Box.Group group) {
        boxes.add(new LocalBox(x, y, z, w, h, d, group));
        return this;
    }

    PartNode child(final PartNode child) {
        child.parent = this;
        children.add(child);
        return this;
    }
}
