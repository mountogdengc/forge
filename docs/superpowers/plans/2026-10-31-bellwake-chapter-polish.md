# Bellwake Chapter Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Bellwake as a complete, polished first chapter with curated decks, purposeful exploration, a meaningful deck improvement, two intentional endings, and saving throughout exploration.

**Architecture:** Retain Forge's Java rules/AI, libGDX map runtime, campaign isolation, and immutable save repository. Extend capture/restoration to interior exploration, then implement most chapter changes as campaign content. Separate save infrastructure from chapter content so either can be reviewed independently.

**Tech Stack:** Existing Java 17 source target, Maven, libGDX 1.14.2, TestNG, Tiled TMX, Forge JSON and deck formats. No new runtime dependency.

**Spec:** [Bellwake baseline](../../Adventure/Bellwake.md), [campaign/save contract](../../Adventure/Campaigns-and-Saves.md), [fork direction](../../Adventure/Fork-Roadmap.md), and the proposed chapter brief below. This plan records proposed defaults; it does not claim new behavior is implemented or tested.

## Proposed chapter brief

- Keep the title, ferry mystery, Mira/Ada relationship, Keeper, and both existing ending concepts. Keep campaign ID `fork.bellwake`, quest ID `7001`, and existing flags `bellwakeFareReleased` and `bellwakeEnding`.
- Target a 30–45 minute first visit for a player familiar with Magic; measure this in playtests rather than treating it as established duration.
- Sequence: arrive at the quay → speak to Mira → optional preparation duel and deck improvement → short marsh journey with an optional discovery → investigate the tollhouse → defeat the Keeper and take the ticket → speak to Mira and choose an ending → explore the changed quay.
- Keep two principal locations. Rework their layouts and interactions; do not add a third dungeon or large world.
- Offer two curated 40-card starters: white creatures/protection and red creatures/direct damage. Offer one chapter difficulty initially: 20 starting life, no gold or maximum-life loss on defeat, and free recovery at the quay. Tune opponents through playtests; this is a deliberate first-chapter default, not a global rebalance.
- Preparation duel is optional, repeatable for practice, and offers its card reward once. Boss victory must not depend on completing it. No required roaming encounter or grinding.
- Use the existing two ending cards as story keepsakes. Add the useful deck-building reward before the boss; do not rely on an ending keepsake to demonstrate progression.
- Manual save, quicksave, load, and Continue work in stable exploration on the overworld and inside locations. Restore the location and gameplay state, without rerunning entry rewards or conversations. Loading is allowed from the same stable states.
- Do not save an unfinished Magic match, open dialogue/choice, reward selection, or scene transition in this iteration. Explain the brief restriction in the UI. Automatic checkpoint requests during those states wait until the action is settled; they do not capture half of it.
- Keep deck building, inventory, map, journal, settings, and run management. Defer shops/crafting/arenas/New Game+ unless needed by this chapter. Bellwake continues to disable New Game+.

## Global Constraints

- Preserve Forge rules, card data, AI, campaign/run isolation, and existing legacy campaign behavior.
- Existing saves are not overwritten or silently reinterpreted under changed content.
- Ship polished Bellwake as content version `0.2.0`; `0.1.0` saves require the old package. Only one installed version per campaign ID is currently supported: document package swapping, not side-by-side compatibility. No migration is promised.
- Do not change stable TMX object IDs of retained persisted actors casually. Allocate new IDs for new actors and document removed IDs.
- New save readers accept existing version-1 overworld snapshots. Newly captured snapshots use envelope version 2 so older clients reject them rather than load an incorrect overworld state. Unmodified imports of old payloads retain envelope version 1. Keep playthrough metadata version 1; split the repository's shared version constant accordingly.
- Build the Adventure client through `forge-gui-mobile-dev`, not the separate Swing client.
- No engine migration, broad framework rewrite, multiplayer work, or upstream submission.
- Code/data checks cannot establish pacing, balance, graphical quality, or native Windows acceptance.

## Review Focus

1. Loading an interior must not emit normal first-entry events, heal the player, replay dialogue, or regrant rewards (Tasks 2–3).
2. A failed interior/cross-campaign load must restore the outgoing interior and its state, not only its overworld coordinates (Task 2).
3. A delayed autosave must remain in its original run and cannot displace a newer Continue target (Task 3; retain repository regressions).
4. Chapter progress must survive unusual ordering, repeat conversations, loss/rematch, and reload immediately after a reward (Task 4).
5. Optional preparation and alternate starters must not produce a mandatory grind or unwinnable recovery loop (Tasks 5 and 7).

