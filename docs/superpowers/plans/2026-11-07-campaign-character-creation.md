# Campaign Character Creation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Implement the approved campaign-configurable creator while preserving existing saves and starter-deck compatibility.

**Architecture:** Add an optional campaign creation schema, a texture-free selection catalog, and an additive saved identity. Rebuild NewGameScene's presentation with existing Scene2D controls, keeping legacy deck policies available and reusing RewardActor for actual card images.

**Tech Stack:** Java 17-compatible source, LibGDX/Scene2D, TestNG, Maven, existing campaign JSON and player SaveFileData.

**Spec:** docs/superpowers/specs/2026-11-07-campaign-character-creation-design.md

## Global Constraints

- Preserve old saves and existing generateNewWorld/create callers; add identity fields rather than replacing old fields.
- Current checkout and feature branch; escalate blocked Git writes, never change Windows permissions.
- No changes to shared/default starter deck names or card lists.
- No real profiles or frozen distributions in tests; hidden/unfocused native checks only.
- Imports: PNG/JPEG, 10 MiB maximum, 4096 pixels per dimension, center-square 512-pixel save image.
- Campaign and common resource fallback remains compatible; no new dependencies.

## Review Focus

- Campaign switches with sparse/inherited options retain valid selections and do not activate a campaign during preview.
- Old saves retain their exact portrait/sprite selection after loading and saving again.
- Both filters plus a race with no matching portraits recover without destroying imports or dividing by zero.
- Invalid images and released scenes cannot leak textures or replace a valid portrait.
- Random/legacy deck previews and difficulty changes start with the same deck the user confirmed.

### Task 1: Configuration and selection policy

**Files:** data/CharacterCreationData.java, character/CharacterCreationCatalog.java, data/ConfigData.java, data/HeroListData.java; matching TestNG tests under forge-gui-mobile/test/forge/adventure/character.

**Interfaces:** `CharacterCreationData` owns nested Gender/Race/Portrait/Appearance/StarterClass option records; `CharacterCreationCatalog` supplies inherited/curated choices and `matchingPortraits(raceId, genderId, matchRace, matchGender)` with stable IDs. HeroListData exposes legacy metadata and portrait entries without changing old getHero/getAvatar behavior.

- [x] Add failing tests for optional schema, independent campaign catalogs, intersection/unspecified filters, invalid/duplicate IDs and empty collections.
- [x] Run targeted tests and capture missing-feature evidence.
- [x] Implement schema and catalog, preserving inherited options and legacy indices.
- [x] Rerun targeted tests; retain evidence and checkpoint in the ledger.

### Task 2: Identity persistence and imported portraits

**Files:** character/CharacterIdentity.java, character/PortraitImages.java, player/AdventurePlayer.java, world/WorldSave.java, scene/PlayerStatisticScene.java; tests for identity round trips and import headers/limits.

**Interfaces:** `CharacterIdentity.toSaveData()/fromSaveData(SaveFileData)` contains explicit IDs/labels, appearance sprite and portrait descriptor/PNG bytes. `PortraitImages` imports/loads owned textures. AdventurePlayer exposes independent identity and retains null-identity legacy paths. WorldSave adds an overload accepting identity and an optional already-previewed Deck while preserving the old overload.

- [x] Add failing round-trip, detached image bytes, missing identity and invalid image tests.
- [x] Implement additive save/load/create handling and owned portrait resource retirement.
- [x] Add header/dimension validation, centered import normalization and smooth portrait textures.
- [x] Run targeted tests and checkpoint evidence.

### Task 3: Creator and class/deck window

**Files:** scene/NewGameScene.java, character/CharacterImageSelector.java as needed, common UI resource JSON; Barovia characterCreation configuration and painted atlas resource.

**Interfaces:** Main UI captures an immutable CharacterIdentity plus selected difficulty/mode/edition/deck before campaign activation. Class/deck window previews and starts with the same Deck; fixed RewardActor bounds, explicit cancel/confirm, no duplicate deck title. Existing scene entry and new-playthrough transaction remain intact.

Review-driven supporting files: `character/CharacterOptionSelector.java` bounds creator controls to their rows; `character/StarterDeckPreview.java`, `util/AdventureOverrides.java`, `data/RewardData.java`, `world/World.java`, `util/Config.java` and `forge-core/.../MyRandom.java` isolate preview policy/randomness without activating a campaign; `UIScene.java` exposes non-text dialog navigation. Regressions cover real stage key events, difficulty retention/reconfirmation, unavailable confirmation recovery and outgoing context invariance.

- [x] Add native smoke assertions for campaign title, arrows, independent sprite/portrait, checkbox defaults, no-match/import preservation and fixed card preview bounds.
- [x] Implement the approved responsive layout using the existing skin, selectors and menu navigation, both orientations.
- [x] Implement campaign class choices and inherited mode/edition/deck window using actual card rendering.
- [x] Add Barovia portraits and class metadata referencing its current campaign-owned deck paths; do not edit deck files.
- [x] Run targeted and affected reactor tests; repair failures and checkpoint evidence.

### Task 4: Compatibility and verification

**Files:** forge-gui-mobile-dev/test/forge/app/CharacterCreationSmoke.java, authoring docs, this plan/ledger.

- [x] Run a hidden native creator scenario with disposable profiles and image fixtures, including a previously saved identity-free character and a new imported portrait save/Continue.
- [x] Check inherited campaign preview, class/deck confirmation, and both screen orientations; preserve source/profile hashes where applicable.
- [x] Run affected Maven reactor verification and existing Barovia authoring/navigation checks through the shared heavy-job runner.
- [x] Request one fresh whole-change review, address material findings, rerun affected checks.
- [x] Document campaign JSON examples, exact checks/results and any pending physical playtest coverage. Leave changes reviewable on the feature branch; no merge/push is part of this request.
