package com.github.cerealklla.caryvanimager.color;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

/**
 * A cart part's color, for a given type+species, is the AVERAGE color of that species' own real
 * existing whole-model texture sheet (e.g. {@code hand_cart_oak.png}) -- not a UV-mapped sample.
 * Confirmed reasonable because every one of these sheets is already a near-uniform tiled-plank
 * texture with no hand-painted per-region detail (see {@code WagonModel}'s own doc: "only 7 distinct
 * colors across 500 random samples") -- averaging the whole sheet gives a correct, visually distinct
 * per-species color with no UV layout to get wrong, and crucially needs no new split texture art:
 * this renderer binds the body boxes to the body species' own sheet and the wheel boxes to the wheel
 * species' own sheet independently, something Caryvan's real live-game renderer can't do yet (it
 * binds one texture per whole entity per draw call) but this standalone program has no such
 * constraint for, since it isn't using Caryvan's renderer at all.
 */
public final class SpeciesSwatch {

    private static final Map<Path, Integer> CACHE = new ConcurrentHashMap<>();

    private SpeciesSwatch() {
    }

    /** @return the average opaque RGB (no alpha) of {@code <type>_<species>.png} under {@code texturesDir}, or a neutral gray if the file can't be read. */
    public static int averageColor(final Path texturesDir, final String type, final String species) {
        final Path file = texturesDir.resolve(type + "_" + species + ".png");
        return CACHE.computeIfAbsent(file, SpeciesSwatch::computeAverage);
    }

    private static int computeAverage(final Path file) {
        try {
            final BufferedImage image = ImageIO.read(file.toFile());
            if (image == null) {
                return 0x808080;
            }
            long r = 0, g = 0, b = 0, count = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    final int argb = image.getRGB(x, y);
                    final int alpha = (argb >>> 24) & 0xFF;
                    if (alpha < 16) {
                        continue;
                    }
                    r += (argb >> 16) & 0xFF;
                    g += (argb >> 8) & 0xFF;
                    b += argb & 0xFF;
                    count++;
                }
            }
            if (count == 0) {
                return 0x808080;
            }
            return (int) ((r / count) << 16 | (g / count) << 8 | (b / count));
        } catch (IOException | RuntimeException e) {
            return 0x808080;
        }
    }

    static boolean exists(final Path texturesDir, final String type, final String species) {
        return Files.exists(texturesDir.resolve(type + "_" + species + ".png"));
    }
}