## File ownership and execution order

Save model/capture: `forge-gui-mobile/src/forge/adventure/world/ExplorationSnapshot.java` (new), `WorldSave.java`, `PlaythroughRepository.java`, `SaveSnapshot.java`.

Map restoration: `forge-gui-mobile/src/forge/adventure/stage/MapStage.java`, `GameStage.java`, `WorldStage.java`, `scene/TileMapScene.java`; actor classes under `character/` only where their state cannot be captured through the stage.

Save controls: `scene/StartScene.java`, `SaveLoadScene.java`, `stage/GameHUD.java`; route load completion consistently instead of hard-coding `GameScene` at each caller.

Chapter content: `forge-gui/res/adventure/Bellwake/` and `forge-gui/res/adventure/common/maps/map/bellwake/{quay,toll}.tmx`. Keep these namespaced common-map locations until the root-map resolver is deliberately changed; a resolver rewrite is outside scope.

Chapter presentation: campaign-specific `ui/` overrides where supported, `scene/QuestLogScene.java`, `stage/GameHUD.java`, and map music properties. Shared Java changes remain campaign-neutral.

Validation: new focused tests next to existing suites under `forge-gui-mobile/test/forge/adventure/`, plus `docs/Adventure/Bellwake.md` and `Progression-Validation.md`.

Execute Tasks 1–3 in order, then 4–6, then 7. Do not ship interior saving after Task 1 alone.

## Execution status (October 31, 2026)

Approved for implementation. Snapshot model/versioning is committed; integration,
authored content, stable interior controls, diagnostics and native regression
fixtures are implemented on `codex/bellwake-chapter-polish`. See
[the validation record](../../Adventure/Progression-Validation.md) for evidence.
The original task checklists below remain the acceptance specification; unchecked
multi-part tasks are not a claim that no code exists.

Automated/native checks and a local candidate package are being prepared.
Acceptance remains open for played matches and both starters, deliberate
loss/rematch, physical controller navigation, measured duration, complete display
readability, and the stronger quay art/ambient pass. Supplied victories do not
satisfy those gates. This is a candidate, not a fully accepted polished release.

## Task 1: Define a versioned exploration snapshot

**Deliverable:** A serializable, detached representation of where exploration resumes; existing saves remain readable.

**Files:** New `world/ExplorationSnapshot.java`; modify `world/PlaythroughRepository.java`, `SaveSnapshot.java`; new tests `world/ExplorationSnapshotTest.java`; extend `world/PlaythroughRepositoryTest.java`.

**Interfaces (new):** `ExplorationSnapshot.toSaveData(): SaveFileData`; `ExplorationSnapshot.fromSaveData(SaveFileData): ExplorationSnapshot`. Representation includes version, overworld/interior discriminator, root POI ID, current/source map resource identities, return/entry context, player position/facing, live mobile-actor state keyed by TMX object ID, and random state needed by resumed exploration. Keep graphics, Box2D bodies, scene instances, listeners, and timers out of serialization. Existing player/world/POI payload sections continue to own quests, collection, deleted objects, flags, shops, and world state.

- [x] Add failing round-trip tests for overworld, quay, tollhouse, and a connected-floor fixture. Assert actor IDs/positions and return context survive; malformed version, non-finite coordinates, invalid map identity, and absent interior POI fail explicitly.
- [x] Add repository tests: accept old envelope 1 and new envelope 2; new captures use 2; unmodified legacy imports use 1; run metadata stays 1; reject future envelope versions. Verify snapshot validation agrees with payload requirements and import remains compatible with old overworld payloads. Make the capture/import writer specify its envelope version explicitly rather than infer compatibility from missing fields.
- [x] Implement the snapshot codec and split metadata/snapshot version checks. Missing exploration data means overworld only for old-format saves; malformed new-format data never silently becomes overworld.
- [x] Run focused world tests, confirm new cases fail before implementation and pass afterward, and commit this isolated model/version change.

## Task 2: Capture and restore interior exploration safely

**Deliverable:** Save and restore a paused location without replaying gameplay events; rollback restores an outgoing interior too.

