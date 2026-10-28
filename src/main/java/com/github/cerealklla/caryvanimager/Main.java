package com.github.cerealklla.caryvanimager;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.github.cerealklla.caryvanimager.pipeline.PreviewFileProcessor;
import com.github.cerealklla.caryvanimager.pipeline.PreviewRenderer;
import com.github.cerealklla.caryvanimager.pipeline.PreviewWatcher;

/**
 * CLI entry point -- same shape as BluepryntImager's own {@code Main} (same flag names where they
 * mean the same thing). {@code --textures} points directly at Caryvan's own loose entity-texture
 * PNGs (e.g. {@code assets/caryvan/textures/entity/}) -- no extraction step needed here, unlike
 * BluepryntImager's block-texture atlas extraction, since these files are already standalone PNGs.
 *
 * <pre>
 * java -jar CaryvanImager.jar \
 *     --requests /path/to/server/caryvanimager/requests \
 *     --textures /path/to/Caryvan/src/main/resources/assets/caryvan/textures/entity \
 *     [--width 512] [--height 384] [--small-width 128] [--small-height 96] [--once]
 * </pre>
 */
public final class Main {

    public static void main(final String[] args) throws Exception {
        final Map<String, String> opts = parseArgs(args);

        final Path requestsRoot = Path.of(require(opts, "requests"));
        final Path texturesDir = Path.of(require(opts, "textures"));
        final int width = Integer.parseInt(opts.getOrDefault("width", "512"));
        final int height = Integer.parseInt(opts.getOrDefault("height", "384"));
        final int smallWidth = Integer.parseInt(opts.getOrDefault("small-width", "128"));
        final int smallHeight = Integer.parseInt(opts.getOrDefault("small-height", "96"));
        final boolean once = opts.containsKey("once");

        final PreviewRenderer renderer = new PreviewRenderer(texturesDir, width, height, smallWidth, smallHeight);
        final PreviewFileProcessor processor = new PreviewFileProcessor(renderer);
        final PreviewWatcher watcher = new PreviewWatcher(requestsRoot, processor);

        watcher.scanOnce();
        if (!once) {
            System.out.println("Watching " + requestsRoot + " for new preview requests...");
            watcher.watchForever();
        }
    }

    private static String require(final Map<String, String> opts, final String key) {
        final String value = opts.get(key);
        if (value == null) {
            System.err.println("Missing required --" + key + " argument");
            System.exit(1);
        }
        return value;
    }

    private static Map<String, String> parseArgs(final String[] args) {
        final Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            final String key = arg.substring(2);
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                opts.put(key, args[++i]);
            } else {
                opts.put(key, "true");
            }
        }
        return opts;
    }
}
