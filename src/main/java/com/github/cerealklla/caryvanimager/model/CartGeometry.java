package com.github.cerealklla.caryvanimager.model;

import java.util.List;

import com.github.cerealklla.caryvanimager.render.Box;

/**
 * Hand-authored, SIMPLIFIED stand-in geometry for each cart type -- NOT a port of Caryvan's real
 * {@code EasyMeshBuilder}-based entity models (hand_cart/animal_cart/wagon {@code *Model.java}),
 * which use real articulated parent-child cuboid hierarchies (8-spoke wheels, axles, corner posts,
 * a driving bench, broken-wheel variants) this standalone program deliberately does not try to
 * reproduce -- a plain-Java program with no NeoForge/Minecraft runtime dependency has no clean way
 * to walk a baked {@code ModelPart} tree, and transcribing every box/rotation by hand across 3 model
 * files would be a large, constantly-drifting duplication for a small crafting-preview icon.
 *
 * <p>Instead, each cart type gets a small flat list of absolute world-space boxes, each tagged
 * {@code BODY} or {@code WHEEL} -- enough to read as "this cart shape" and show the two parts in two
 * visibly distinct colors, which is the actual thing the Wainwright Station Body tab needs to
 * communicate (mixed wood species), not a pixel-accurate render. If/when this needs to look more
 * like the real models, extend the box lists here -- no other class needs to change.
 */
public final class CartGeometry {

    private CartGeometry() {
    }

    public static List<Box> boxesFor(final String type) {
        return switch (type) {
            case "hand_cart" -> twoWheelCart(18, 12, 6);
            case "animal_cart" -> twoWheelCart(22, 13, 6);
            case "wagon" -> wagon();
            default -> throw new IllegalArgumentException("Unknown cart type: " + type);
        };
    }

    /** Hand Cart / Animal Cart: one bed on one axle, 2 wheels, {@code bedLength}/{@code bedWidth} in world units, {@code wheelRadius} the wheel's half-height/half-depth. */
    private static List<Box> twoWheelCart(final double bedLength, final double bedWidth, final double wheelRadius) {
        final double bedHalfLength = bedLength / 2.0;
        final double bedHalfWidth = bedWidth / 2.0;
        final double bedBottomY = wheelRadius * 2.0;
        final double bedTopY = bedBottomY + 5.0;
        final double wallTopY = bedTopY + 4.0;

        return List.of(
                new Box(-bedHalfWidth, bedBottomY, -bedHalfLength, bedHalfWidth, bedTopY, bedHalfLength, Box.Group.BODY),
                new Box(-bedHalfWidth - 1, bedTopY, -bedHalfLength, -bedHalfWidth, wallTopY, bedHalfLength, Box.Group.BODY),
                new Box(bedHalfWidth, bedTopY, -bedHalfLength, bedHalfWidth + 1, wallTopY, bedHalfLength, Box.Group.BODY),
                new Box(-bedHalfWidth - 3, 0, -wheelRadius, -bedHalfWidth - 1, wheelRadius * 2.0, wheelRadius, Box.Group.WHEEL),
                new Box(bedHalfWidth + 1, 0, -wheelRadius, bedHalfWidth + 3, wheelRadius * 2.0, wheelRadius, Box.Group.WHEEL));
    }

    /** Wagon: two axles (4 wheels), a longer open bed with side walls. */
    private static List<Box> wagon() {
        final double wheelRadius = 7.0;
        final double wheelY0 = 0.0;
        final double wheelY1 = wheelRadius * 2.0;
        final double axleZ = 16.0;
        final double bedBottomY = wheelY1;
        final double bedTopY = bedBottomY + 6.0;
        final double wallTopY = bedTopY + 6.0;
        final double bedHalfLength = 20.0;
        final double bedHalfWidth = 9.0;

        return List.of(
                new Box(-bedHalfWidth, bedBottomY, -bedHalfLength, bedHalfWidth, bedTopY, bedHalfLength, Box.Group.BODY),
                new Box(-bedHalfWidth - 1, bedTopY, -bedHalfLength, -bedHalfWidth, wallTopY, bedHalfLength, Box.Group.BODY),
                new Box(bedHalfWidth, bedTopY, -bedHalfLength, bedHalfWidth + 1, wallTopY, bedHalfLength, Box.Group.BODY),
                new Box(-bedHalfWidth, bedTopY, -bedHalfLength - 1, bedHalfWidth, wallTopY, -bedHalfLength, Box.Group.BODY),
                new Box(-bedHalfWidth, bedTopY, bedHalfLength, bedHalfWidth, wallTopY, bedHalfLength + 1, Box.Group.BODY),
                wheelBox(-bedHalfWidth - 1, -axleZ, wheelY0, wheelY1, wheelRadius),
                wheelBox(bedHalfWidth, -axleZ, wheelY0, wheelY1, wheelRadius),
                wheelBox(-bedHalfWidth - 1, axleZ, wheelY0, wheelY1, wheelRadius),
                wheelBox(bedHalfWidth, axleZ, wheelY0, wheelY1, wheelRadius));
    }

    private static Box wheelBox(final double minX, final double centerZ, final double minY, final double maxY, final double radius) {
        return new Box(minX, minY, centerZ - radius, minX + 2.0, maxY, centerZ + radius, Box.Group.WHEEL);
    }
}
