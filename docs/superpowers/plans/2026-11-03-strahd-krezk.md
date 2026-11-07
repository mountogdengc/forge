# Krezk Quest Area Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans inline, followed by
> one fresh whole-change reviewer. David delegated design and execution.

**Goal:** Ship A Bell Against the Mists as a complete third chapter in Krezk.
**Architecture:** Data-only extension of the existing quest/dialogue/card system;
metadata drives four scenes and 37 fixed overworld anchors. Reuse unchanged engine.
**Tech Stack:** JSON/TMX, Python, Java/TestNG/native libGDX fixture, Maven.
**Spec:** [Krezk design](../specs/2026-11-03-strahd-krezk-design.md).

## Global constraints

- Version 0.5.0/new run, quest 8103, nine stage flags and acceptance/choice exactly
  as in the spec. Preserve old builds/profiles and unrelated theme changes.
- Four Krezk maps; 25 passive arrivals; 37 world POIs. No new engine features.
- No commits/publishing/visible launch; queue heavy checks, capped isolated JVMs.

## Review focus

- Early or inconsistent evidence must not overlap dialogue roots or bypass gates.
- Removed enemies must not strand the player after a post-victory save.
- Water/accord/reward require explicit separate actions, with atomic completion.
- Handoff and revisits must not issue duplicate quests or repeat rewards.
- Regeneration must keep quest maps separate from passive arrivals and retain
  original maps/dialogue except the deliberate sanctuary handoff.

### Task 1: Complete chapter data and journal

Files: new `tools/adventure/create_strahd_krezk.py`, `test_strahd_krezk.py`,
`StrahdKrezkContentTest.java`; campaign/config/story/quest/enemy/deck data;
existing content/version tests.
Interface: four scene keys `krezk-gate`, `krezk-village`, `krezk-pool`,
`krezk-abbey`; dialogue prefixes same; flags/decks/POIs from spec.

- [x] Add and run failing chapter tests for version/quest/handoff, nine stages,
  early gates, mutually exclusive roots, deliberate water/accord and once-only
  two-color rewards. Expected missing chapter failures, not syntax/import errors.
- [x] Implement `build_content()` to preserve earlier catalogs and author only
  Krezk keys/quest/enemies/decks, replace passive Krezk dialogue and add handoff.
- [x] Run data tests to green, adapt retained old content contract counts/version.

### Task 2: Four playable scenes and geography

Files: generator above, `create_strahd_maps.py`, `create_strahd_landmarks.py`,
`create_strahd_overworld.py`, metadata and four generated TMX files.
Interface: actor IDs gate60 captain/61wolves/62sign; village60Dmitri/61Anna/62notice;
pool60watcher/61water/62shrine; abbey60Abbot/61sentinel/62Vasilka/63bell; entry38.

- [x] Add absent-map/metadata/ownership tests; observe red.
- [x] Implement `compose()` using existing tiles; register 13 actors, three
  enemies, namespaced keys and quest-owned metadata. Passive generator skips
  questArea entries. Build only Krezk geometry; embed/validate all 90 actors.
- [x] Generate connected spurs/37 anchors, update counts, run all Python checks,
  compare unaffected TMX hashes and inspect scene/map previews.

### Task 3: Native regression, review and portable build

Files: `StrahdChapterSmoke.java`, smoke wrapper; design/playtest/validation docs;
new `dist/adventure-windows/strahd-0.5.0-krezk` candidate.
Interface: `-KrezkProbe` runs chapter-specific native flow with passive tour skipped,
both prior chapters, nine Krezk stages/choices/checkpoints and fresh Continue.

- [x] Extend native checks for real quest issuance, locked visits, losses,
  collection checkpoints, both accords and rewards once, cleanup and Continue.
- [x] Run affected Java/Python suites; verify unchanged engine byte-for-byte.
  Run hidden native fixture; inspect actual evidence/screenshots and repair bugs.
- [x] Fresh scoped whole-change review; fix important findings with regression
  checks, then package new 0.5.0 and verify archive/current-input hashes.
- [x] Deliver exact launcher/preview and manual route with prerequisites,
  expected outcomes, report fields and dependent balance/art work.
