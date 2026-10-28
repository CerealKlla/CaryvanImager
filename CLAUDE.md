# CaryvanImager

A standalone Java program -- **not a Minecraft mod, no NeoForge/Minecraft-runtime dependency at
all** -- that watches Caryvan's Wainwright Station preview-request directory and renders a
flat-shaded orthographic PNG pair for every (cart type, body species, wheel species) combo it finds.
Runs as its own process on the same physical machine as the Minecraft server. Part of the same mod
suite (`C:\Users\benja\Minecraft\MinecraftMods\`) but intentionally decoupled from it -- same role
BluepryntImager already plays for Blueprynts, same reasoning for being a separate program (see that
project's own CLAUDE.md for the general "why not a mod" case, which applies identically here).

## Context directory -- read this first

`context/` is a **separate private repo** (https://github.com/CerealKlla/caryvanimager-context),
not part of this one -- it's listed in `.gitignore` here and must never be committed to this repo.
It's cloned as a subdirectory at `context/` for local convenience. If this directory is missing
(e.g. a fresh clone of just this repo), restore it with:

```
git clone https://github.com/CerealKlla/caryvanimager-context.git context
```

- `context/design-document.md` -- the authoritative design: why this exists as a separate program,
  why simplified stand-in geometry instead of a real `ModelPart` port, the species-swatch coloring
  technique, the render pipeline, and open items.
- `context/decisions.md` -- dated log of decisions made during implementation, with rationale.
- `context/classes/` -- one short markdown file per implemented class (or a small tightly-related
  group) -- public surface, key state, collaborators. Read the relevant file here before opening the
  actual source, and before editing a class update its file to match.

**Keep this system current as you work:**
- When a design decision is made that conflicts with or is absent from context/design-document.md,
  update it directly and add a dated entry to context/decisions.md explaining the change.
- When a class is added or its public surface changes, add or update its file in `context/classes/`.
- Don't let source and these docs drift -- treat updating them as part of finishing the change, not
  optional cleanup.
- `context/` has its own git history, independent of this repo's commits. Commit and push changes
  there separately (`git -C context add . && git -C context commit -m "..." && git -C context push`).

## Status

Scaffolded and V1 implemented 2026-10-28, same session as Wainwright Station's own Part 2 (the
crafting structure + GUI this feeds) -- see context/decisions.md for the full dated history. In
short: a hand-authored, deliberately SIMPLIFIED box list per cart type (`model.CartGeometry`) --
not a port of Caryvan's real `EasyMeshBuilder`-based entity models, which this plain-Java program
has no clean way to read (no `ModelPart` baking available outside a real Minecraft runtime) and
which would be a large, constantly-drifting hand-transcription effort for a small crafting-preview
icon anyway. Each box is tagged `BODY` or `WHEEL` and colored from the AVERAGE color of that part's
species' own real existing whole-model texture sheet (e.g. `wagon_oak.png`) -- reasonable because
those sheets are already near-uniform tiled-plank textures with no hand-painted per-region detail
(confirmed directly: `WagonModel`'s own doc notes "only 7 distinct colors across 500 random
samples"). This sidesteps the texture-art dependency entirely: this renderer binds the body boxes
to the body species' own sheet and the wheel boxes to the wheel species' own sheet independently,
something Caryvan's real live-game renderer can't do yet (one texture per whole entity per draw
call) but this standalone program has no such constraint for, since it isn't using Caryvan's
renderer at all.

Reuses BluepryntImager's proven orthographic-projection/Z-buffer-rasterization math verbatim
(`render.Vec3`/`OrthographicCamera`/`Transform2D`/`Rasterizer`'s core loop) -- copied, not shared as
a dependency, same "no shared code between these two standalone programs" precedent BluepryntImager
itself already set relative to Blueprynts. Simplified in one real way: faces carry a flat `rgb` int
instead of a UV-mapped texture id, since the simplified box geometry has no meaningful per-pixel UV
layout to sample against -- no `TextureSet`/UV-inversion math needed at all.

Pipeline shape (`pipeline.*`) is a direct, simplified port of BluepryntImager's own `BlueprintWatcher`/
`BlueprintFileProcessor`/`BlueprintRenderer` trio -- simpler in one real way: a (type, bodySpecies,
wheelSpecies) combo is immutable once rendered (no "re-save" concept exists, unlike a Blueprint), so
there's no mtime/freshness comparison anywhere, just "does the output already exist."

**Verified**: `./gradlew build` green. Smoke-tested end-to-end against a real request
(`wagon`/`oak`/`birch`) -- produced a correctly two-toned (brown bed, tan wheels), recognizable
preview image on the first real run.

**Launch**: `tools\start-caryvanimager.ps1` in the parent `Minecraft\` project (mirrors
`start-bluepryntimager.ps1` exactly) -- `-Target DevServer|Production|Custom`. Unlike
BluepryntImager, `--textures` is NOT per-target: it always points directly at Caryvan's own loose
source-resource PNGs (`MinecraftMods\Caryvan\src\main\resources\assets\caryvan\textures\entity\`),
since no extraction step is needed (these are already standalone files, not baked into a texture
atlas) and the same physical machine hosts both the Dev Server and Production. `start-server.ps1`
calls it automatically (`-Target Production`), same proactive fix as BluepryntImager's own launch-
story gap closure -- added from the start this time, not after getting bitten by it once.

**Not yet done / open**: camera angle (35°/25°) is a first guess, not tuned by eye against a real
in-game screenshot; wheel boxes currently render as flat rectangular "legs" rather than anything
circular -- reads correctly as a distinct second color but not yet visually wheel-shaped (an easy,
isolated follow-up inside `CartGeometry` alone, no pipeline/integration change needed); Carriage is
not supported (matches Wainwright Station's own explicit Carriage exclusion); if/when Caryvan's live
renderer eventually gets a real body/wheel texture split (held pending real split texture art, see
Caryvan's own `context/decisions.md`), this program's species-swatch approach stays valid regardless
-- it was never blocked on that split to begin with.