**Files:** `WorldSave.java`, `stage/{MapStage,GameStage,WorldStage}.java`, `scene/TileMapScene.java`, `world/ExplorationSnapshot.java`; new `world/InteriorRestorationTest.java` and `world/InteriorRestorationSmoke.java` graphical harness.

**Interfaces (new):** `WorldSave.captureExploration(): ExplorationSnapshot`; `WorldSave.restoreExploration(ExplorationSnapshot): void` (throws `IOException` on unsupported/missing location state); `WorldSave.showRestoredExploration(): void`. Add a restore-specific map construction path in `TileMapScene`, separate from its existing normal-entry `load(PointOfInterest)` path. Normal entry keeps existing behavior.

- [ ] Add failing codec/integration cases covering player position, facing, enemy position/movement behavior, removed enemies, local flags, return exit, connected floors, and resource failure. Use a native harness for cases that require libGDX graphics/physics rather than asserting them with mocks.
- [ ] Extend `captureLiveData()` and async snapshot assembly with the same exploration representation. Capture detached state on the game thread; encode/write in the worker. Freeze simulation while a manual snapshot is captured. Resume movement safely with no remembered held input.
- [ ] Restore in order: campaign resources → world/player/POI changes → location assets/actors → snapshot positions and behavior state → HUD/listeners → visible scene. Suppress entry events, automatic town healing, contact-triggered transitions, and introductory dialogue during restore. Clear trigger suppression after leaving the overlapping trigger, so normal re-entry works.
- [ ] Rebuild native resources from content and snapshot values; do not attempt to serialize native resource objects. Restore gameplay state, not exact particles or animation frames. For any actor state the model omitted, extend the codec and round-trip assertions before proceeding.
- [ ] Use the same complete detached capture for load rollback. Add failure injection after target campaign activation and after map construction: assert outgoing interior, quests, inventory, run identity, and Continue remain correct. Missing map/POI errors leave saved snapshots intact.
- [ ] Add a graphical restore fixture where entering normally heals/sets a flag/grants a reward; restoring does none of them. Verify an ordinary later entry still does.
- [ ] Run focused tests and graphical harness, record fixture results, and commit. Keep public interior save controls gated until Task 3 is complete.

## Task 3: Enable saving/loading throughout stable exploration

**Deliverable:** Manual/quick/automatic checkpoints and Continue use interior snapshots consistently, with clear feedback.

**Files:** `WorldSave.java`, `scene/{StartScene,SaveLoadScene}.java`, `stage/{GameHUD,MapStage,WorldStage}.java`, `scene/{TileMapScene,RewardScene}.java`, `util/MapDialog.java`; new `world/ExplorationSavePolicyTest.java`.

**Interfaces (new):** `WorldSave.explorationSaveBlockReason(): String` (empty means allowed); `WorldSave.requestExplorationCheckpoint(String reason): void` (coalesces requests until the next stable boundary). Existing save/load public methods remain. Every Continue/load/quickload completion calls Task 2's `showRestoredExploration()`.

- [ ] Add failing tests: stable overworld/interior allowed; battle, transition, dialogue, reward selection, and incomplete restoration blocked with an actionable reason. A deferred request captures state after choice/reward completion, never before it. Cancelling the application does not display an uncommitted checkpoint as saved.
- [ ] Replace interior-only guards in save, async capture, load, and checkpoint/suspend with the shared stable-state policy. Audit all load/Continue/quickload and save-return callers for forced overworld scene changes. Save while in a location must return to that location.
- [ ] Pause the simulation while the save menu is open. Show save-in-progress and committed/failed feedback; display the actual location in the run browser. Defer automatic requests at unstable boundaries and discard requests belonging to a retired run.
- [ ] Checkpoint after completed preparation reward, ticket acquisition, ending reward, and defeat/recovery, plus existing travel checkpoints. An ending flag, its reward, and removal of the old interaction must be captured together after the whole interaction settles. Do not allow opening a save/load menu between those effects.
- [ ] Retain and run async ordering/run isolation tests. Add interior A → campaign B → interior A and failed-load rollback cases through the UI; test replacement of a named manual save and quicksave/quickload inside both Bellwake locations.
- [ ] Run affected tests and graphical scenarios; update the campaign/save contract to describe implemented behavior and remaining mid-action restrictions; commit.

## Task 4: Make Bellwake an intentional, recoverable story

