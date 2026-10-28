package com.github.cerealklla.caryvanimager.pipeline;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Renders a single {@code .request.json} to its sibling {@code .png}/{@code .small.png}, isolated
 * from every other request -- same error-isolation reasoning as BluepryntImager's own {@code
 * BlueprintFileProcessor}. Simpler than that class in one real way: a (type, bodySpecies,
 * wheelSpecies) combo is immutable once rendered -- a player can't "edit" it the way a Blueprint can
 * be re-saved -- so there's no mtime/freshness comparison at all, just "does the output already
 * exist." A permanently-malformed request (bad type/species name) is still only retried once per
 * file, via the same marker-file idiom, so the watcher never spins on it forever.
 */
public final class PreviewFileProcessor {

    private static final Gson GSON = new Gson();

    private final PreviewRenderer renderer;

    public PreviewFileProcessor(final PreviewRenderer renderer) {
        this.renderer = renderer;
    }

    public void process(final Path requestFile) {
        try {
            final Path fullPngFile = withExtension(requestFile, ".png");
            final Path smallPngFile = withExtension(requestFile, ".small.png");
            final Path errorFile = errorMarkerFor(requestFile);

            if (Files.exists(fullPngFile) && Files.exists(smallPngFile)) {
                return;
            }
            if (Files.exists(errorFile)) {
                return;
            }

            final String json = Files.readString(requestFile, StandardCharsets.UTF_8);
            final PreviewRequest request = GSON.fromJson(json, PreviewRequest.class);
            renderer.render(request, fullPngFile, smallPngFile);
            System.out.println("[render] " + requestFile);
        } catch (IOException | JsonSyntaxException | IllegalArgumentException e) {
            System.err.println("[render-failed] " + requestFile + ": " + e);
            writeErrorMarker(requestFile, e);
        }
    }

    private static Path withExtension(final Path requestFile, final String newExtension) {
        final String name = requestFile.getFileName().toString();
        final String base = name.endsWith(".request.json") ? name.substring(0, name.length() - ".request.json".length()) : name;
        return requestFile.resolveSibling(base + newExtension);
    }

    private static Path errorMarkerFor(final Path requestFile) {
        return requestFile.resolveSibling(requestFile.getFileName().toString() + ".error");
    }

    private static void writeErrorMarker(final Path requestFile, final Exception failure) {
        try {
            final StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.writeString(errorMarkerFor(requestFile), Instant.now() + "\n" + trace, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[error-marker-failed] " + requestFile + ": " + e);
        }
    }
}
