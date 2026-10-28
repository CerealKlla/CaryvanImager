package com.github.cerealklla.caryvanimager.model;

import java.util.ArrayList;
import java.util.List;

import com.github.cerealklla.caryvanimager.render.Box;
import com.github.cerealklla.caryvanimager.render.Face;
import com.github.cerealklla.caryvanimager.render.Vec3;
import com.github.cerealklla.caryvanimager.render.VisibleFace;

/**
 * Real cart geometry -- a direct, hand-transcribed port of Caryvan's own real {@code
 * EasyMeshBuilder}-based entity models ({@code client.renderer.entity.model.HandCartModel}/
 * {@code AnimalCartModel}/{@code WagonModel}, read directly from source before transcribing any of
 * this), replacing an earlier hand-authored SIMPLIFIED stand-in box list (flat bed + rectangular
 * "leg" wheels) that a real user screenshot confirmed looked nothing like the actual in-game asset.
 *
 * <p>Each cart type's static (rest-pose) part tree is built as a {@link PartNode} hierarchy --
 * pivot, rotation (radians, applied X-then-Y-then-Z per part, vanilla's own real order -- see that
 * class's own doc), and local-space boxes tagged {@link Box.Group#BODY}/{@link Box.Group#WHEEL}.
 * {@link #facesFor} walks the tree, transforming every box's 6 faces (reusing {@link Face}'s own
 * per-face corner-selection logic against a LOCAL, not world, box) through the full parent chain
 * into world space, exactly mirroring vanilla's {@code PoseStack}/{@code ModelPart} composition.
 * Face brightness is picked by bucketing each transformed face's world-space normal to the nearest
 * of {@link Face}'s 6 fixed axis directions -- an approximation that holds up well here since every
 * real rotation in these models is a multiple of 45 degrees.
 *
 * <p>Deliberately ports only the STATIC rest pose: wheel spin, the Wagon's broken-wheel variants/
 * fallen-wheel debris/body tilt, and Hand Cart's hurt-shake are all live, runtime-animated state
 * (see each real model's own {@code setupAnim}) that a crafting-preview image of a freshly-assembled
 * cart has no use for -- this renders the same "parked, undamaged, wheels at rest" pose every time.
 */
public final class CartGeometry {

    private CartGeometry() {
    }

    /** @return every face of {@code type}'s real geometry, colored by group from the given species swatches. */
    public static List<VisibleFace> facesFor(final String type, final int bodyColor, final int wheelColor) {
        final PartNode modelRoot = switch (type) {
            case "hand_cart" -> handCart();
            case "animal_cart" -> animalCart();
            case "wagon" -> wagon();
            default -> throw new IllegalArgumentException("Unknown cart type: " + type);
        };
        // Every real Caryvan renderer applies `poseStack.scale(-1.0F, -1.0F, 1.0F)` before
        // submitting its model (confirmed directly, e.g. WagonRenderer#submit) -- the standard
        // vanilla convention that entity-model local space needs both X and Y negated to land in
        // true world orientation. Mathematically identical to a 180-degree rotation about Z, so
        // wrapping the real model's own root in one extra node with zRot=PI replicates it exactly
        // (confirmed live, 2026-10-28: without this, every real-geometry preview rendered upside
        // down -- correct in silhouette/shape, since the placeholder boxes never had enough visual
        // asymmetry for this to be noticeable, but genuinely flipped once there was real detail).
        final PartNode orientedRoot = new PartNode(0, 0, 0, 0, 0, Math.PI);
        orientedRoot.child(modelRoot);
        final List<VisibleFace> faces = new ArrayList<>();
        collectFaces(orientedRoot, bodyColor, wheelColor, faces);
        return faces;
    }

    private static void collectFaces(final PartNode part, final int bodyColor, final int wheelColor, final List<VisibleFace> out) {
        for (final PartNode.LocalBox localBox : part.boxes) {
            final int rgb = localBox.group() == Box.Group.WHEEL ? wheelColor : bodyColor;
            // Face.corners(Box) only reads min/max -- reusing it against a LOCAL-space box is safe,
            // the resulting corners are just in local space too, transformed to world space below.
            final Box local = new Box(localBox.x(), localBox.y(), localBox.z(),
                    localBox.x() + localBox.w(), localBox.y() + localBox.h(), localBox.z() + localBox.d(), localBox.group());
            for (final Face face : Face.values()) {
                final Vec3[] c = face.corners(local);
                final Vec3 world00 = toWorld(part, c[0]);
                final Vec3 world10 = toWorld(part, c[1]);
                final Vec3 world01 = toWorld(part, c[2]);
                final Vec3 worldNormal = toWorldRotationOnly(part, new Vec3(face.nx, face.ny, face.nz));
                out.add(new VisibleFace(world00, world10, world01, brightnessFor(worldNormal), rgb));
            }
        }
        for (final PartNode child : part.children) {
            collectFaces(child, bodyColor, wheelColor, out);
        }
    }

