# Strahd Vallaki Chapter Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans inline, with one fresh
> whole-change review after verification. David delegated scope and execution.

**Goal:** Ship a new-run 0.2.0 chapter following Tser Pool through sanctuary in Vallaki.

**Architecture:** Extend campaign data, namespaced maps and dialogue embedding;
retain the existing Java engine and exact-version save compatibility checks.

**Tech Stack:** JSON/TMX, Python authoring tools, Java/TestNG, libGDX and Maven.

**Spec:** [2026-11-03-strahd-vallaki-design.md](../specs/2026-11-03-strahd-vallaki-design.md).

## Global Constraints

- Content 0.2.0, ID `fork.curse-of-strahd`; new runs required, no migration.
- Quest 8102, explicit Eva handoff, ten ordered flags exactly as in the spec.
- No unrelated theme/Bellwake changes, commits, merges, existing-profile writes,
  copied published prose/assets, new engine features or visible game launches.
- Heavy verification uses the shared runner; JVM at most 1 GB/two processors.

## Review Focus

- Early/out-of-order visits cannot bypass prerequisites or start locked fights.
- Saving after a victory or confession must allow resuming without removed actors.
- Once-only cards and quest issuance cannot repeat on revisit or checkpoint load.
- Eight POIs must actually generate, have reachable interactions and valid cards.
- Old exact-version saves stay incompatible; new Continue retains interior/state.

### Task 1: Campaign progression and encounters

**Files:** `forge-gui/res/adventure/CurseOfStrahd/{campaign.json,config.json,world/*.json,story/dialogues.json,decks/*.dck}`;
`forge-gui-mobile/test/forge/adventure/data/{StrahdChapterContentTest,StrahdVallakiContentTest}.java`.

**Interfaces:** Produces quest 8102, spec flags, new actor keys and three enemy names
`Western Road Ambush`, `Coffin Shop Spawn`, `Belfry Stalker` for maps/native checks.

- [x] Write failing data tests for version, Eva handoff, ten-stage quest, every
  locked interaction, both truth/Doru outcomes, interrupted relic collection,
  reward atomicity/repeats and completion gating; run and confirm absent-content failures.
- [x] Author chapter content and three fixed 40-card decks, gate every interaction,
  update end guidance and completion flag, adapt opening regression expectations.
- [x] Run chapter suites to green; validate compact HUD text and valid JSON.

Review correction: ordinary TMX `issueQuest` actions had no default recipient.
`MapDialog.issueQuest(String, Consumer<String>)` routes to the standard issuer
only when no existing quest-offer listener owns the event. Tests cover both
paths; the native fixture verifies Eva actually adds exactly one quest. Fallback
roots are mutually exclusive because runtime activates every matching root;
the data oracle now rejects overlapping roots instead of returning the first.

### Task 2: Four new reachable maps

**Files:** `tools/adventure/create_strahd_maps.py`, new `common/maps/map/strahd/{west-road,vallaki,saint-andral,coffin-shop}.tmx`,
`tools/adventure/test_strahd_vallaki_maps.py`.

**Interfaces:** Consumes dialogue keys/enemies; produces eight reproducible maps
with embedded JSON compatible with existing MapDialog and entry 38.

- [x] Write absent-map tests pinning four POIs, matching actor keys and unchanged
  opening geometry; run red.
- [x] Extend map generator and compose four distinct scenes; generate only new
  geometry, embed full catalog, validate resources/collision/reachability.
- [x] Run all Python authoring/package tests and inspect map previews.

### Task 3: Native journey, candidate and handoff

**Files:** `forge-gui-mobile-dev/test/forge/app/StrahdChapterSmoke.java`,
`docs/Adventure/Curse-of-Strahd-{Playtests,Validation}.md`, new candidate under `dist`.

**Interfaces:** Consumes shipped content through actual engine; proves loaded
cards, POIs, real dialogue effects, saved flags/collection/interior and Continue.

- [x] Extend assisted fixture through both truth choices after the existing two
  Doru branches; add early visits, ordinary loss/rematch, post-victory checkpoint,
  collection count/deck preservation and old-version discovery guard checks.
- [x] Build current reactor and run hidden capped native W/B journeys and fresh
  Continue; inspect logs/screenshots and repair failures within scope.
- [x] Fresh review the complete scoped diff; repair important findings and verify.
- [ ] Package a fresh Windows candidate, verify archive/current-byte provenance,
  provide exact launch path, new-run requirement and numbered manual checklist.