**Deliverable:** A small authored route, an interactive investigation, and an ending initiated by speaking to Mira.

**Files:** `Bellwake/world/{quests,points_of_interest,enemies,bellwake-marsh,world}.json`, Bellwake TMX maps; new `test/forge/adventure/data/BellwakeContentTest.java`. Modify `MapDialog.java` only if existing actions cannot express the required state transitions.

**State contract:** Existing quest flag `bellwakeFareReleased=1` means ticket acquired; existing character flag `bellwakeEnding=1/2` means committed outcome. Add quest flags `bellwakeBriefed=1`, `bellwakeClueRead=1`, `bellwakePracticeRewarded=1`, and `bellwakeResolved=1`. No ending value before the ending choice. Story stage 1 becomes speaking to Mira (`QuestFlag`, `bellwakeBriefed`), stage 2 ticket acquisition (prerequisite 1), stage 3 ending committed (`QuestFlag`, `bellwakeResolved`, prerequisite 2). Do not check the character flag through a quest-flag objective. Set the character ending flag, grant its reward, and set the resolved quest flag within one non-interruptible interaction before checkpointing. Reading the clue is optional and never blocks the Keeper. Normal exploration/re-entry dialogue handles the ending; remove the travel-triggered ending epilogue.

- [ ] Add failing libGDX-parser/content tests for the three-stage prerequisite chain, required dialogue choices, flag names, enemy/deck/map references, and both ending reward definitions. Assert the quest cannot finish merely by entering the quay.
- [ ] Make Mira deliver the opening request on interaction; use the initial prologue only for a brief scene-setting line. Add pre-ticket, ready-to-resolve, and ending-specific dialogue conditions. Never show incompatible branches together.
- [ ] Add a readable tollhouse clue about Ada's repeated fare, a clear approach to the Keeper, and a visible way back. Preserve the ticket-taking step following victory. Retain the two ending concepts and their existing reward cards.
- [ ] Rework quay/tollhouse entrances, collision, and signage; make the south exit and eastward route readable without opening documentation. Keep the compact marsh and suppress required random combat; optional roaming rats must not block the route.
- [ ] Add sequence coverage: tollhouse before briefing, return without ticket, clue skipped, loss/rematch, victory then reload, each ending then repeated interaction/re-entry/reload. Rewards and boss removal persist once per run; loading an older snapshot intentionally restores that older state.
- [ ] Run focused content tests and a full graphical story walkthrough; commit with content version still marked development until packaging Task 7.

## Task 5: Curate the starter, preparation, reward, and recovery loop

**Deliverable:** Both starters can finish without grinding; players receive and can use a meaningful early upgrade.

**Files:** New `Bellwake/config.json`, `decks/{quay-white,quay-red,quay-practice}.dck`; modify `decks/last-fare.dck`, `world/enemies.json`, quay map; extend `BellwakeContentTest.java`. Reuse the existing Constructed starter configuration; only populate that mode and the two color choices. Copy required shared configuration explicitly because config fallback is file-based, not a promised field merge.

- [ ] Add failing resource tests: exactly two offered 40-card starts, supported card names, functional land/spell composition, only the intended mode, one difficulty, 20 life, zero defeat gold/max-life penalty, no unwanted inherited starter items, and valid practice/boss decks.
- [ ] Author the two starters and a simple practice opponent. Add optional practice at the quay using existing battle actor/dialogue mechanisms. Practice awards a one-time choice: two copies of Pacifism or two copies of Lightning Strike, with card inspection and a clear color/use explanation. Set the reward guard after the selected reward is granted; repeated practice grants nothing. The player may decline practice and proceed.
- [ ] Provide free recovery by a clearly labeled quay interaction; make defeat return the player to a safe, recoverable state with a plain explanation. Confirm the inherited town auto-heal behavior and avoid contradictory UI. No story flag advances on loss, and the Keeper stays available.
- [ ] Rework the Keeper deck only as playtesting warrants; preserve the fare/death theme and readable threats. Exclude arbitrary instant-win spikes or infinite loops from these authored opponents.
- [ ] Add a contextual prompt after receiving the preparation reward: inspect it and open the existing deck editor. Preserve free choice about changing the deck; do not automatically alter it.
- [ ] Verify one-time practice rewards, no penalties, correct healing, deck/collection persistence, and practice-skipped boss completion for both starters. Record match lengths, losses, and confusion; tune content rather than alter the general AI. Commit.