    /** Transforms a point in {@code part}'s own local space to world space, up through every ancestor. */
    private static Vec3 toWorld(final PartNode part, final Vec3 local) {
        final Vec3 rotated = rotateZ(rotateY(rotateX(local, part.xRot), part.yRot), part.zRot);
        final Vec3 translated = new Vec3(rotated.x() + part.x, rotated.y() + part.y, rotated.z() + part.z);
        return part.parent == null ? translated : toWorld(part.parent, translated);
    }

    /** Same composition as {@link #toWorld}, but rotation only (no translation) -- for face normals. */
    private static Vec3 toWorldRotationOnly(final PartNode part, final Vec3 local) {
        final Vec3 rotated = rotateZ(rotateY(rotateX(local, part.xRot), part.yRot), part.zRot);
        return part.parent == null ? rotated : toWorldRotationOnly(part.parent, rotated);
    }

    private static Vec3 rotateX(final Vec3 v, final double a) {
        if (a == 0) {
            return v;
        }
        final double c = Math.cos(a);
        final double s = Math.sin(a);
        return new Vec3(v.x(), v.y() * c - v.z() * s, v.y() * s + v.z() * c);
    }

    private static Vec3 rotateY(final Vec3 v, final double a) {
        if (a == 0) {
            return v;
        }
        final double c = Math.cos(a);
        final double s = Math.sin(a);
        return new Vec3(v.x() * c + v.z() * s, v.y(), -v.x() * s + v.z() * c);
    }

    private static Vec3 rotateZ(final Vec3 v, final double a) {
        if (a == 0) {
            return v;
        }
        final double c = Math.cos(a);
        final double s = Math.sin(a);
        return new Vec3(v.x() * c - v.y() * s, v.x() * s + v.y() * c, v.z());
    }

    private static double brightnessFor(final Vec3 worldNormal) {
        Face best = Face.UP;
        double bestDot = Double.NEGATIVE_INFINITY;
        for (final Face f : Face.values()) {
            final double d = worldNormal.x() * f.nx + worldNormal.y() * f.ny + worldNormal.z() * f.nz;
            if (d > bestDot) {
                bestDot = d;
                best = f;
            }
        }
        return best.brightness;
    }

    // ------------------------------------------------------------------------------------------
    // Hand Cart -- ported from HandCartModel#createLayer/#wheel verbatim (constants/box numbers
    // read directly from that class, not re-derived).
    // ------------------------------------------------------------------------------------------

    private static PartNode handCart() {
        final double rimLength = 7.0;
        final double axleLength = 20.0;
        final double wheelRadius = (rimLength / 2.0) / Math.tan(Math.PI / 8.0);
        final double xOffset = axleLength / 2.0 + 2.0;

        final PartNode root = new PartNode();
        root.child(spokedWheel(xOffset, -wheelRadius, 1.0, -2, rimLength, wheelRadius));
        root.child(spokedWheel(-xOffset, -wheelRadius, 1.0, 0, rimLength, wheelRadius));

        final PartNode body = new PartNode(0, -wheelRadius, 1.0);
        body.child(new PartNode().box(-10, -1, -1, 20, 2, 2, Box.Group.BODY));
        body.child(new PartNode(0, 0, 0, -Math.PI / 2.0, -Math.PI / 2.0, 0).box(-10, -9, -2, 19, 18, 1, Box.Group.BODY));
        body.child(new PartNode().box(-10, -10, -11, 20, 9, 1, Box.Group.BODY));
        body.child(new PartNode(-11, -5, -1, 0, -Math.PI / 2.0, 0).box(-9, -5, -2, 19, 3, 1, Box.Group.BODY));
        body.child(new PartNode(11, -5, -1, 0, Math.PI / 2.0, 0).box(-10, -5, -2, 19, 3, 1, Box.Group.BODY));
        body.child(new PartNode(-11, -5, -1, 0, -Math.PI / 2.0, 0).box(-9, -1, -2, 19, 3, 1, Box.Group.BODY));
        body.child(new PartNode(11, -5, -1, 0, Math.PI / 2.0, 0).box(-10, -1, -2, 19, 3, 1, Box.Group.BODY));
        body.child(new PartNode().box(8, -10, 9, 2, 9, 1, Box.Group.BODY));
        body.child(new PartNode().box(-10, -10, 9, 2, 9, 1, Box.Group.BODY));
        body.child(new PartNode().box(-8, -10, 9, 16, 3, 1, Box.Group.BODY));
        body.child(new PartNode().box(-8, -6, 9, 16, 3, 1, Box.Group.BODY));
        final PartNode shaft = new PartNode(0, -5, -15, 0, Math.PI / 2.0, 0);
        shaft.box(-4, 0, -7, 20, 2, 1, Box.Group.BODY);
        shaft.box(-4, 0, 6, 20, 2, 1, Box.Group.BODY);
        body.child(shaft);
        root.child(body);
        return root;
    }

