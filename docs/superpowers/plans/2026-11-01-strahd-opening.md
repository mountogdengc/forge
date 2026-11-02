# Strahd Opening and Desktop Defaults Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans; independent defaults,
> content and geometry units use superpowers:dispatching-parallel-agents. Observe
> RED/GREEN checks and request a fresh whole-change review before delivery.

**Goal:** Adopt the user's tested desktop defaults and build a selectable,
complete opening chapter for a Curse of Strahd card-adventure campaign.

**Architecture:** Reuse existing campaign/resource roots, Magic duels, quest
flags, saved playthroughs and namespaced TMX maps. Author dialogue as JSON,
embed it into geometry at build time, and validate all resource/card references.

**Tech Stack:** Java 17, Maven/TestNG, libGDX/TMX, existing pixel tiles and Python
standard-library authoring/packaging tools.

**Spec:** [Curse-of-Strahd-Design.md](../../Adventure/Curse-of-Strahd-Design.md).
User selected the campaign and delegated the opening chapter; no further scope
approval is needed for the design's first playable slice.

## Global constraints

- New ID `fork.curse-of-strahd`, content 0.1.0, quest 8101; no Bellwake changes.
- Follow the map/actor/dialogue-key/flag contract in the design exactly.
- No new battle engine or physical follower system; escort is narrative state.
- No copied published prose, maps, artwork or stat blocks; authored adaptation.
- Use capped sequential builds through the shared runner; no visible game run
  without user approval. Hidden native probes must not capture input/focus.
- Do not touch normal profiles, delete saves, merge/push or publish.

## Review focus

- Early/out-of-order location visits and reloaded choices must reconcile flags
  without requiring an already removed enemy or duplicating rewards.
- Every encounter and character interaction must have a clear walkable route;
  root exits return to the correct overworld location.
- Camp completion is an explicit interaction with prerequisites, not arrival;
  both church outcomes remain loadable and visible in later dialogue.
- Campaign switching cannot leak actors, reward pools, journal state or saves.
- Desktop defaults preserve explicit resolution preferences and mobile behavior,
  and normal release launchers must not regress to uncapped rendering.

## Task 1: Tested desktop defaults

Files: DesktopGraphicsPolicy + test; SettingData/Config + resolution tests;
package_windows_test.py/package_bellwake_windows.py templates and metadata.

- [ ] Change normal-launcher test to require 30 FPS; observe RED then GREEN.
- [ ] Test fresh desktop 1920×1080, fresh mobile legacy 1280×720, explicit saved
  sizes retained; implement a small SettingsData default helper used by Config.
- [ ] Package normal launchers at 128-MB/1-GB heap, two reported processors,
  fresh 1920×1080/windowed profile; keep eager diagnostic flag functioning.
- [ ] Root updates the user's main CMD/alias to the proven capped launcher,
  preserving backups and original client/saves. No automatic launch.

## Task 2: Campaign definition and story data

Files: new CurseOfStrahd/campaign.json, config.json, world catalogs, fixed decks,
story/dialogues.json; StrahdChapterContentTest in the mobile test tree.

- [ ] Write missing-campaign/progression/once-only-choice tests, observe RED.
- [ ] Author all first-chapter definitions and dialogue keys from the contract.
- [ ] Verify both 40-card starts, enemy decks, whitelist printings, quest stages,
  explicit completion, both church outcomes, upgrades/reward-once behavior.

## Task 3: Original map compositions

Files: common/maps/map/strahd/{village,church,road,tser-pool}.tmx;
tools/adventure/create_strahd_maps.py or equivalent reproducible geometry tool.

- [ ] Compose four visually distinct maps using existing tile resources.
- [ ] Place fixed IDs and cosDialogKey properties; use `[]` dialogue placeholders
  until root embeds the finished story catalog. No concurrent content edits.
- [ ] Validate references, collidable boundaries, clear entries/exits and paths
  to every actor; retain preview/render evidence for visual inspection.

## Task 4: Integration and verification

Root owns the dialogue-embedding tool, actual card/reference validator, tests
across the finished TMX/data, hidden story/save/native probe and packaging.

- [ ] Embed all 13 keys; reject missing/unused keys and malformed dialogue.
- [ ] Validate actual lazy card names and resources; no full catalog at menu.
- [ ] Run affected Java/Python suites, Bellwake regression and defaults checks.
- [ ] Hidden native: both starts, church outcomes, premature camp visit,
  practice loss/reward once, road encounter, chapter completion, interior
  save/load/revisit/Continue and cross-campaign isolation. Supplied victories
  remain explicitly assisted; played wins/balance are manual.
- [ ] Fresh review of whole source/data changes; repair important findings.
- [ ] Package a complete Windows candidate with 1080p/30 FPS defaults, current
  client/runtime and both campaigns; CRC/hash/extraction and bundled runtime
  checks. Preserve existing artifacts and record source identities.
- [ ] Hand off exact launch path, numbered manual actions, expected outcomes,
  report fields and dependent next work. Do not claim the full published
  campaign, unassisted balance or long-session results that were not measured.
