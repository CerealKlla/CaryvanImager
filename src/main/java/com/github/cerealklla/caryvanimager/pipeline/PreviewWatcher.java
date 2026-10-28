package com.github.cerealklla.caryvanimager.pipeline;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Watches the shared request directory (recursively -- {@link WatchService} itself is not
 * recursive) and renders any new {@code .request.json} via {@link PreviewFileProcessor}. Direct
 * port of BluepryntImager's own {@code BlueprintWatcher} shape -- same resilience (an individual
 * event/overflow never kills the loop), same {@code scanOnce}/{@code watchForever} split.
 */
public final class PreviewWatcher {

    private static final String SUFFIX = ".request.json";

    private final Path root;
    private final PreviewFileProcessor processor;

    public PreviewWatcher(final Path root, final PreviewFileProcessor processor) {
        this.root = root;
        this.processor = processor;
    }

    public void scanOnce() throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> p.toString().endsWith(SUFFIX)).forEach(processor::process);
        }
    }

    public void watchForever() throws IOException {
        try (WatchService service = FileSystems.getDefault().newWatchService()) {
            final Map<WatchKey, Path> keysByDir = new HashMap<>();
            registerAll(root, service, keysByDir);

            while (true) {
                final WatchKey key;
                try {
                    key = service.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                final Path dir = keysByDir.get(key);
                if (dir != null) {
                    handleEvents(key, dir, service, keysByDir);
                }

                if (!key.reset()) {
                    keysByDir.remove(key);
                }
            }
        }
    }

    private void handleEvents(final WatchKey key, final Path dir, final WatchService service, final Map<WatchKey, Path> keysByDir) {
        for (final WatchEvent<?> event : key.pollEvents()) {
            try {
                if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                    scanOnce();
                    continue;
                }
                final Path changed = dir.resolve((Path) event.context());
                if (Files.isDirectory(changed)) {
                    registerAll(changed, service, keysByDir);
                } else if (changed.toString().endsWith(SUFFIX)) {
                    processor.process(changed);
                }
            } catch (Exception e) {
                System.err.println("[watch-event-failed] " + e);
            }
        }
    }

    private static void registerAll(final Path start, final WatchService service, final Map<WatchKey, Path> keysByDir) throws IOException {
        Files.createDirectories(start);
        try (Stream<Path> walk = Files.walk(start)) {
            for (final Path dir : (Iterable<Path>) walk.filter(Files::isDirectory)::iterator) {
                final WatchKey key = dir.register(service, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
                keysByDir.put(key, dir);
            }
        }
    }
}
