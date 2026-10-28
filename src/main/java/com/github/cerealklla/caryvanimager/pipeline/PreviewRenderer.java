package com.github.cerealklla.caryvanimager.pipeline;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import com.github.cerealklla.caryvanimager.color.SpeciesSwatch;
import com.github.cerealklla.caryvanimager.model.CartGeometry;
import com.github.cerealklla.caryvanimager.render.Box;
import com.github.cerealklla.caryvanimager.render.Rasterizer;
import com.github.cerealklla.caryvanimager.render.VisibleFace;

/** Renders one {@link PreviewRequest} to a full-size + small PNG pair, mirroring BluepryntImager's own {@code BlueprintRenderer} shape. */
public final class PreviewRenderer {

    private final Path texturesDir;
    private final int width;
    private final int height;
    private final int smallWidth;
    private final int smallHeight;

    public PreviewRenderer(final Path texturesDir, final int width, final int height, final int smallWidth, final int smallHeight) {
        this.texturesDir = texturesDir;
        this.width = width;
        this.height = height;
        this.smallWidth = smallWidth;
        this.smallHeight = smallHeight;
    }

    public void render(final PreviewRequest request, final Path fullPngFile, final Path smallPngFile) throws IOException {
        final List<Box> boxes = CartGeometry.boxesFor(request.type());
        final int bodyColor = SpeciesSwatch.averageColor(texturesDir, request.type(), request.bodySpecies());
        final int wheelColor = SpeciesSwatch.averageColor(texturesDir, request.type(), request.wheelSpecies());

        final List<VisibleFace> faces = new java.util.ArrayList<>();
        for (final Box box : boxes) {
            faces.addAll(box.faces(box.group() == Box.Group.WHEEL ? wheelColor : bodyColor));
        }

        final BufferedImage full = new Rasterizer(width, height).render(faces);
        final BufferedImage small = new Rasterizer(smallWidth, smallHeight).render(faces);
        ImageIO.write(full, "png", fullPngFile.toFile());
        ImageIO.write(small, "png", smallPngFile.toFile());
    }
}