    /** Shared 8-spoke wheel shape -- {@code boxOffset} bakes the hub/rim/spoke's own X-offset within an asymmetric wheel (Hand Cart's own left/right-specific boxes); 0 for a symmetric wheel. */
    private static PartNode spokedWheel(final double pivotX, final double pivotY, final double pivotZ,
            final double boxOffset, final double rimLength, final double wheelRadius) {
        final PartNode wheel = new PartNode(pivotX, pivotY, pivotZ);
        wheel.box(boxOffset, -1, -1, 2, 2, 2, Box.Group.WHEEL);
        for (int i = 0; i < 8; i++) {
            final double rot = i * Math.PI / 4.0;
            wheel.child(new PartNode(0, 0, 0, rot, 0, 0).box(boxOffset, -rimLength / 2.0, wheelRadius - 1, 2, rimLength, 1, Box.Group.WHEEL));
            wheel.child(new PartNode(0, 0, 0, rot, 0, 0).box(0.5 + boxOffset, 1, -0.5, 1, wheelRadius - 2, 1, Box.Group.WHEEL));
        }
        return wheel;
    }

    // ------------------------------------------------------------------------------------------
    // Animal Cart -- ported from AnimalCartModel#createLayer/#leftWheel/#rightWheel verbatim.
    // ------------------------------------------------------------------------------------------

    private static PartNode animalCart() {
        final PartNode root = new PartNode();
        root.child(animalCartWheel(14.5, -2.0));
        root.child(animalCartWheel(-14.5, 0.0));

        final PartNode body = new PartNode(0, -11, 1.0);
        body.child(new PartNode().box(-12.5, -1, -1, 25, 2, 2, Box.Group.BODY));
        body.child(new PartNode(0, 0, 0, -Math.PI / 2.0, -Math.PI / 2.0, 0).box(-15.5, -10, -2, 29, 20, 1, Box.Group.BODY));
        final PartNode shaft = new PartNode(0, -5, -15, 0, Math.PI / 2.0, 0);
        shaft.box(0, -0.5, -8, 20, 2, 1, Box.Group.BODY);
        shaft.box(0, -0.5, 7, 20, 2, 1, Box.Group.BODY);
        body.child(shaft);
        body.child(new PartNode(0, 0, 0, -Math.PI / 2.0, 0, Math.PI / 2.0).box(-10, -14.5, 9, 8, 31, 2, Box.Group.BODY));
        body.child(new PartNode(0, 0, 0, -Math.PI / 2.0, 0, Math.PI / 2.0).box(-10, -14.5, -11, 8, 31, 2, Box.Group.BODY));
        body.child(new PartNode().box(-9, -10, 12.5, 18, 8, 2, Box.Group.BODY));
        body.child(new PartNode().box(-9, -10, -16.5, 18, 8, 2, Box.Group.BODY));
        root.child(body);
        return root;
    }

    /** Animal Cart's own wheel is pivoted at {@code (pivotX, -11, 1)}, box offset {@code boxOffset} (asymmetric, matching the real left/right-specific box numbers). */
    private static PartNode animalCartWheel(final double pivotX, final double boxOffset) {
        final PartNode wheel = new PartNode(pivotX, -11, 1.0);
        wheel.box(boxOffset, -1, -1, 2, 2, 2, Box.Group.WHEEL);
        final double rimBoxOffset = boxOffset == 0.0 ? 0.0 : -2.0;
        final double spokeBoxOffset = boxOffset == 0.0 ? 0.5 : -1.5;
        for (int i = 0; i < 8; i++) {
            final double rot = i * Math.PI / 4.0;
            wheel.child(new PartNode(0, 0, 0, rot, 0, 0).box(rimBoxOffset, -4.5, 9.86, 2, 9, 1, Box.Group.WHEEL));
            wheel.child(new PartNode(0, 0, 0, rot, 0, 0).box(spokeBoxOffset, 1, -0.5, 1, 9, 1, Box.Group.WHEEL));
        }
        return wheel;
    }

