# Barovian Geography Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans inline, then one fresh
> whole-change review. Scope/execution delegated; pixel style explicitly chosen.

**Goal:** Make the playable overworld resemble the supplied Barovia geography.
**Architecture:** Optional palette-grid authored world, fixed POIs and explicit
road polylines applied during generation; existing arrays persist the result.
**Tech Stack:** Java/libGDX/TestNG, JSON, Python/Pillow preview, Maven.
**Spec:** [geography design](../specs/2026-11-03-strahd-geography-design.md).

## Global constraints

- 300×180 world; eight existing POIs/quests unchanged; content 0.3.0/new run.
- No image tracing/import, new gameplay, changes to frozen 0.2.0 package,
  profiles, unrelated theme files, commits or visible launches.
- Heavy jobs through shared runner, capped JVMs, hidden isolated native fixture.

## Review focus

- Top-down JSON and bottom-up world coordinates agree after save/Continue.
- Explicit roads clear blocked terrain without making entire lakes passable.
- Invalid files fail clearly rather than endlessly retrying fixed placement.
- All eight POIs generate once and remain connected with player-sized approaches.
- Campaigns without authored data retain procedural generation and save behavior.

### Task 1: Optional authored map and rendering regressions

Files: new `data/AuthoredWorldMapData.java`, `data/WorldData.java`, `world/World.java`,
`world/BiomeSprites.java`, new `test/forge/adventure/world/AuthoredWorldMapTest.java`.
Interface: `validate(int,int,int)`, `paint(long[][],int[][])`, `paintRoads(long[][],int[][],int)`,
`location(String)` on authored data, and nullable `WorldData.authoredMap` resource.

- [x] Write/observe failing tests for grid/anchor validation, road bridges,
  deterministic painting, negative sprite seeds and collision-vs-structure bits.
- [x] Implement schema/validation and narrow World generation hooks, retain the
  default path, fix the two rendering predicates, then verify green.

### Task 2: Authored Barovia landscape

Files: `tools/adventure/create_strahd_overworld.py`, matching Python tests,
`CurseOfStrahd/world/{barovia-map,barovia-*,world}.json`, campaign version tests.
Interface: deterministic raster, six biome palettes, named anchors and connected
road polylines; `--check` compares output, `--preview` renders existing pixels.

- [x] Observe absent-geography failures, author mountains/lakes/rivers/forests,
  place eight POIs and wind roads through clear crossings/passes.
- [x] Verify raster geography, reachability/road widths, references and stable
  generation; inspect and refine pixel preview. Update version to 0.3.0.

### Task 3: Native validation and portable candidate

Files: `StrahdChapterSmoke.java`, playtest/validation docs; new candidate in dist.
Interface: actual generated terrain, fixed positions, minimap export and exact
geometry after fresh-process Continue, with existing chapter journey checks.

- [x] Build/run affected tests; native hidden new world + both chapters + fresh
  Continue and Bellwake switch, export/inspect actual minimap.
- [x] Fresh review, repair important findings, verify scope/current bytes.
- [ ] Package new 0.3.0 build, verify archive and provide exact manual route guide.
