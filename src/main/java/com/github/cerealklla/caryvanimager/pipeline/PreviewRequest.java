package com.github.cerealklla.caryvanimager.pipeline;

/**
 * One request file's content, written by Caryvan's own {@code wainwright.preview.WainwrightPreviewStorage}
 * -- {@code {"type": "hand_cart"|"animal_cart"|"wagon", "bodySpecies": "<WoodType name>", "wheelSpecies": "<WoodType name>"}}.
 * The combo is immutable once rendered (unlike a Blueprint, which a player can re-save) -- there's no
 * mtime/freshness concept here at all, see {@code PreviewFileProcessor}'s own doc.
 */
public record PreviewRequest(String type, String bodySpecies, String wheelSpecies) {
}