    // ------------------------------------------------------------------------------------------
    // Wagon -- ported from WagonModel#createLayer/#wheel verbatim, STATIC rest pose only (the 4
    // "Normal" wheels at their baked position; broken-wheel variants, fallen debris, and the body
    // tilt are runtime damage-animation state with nothing to show on a freshly-assembled preview).
    // ------------------------------------------------------------------------------------------

    private static PartNode wagon() {
        final double wheelRadius = 11.0;
        final double wheelY = -wheelRadius;
        final double lengthScale = 1.5;
        final double trackScale = 1.334025;
        final double wheelTrackX = 14.0 * trackScale;
        final double wheelFrontZ = -10.0 * lengthScale;
        final double wheelBackZ = 10.0 * lengthScale;

        final PartNode root = new PartNode();
        root.child(wagonWheel(wheelTrackX, wheelY, wheelFrontZ));
        root.child(wagonWheel(-wheelTrackX, wheelY, wheelFrontZ));
        root.child(wagonWheel(wheelTrackX, wheelY, wheelBackZ));
        root.child(wagonWheel(-wheelTrackX, wheelY, wheelBackZ));

        final PartNode body = new PartNode(0, -wheelRadius, 0);
        body.child(new PartNode(0, 0, 0, -Math.PI / 2.0, -Math.PI / 2.0, 0).box(-13, -9, -2, 27, 18.0 * lengthScale, 1, Box.Group.BODY));
        body.child(new PartNode(0, 0, wheelFrontZ).box(-wheelTrackX, -1, -1, wheelTrackX * 2.0, 2, 2, Box.Group.BODY));
        body.child(new PartNode(0, 0, wheelBackZ).box(-wheelTrackX, -1, -1, wheelTrackX * 2.0, 2, 2, Box.Group.BODY));

        final double postHeight = 8.0;
        final double postFrontZ = -15.0 * lengthScale;
        final double postBackZ = 14.0 * lengthScale;
        body.child(wagonCornerPost(-14, -postHeight, postFrontZ));
        body.child(wagonCornerPost(13, -postHeight, postFrontZ));
        body.child(wagonCornerPost(-14, -postHeight, postBackZ));
        body.child(wagonCornerPost(13, -postHeight, postBackZ));

        body.child(wagonSideRail(-15, -8, lengthScale));
        body.child(wagonSideRail(-15, -3, lengthScale));
        body.child(wagonSideRail(15, -8, lengthScale));
        body.child(wagonSideRail(15, -3, lengthScale));

        final double sideFrontZ = -15.0 * lengthScale;
        final double sideBackZ = 15.0 * lengthScale;
        body.child(new PartNode(0, -5, sideFrontZ).box(-14, -5, -1, 28, 4, 1, Box.Group.BODY));
        body.child(new PartNode(0, -5, sideBackZ).box(-14, -5, -1, 28, 4, 1, Box.Group.BODY));

        final double benchZ = -16.0 * lengthScale;
        body.child(new PartNode(0, -9, benchZ).box(-11, -7, 0, 22, 7, 1, Box.Group.BODY));
        body.child(new PartNode(0, -9, benchZ).box(-11, -1, -8, 22, 1, 8, Box.Group.BODY));

        root.child(body);
        return root;
    }

    private static PartNode wagonWheel(final double pivotX, final double pivotY, final double pivotZ) {
        final PartNode wheel = new PartNode(pivotX, pivotY, pivotZ);
        wheel.box(-1, -1, -1, 2, 2, 2, Box.Group.WHEEL);
        for (int i = 0; i < 8; i++) {
            final double rot = i * Math.PI / 4.0;
            wheel.child(new PartNode(0, 0, 0, rot, 0, 0).box(-1, -4.5, 9.86, 2, 9, 1, Box.Group.WHEEL));
            wheel.child(new PartNode(0, 0, 0, rot, 0, 0).box(-0.5, 1, -0.5, 1, 9, 1, Box.Group.WHEEL));
        }
        return wheel;
    }

    private static PartNode wagonCornerPost(final double x, final double y, final double z) {
        return new PartNode(x, y, z).box(-0.5, 0, -0.5, 1, 9, 1, Box.Group.BODY);
    }

    private static PartNode wagonSideRail(final double x, final double y, final double lengthScale) {
        final double yRot = x < 0.0 ? Math.PI / 2.0 : -Math.PI / 2.0;
        return new PartNode(x, y, 0, 0, yRot, 0).box(-15.0 * lengthScale, -1, -0.5, 30.0 * lengthScale, 2, 1, Box.Group.BODY);
    }
}
