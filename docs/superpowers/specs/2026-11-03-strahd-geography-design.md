# Barovian overworld geography (0.3.0)

David supplied a Barovia map and chose “Match the geography; keep pixel art.”
Replace the flat line of POIs with a fixed, original tile layout informed by the
reference: Barovia in the east, Tser Pool in the eastern river valley, Vallaki
below Lake Zarovich in the north-central region, wooded valleys, branching
winding roads, western lake country, northern Mount Baratok and southern Mount
Ghakis. Keep existing Forge pixel assets and all eight playable POIs and both
chapters. Future settlements/chapters are not added as playable content.

Use an optional campaign-owned authored-map JSON with top-down palette rows,
named fixed POI coordinates and road polylines. Validate dimensions, palette,
indices, anchors, road bounds and reachability before use. World generation
paints these biomes, places the existing POIs, and paints explicit roads instead
of automatic straight routes. Save the existing terrain/biome/POI arrays without
a save-format change. Campaigns without this field retain their procedural path.
World map/minimap and exploration use the same tiles and collision data.

Choose a 300×180 world (multiples of the current 30-tile chunk size), 16-pixel
tiles, existing 4-pixel minimap tiles. Existing forest/rock/water/snow sprites and
autotiles communicate the regions. Block water and mountain cores; roads form
passable bridges and passes. All eight POIs must be reachable from Barovia with
space for the player at bends and approaches. Decorations must render for
negative seeds and ordinary collision terrain must not be mistaken for a
structure. Original reference artwork is not imported into game assets.

Version decision: keep `fork.curse-of-strahd`, bump to 0.3.0, require a new run.
The frozen 0.2.0 candidate and its saves remain intact. Do not migrate or rewrite
saved worlds or existing installations. Preserve unrelated theme changes; no
commits, publishing, visible game launches or desktop automation.

Verify authored-grid validation, deterministic placement/roads, river crossing,
blocked terrain and all-POI reachability, plus affected campaign/save regressions.
Inspect a pixel preview and actual engine-generated minimap. Run a hidden native
two-chapter journey and fresh Continue on the new world, with a Bellwake switch
to exercise the default procedural path. Package a separate Windows candidate,
verify manifest and current source/resource hashes, and provide launch/manual
navigation instructions. Played balance and physical walking remain manual.
