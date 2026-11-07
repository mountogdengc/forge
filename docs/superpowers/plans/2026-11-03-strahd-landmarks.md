# Reference-Map Destinations Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans inline and one fresh
> whole-change review. The user chose short arrival scenes; no full quest areas.

**Goal:** Add 26 named arrival destinations including Ravenloft and Krezk.
**Architecture:** Authored landmark inventory drives world anchors/roads and
local TMX/dialogue generation. Existing runtime and save arrays are sufficient.
**Tech Stack:** JSON/TMX, Python/Pillow, Java/TestNG/native fixture, Maven.
**Spec:** [landmark design](../specs/2026-11-03-strahd-landmarks-design.md).

## Global constraints

- 34 POIs, 26 new local maps, 79 total dialogue actors; version 0.4.0/new run.
- Preserve all eight existing maps and both chapters; no new battles/rewards or
  progress effects. Existing pixel assets, clear exits, explicit future interiors.
- No commits, existing-profile/package writes or visible game/desktop automation.
- Reuse verified unchanged production client; queue all heavy jobs.

## Review focus

- All added locations generate and survive Continue with exact names/coordinates.
- Oversized markers and clearings must not obscure another entrance or drain lakes.
- Pre-quest visits and nested dialogue cannot issue quests, rewards or flags.
- Exits return to the matching overworld POI; Ravenloft/Krezk saves restore there.
- Catalog regeneration must retain existing authored chapter dialogue and geometry.

### Task 1: Landmark inventory and global placement

Files: new `story/landmarks.json`, `create_strahd_overworld.py`, campaign/version
tests, new `test_strahd_landmarks.py`, `StrahdLandmarkContentTest.java`.
Interface: name/slug/key/x/y/archetype/sprite/arrival/look/clearingRadius inventory.

- [x] Observe absent-location/content failures; author the 26 sites and original
  arrival prose, add connected road spurs and fixed anchors, version 0.4.0.
- [x] Verify exact roster, all reference letters represented, scene safety and
  3×3 world reachability; inspect labelled preview for crowding.

### Task 2: Explorable exteriors

Files: new `create_strahd_landmarks.py`, 26 common/maps/map/strahd TMX files,
existing scene-generator registration and story/dialogues.json additions.
Interface: entry 38; actors 60 arrival, 61 detail; two `landmark.<slug>.*` keys.

- [x] Generate distinct castle/gate/town/ruins/winery/mill/tower/den/temple/hill/
  forest/river/lake/mountain scenes, embed and validate every interaction/exit.
- [x] Inspect representative previews/contact sheet; run current Python/content
  checks, preserve original-map bytes, fix failures within scope.

### Task 3: Native and distribution

Files: StrahdChapterSmoke.java and playtest/validation docs, new dist candidate.
Interface: all-site no-effect tour + exits; Ravenloft/Krezk checkpoints; chapter
regression and final fresh-process Continue with exact authored roster.

- [x] Run affected suites and verify unchanged engine class bytes against JAR.
- [x] Native hidden all-site tour, checkpoints, chapters and Continue; inspect
  actual world map and representative local scenes; fresh review/important fixes.
- [x] Package fresh 0.4.0 build, verify manifests/current input hashes, deliver
  launcher, preview and numbered manual checklist.