## Task 6: Polish orientation, presentation, and content diagnostics

**Deliverable:** The player understands the next action, can read the UI, and can recognize chapter completion.

**Files:** Bellwake campaign `ui/` overrides and maps; shared `QuestLogScene.java`, `GameHUD.java` only where necessary; `docs/Adventure/Bellwake.md`; extend `BellwakeContentTest.java` and existing `AdventureProgressionContentTest.java` helpers where reusable.

- [ ] Show one concrete tracked objective at a time: speak to Mira; find the tollhouse and recover the ticket; return and speak to Mira. Give completed steps readable treatment. After ending resolution, show a persistent chapter-complete message from the saved ending flag even though the existing controller removes completed quests from its active list.
- [ ] Keep menu access to deck, journal, map, inventory, saves, and settings. Ensure gamepad focus and keyboard/mouse navigation remain usable; avoid a broad shared skin rewrite. Preserve legacy campaign menus.
- [ ] Use a consistent palette and matching existing art for quay, marsh, and tollhouse; add restrained ambient/music cues through supported facilities. Check dialogue wrapping, choice labels, card inspection, transitions, and save feedback at 1280×720 and 1920×1080. Include portrait layout checks for any shared UI touched; native Windows is the first release gate.
- [ ] Add campaign content diagnostics using libGDX's accepted JSON syntax: duplicate/missing quest stages, prerequisites, unsatisfied references, empty choice labels, unknown enemies/decks/cards, missing map templates and target maps, and missing declared starting location. Report source resource and object/stage ID. Reuse checks; do not replace the editor or impose strict-JSON rules that reject valid Forge content.
- [ ] Exercise each diagnostic against a broken fixture and valid Bellwake; rerun existing progression fixtures so the validator does not accept errors merely because current content passes. Commit.

## Task 7: Package and accept the chapter

**Deliverable:** A reproducible Windows test build with an evidence-backed chapter acceptance record.

**Files:** `Bellwake/campaign.json` (version `0.2.0`), `docs/Adventure/{Bellwake,Progression-Validation,Campaigns-and-Saves}.md`; reuse `tools/adventure/package_windows_test.py` and its documented invocation in `docs/Adventure/Windows-Test-Build.md`.

- [ ] Run new red/green tests as each code task is developed. At integration, run `mvn -B -ntp -pl forge-gui-mobile -am test`, then `mvn -U -B -ntp clean test` once on the final candidate. Required result: build success, no new failures, and any skips reported explicitly. Supply a display for tests requiring graphics; do not turn unavailable graphics into a pass.
- [ ] Run native Windows, fresh-profile playthroughs with each starter. Cover both endings across those runs; complete actual practice and Keeper matches without assisted victory. Repeat with practice skipped, a deliberate Keeper loss/rematch, and repeated conversations.
- [ ] Save/quit/Continue in quay, marsh, tollhouse before victory, after ticket acquisition, and after each ending. Verify player location, actor state, life, deck, collection, quest state, no repeated rewards, no resurrected Keeper, and correct return exits. Add connected-floor, missing-map, old-overworld-save, and cross-campaign rollback regression fixtures separately from Bellwake's two root maps.
- [ ] Inject a write failure and a failed map restoration using disposable profiles. Verify earlier saves remain loadable, errors are readable, active state is recovered, and Continue is not advanced on a failed checkpoint.
- [ ] Record exact commit, resource version, OS/Java/build tools, build artifact, starter choice, test results, screenshots, observed duration, and unresolved defects. Do not replace the existing assisted-smoke record with claims of unassisted success.
- [ ] Publish the candidate locally through the existing Windows test packaging workflow. Label it `0.2.0` and explain exact content-version matching and that old `0.1.0` runs are not migrated. Update docs from proposed behavior only when verified. Commit the chapter/version/docs together.

## Definition of done and handoff

The chapter is complete when both curated starts support an unassisted finish, optional preparation teaches a useful deck improvement, exploration and objectives are readable, both endings have persistent consequences, losses allow recovery, and location saves restore correctly after a fresh launch. All automated gates and native Windows acceptance must have recorded results; remaining limitations must be explicit.

The original plan selected two starter archetypes, a single forgiving difficulty, optional practice, and stable-exploration saving. The user approved implementation. Current execution and remaining acceptance are recorded above and in the validation guide.
