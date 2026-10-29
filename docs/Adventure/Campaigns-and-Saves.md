# Selectable campaigns and independent playthroughs

## Decision and status

Accepted product requirement, October 29, 2026: the player chooses a campaign whenever starting a new game and can keep multiple saved playthroughs. This includes several runs of the same campaign and runs of different campaigns at the same time.

The architecture below is the proposed implementation contract. This document records requirements and a source audit; it does not claim that the campaign picker, isolated playthrough storage, or campaign switching has been implemented or playtested.

A future adaptation of *Curse of Strahd* is an example of a distinct campaign this should support. It would supply its own adventure content while using the fork's card battles. The example does not select the first campaign to build. The inherited Forge Adventure is an optional campaign candidate, and may be retained, redesigned, or replaced as described in the [roadmap](Fork-Roadmap.md).

## Player experience

| Action | Required behavior |
| --- | --- |
| New Game | Show the installed campaigns first. The player chooses one, sees its description and supported starting options, creates a character, and names a new playthrough. |
| Start another game | Create a separate playthrough even when the campaign, character name, or chosen run name matches an existing one. Keep the earlier game and its recovery saves. |
| Load Game | Browse playthroughs across campaigns, then choose a saved point within the selected playthrough. Show campaign, character, run name, location, and save time so entries are distinguishable. |
| Continue | Resume the last played valid playthrough and resolve its campaign automatically. A late background save from another run must not change this selection. |
| Save Game | Create a named manual save or explicitly replace a selected save in the active playthrough. Several checkpoints of one run can coexist. |
| Autosave and quicksave | Keep their files and recovery history inside the active playthrough. They must not overwrite another run's saves. |
| Switch campaigns | Select a different campaign through New Game or Load Game without manually changing a global World setting or restarting the application. Reinitialize the internal campaign context as needed. |
| Missing campaign | Keep the save visible, explain which campaign/version is needed, and preserve its data. Do not load a different campaign or generate a replacement world. |

Use a browsable save list rather than a shared ten-slot limit. Storage and any deliberate retention policy still apply; automatic retention must not delete named manual saves. Multiple saves does not by itself promise saving in every interior location: exact location/event restoration is a separate capability to validate.

## Three separate identities

| Entity | Meaning | Identity and ownership |
| --- | --- | --- |
| Campaign | An installed adventure definition: world, story, quests, encounters, starting rules, and content. | Stable `campaignId` plus a content version. Its display title and installation folder may change without changing its identity. |
| Playthrough | One New Game instance of a campaign. | Unique `playthroughId`, selected campaign/version, character, run name, initial settings, world seed when applicable, and creation time. |
| Save snapshot | A recoverable point in one playthrough. | Unique `snapshotId`, campaign and playthrough identities, save-format version, save kind, timestamp, display label, and relevant preview metadata. |

Every playthrough owns its character progression, card collection and decks, inventory, quest states, story choices, defeated encounters, and world changes. Application preferences such as audio, display, controls, and accessibility can remain shared. The default is an independent start; transferring a character or collection between campaigns would be a separate explicit feature.

If a campaign offers New Game+, create a new playthrough ID with the campaign's declared carryover and reset rules. Preserve the source run and its snapshots; starting New Game+ must not repurpose that run's automatic-save destinations.

For example, two runs of the same campaign can each have an autosave, quicksave, and several named manual checkpoints. A third run in another campaign can coexist with both. Reusing a character name never joins those runs or changes their storage destinations.

The logical storage relationship is campaign, then playthrough, then snapshots. Use stable IDs for identity and storage keys; human-readable titles are labels. A save index can provide a combined Load Game view without requiring the player to know directory names. The exact disk layout remains an implementation choice.

## Campaign definitions

Introduce a discoverable campaign descriptor, with a versioned schema, that separates campaign identity from the current plane-folder convention. Two campaigns may use the same setting or reusable assets; one campaign may contain more than one region or plane.

At minimum, a descriptor needs:

- Stable ID, content version, title, description, and an optional preview image.
- Engine/save compatibility requirements and declared content dependencies.
- Resource roots and an explicit policy for shared assets or configuration inheritance.
- Initial location and starting quest IDs, including a valid no-starting-quest choice.
- Supported character, starting-deck, card-pool, difficulty, and reward settings.
- Any campaign initialization data, and declared New Game+ behavior if that feature is offered.

Campaigns must be able to provide their own opening and progression. Shared Java startup code must not automatically add the Shandalar main quest to every new campaign. The same rule applies to New Game+, recovery paths, and any alternative new-world entry point.

Support the selected content design, including authored locations and event sequences as well as procedural components where useful. A new campaign must be able to be a different adventure, rather than only a visual or deck-selection variant of the inherited story.

Shared art and interface assets can be reused through declared defaults. Missing required campaign story or world data should produce a useful validation error instead of silently substituting unrelated common content. Existing plane folders can receive compatibility descriptors during transition; that adapter must make their inherited behavior explicit.

## What the current code supports

Source review baseline: [`37216f201e57ad43e033345167cdfa066dbc438b`](https://github.com/mountogdengc/forge/commit/37216f201e57ad43e033345167cdfa066dbc438b). These are source observations, not results from an interactive game session.

| Finding | Source and implication |
| --- | --- |
| Multiple plane folders and user planes are discoverable. | [`Config.java`](https://github.com/mountogdengc/forge/blob/37216f201e57ad43e033345167cdfa066dbc438b/forge-gui-mobile/src/forge/adventure/util/Config.java) provides resource roots and shared-resource fallback. This is a starting point for campaign discovery, but not a campaign identity/version contract. |
| World selection is a global setting with a restart prompt. | [`SettingsScene.java`](https://github.com/mountogdengc/forge/blob/37216f201e57ad43e033345167cdfa066dbc438b/forge-gui-mobile/src/forge/adventure/scene/SettingsScene.java) changes `SettingData.plane`; `Config` captures the active plane and resource prefix when constructed. |
| New Game assumes the Shandalar opening quest. | [`NewGameScene.java`](https://github.com/mountogdengc/forge/blob/37216f201e57ad43e033345167cdfa066dbc438b/forge-gui-mobile/src/forge/adventure/scene/NewGameScene.java) adds quest `28`. The New Game+ path in `SaveLoadScene` also adds it. |
| There are ten manual slots plus an autosave and quicksave. | [`SaveLoadScene.java`](https://github.com/mountogdengc/forge/blob/37216f201e57ad43e033345167cdfa066dbc438b/forge-gui-mobile/src/forge/adventure/scene/SaveLoadScene.java) creates manual slots 1 through 10 and lists files for the selected plane. It also restricts saving inside maps. |
| Save destinations are derived from the active plane, with no run identity. | [`WorldSave.java`](https://github.com/mountogdengc/forge/blob/37216f201e57ad43e033345167cdfa066dbc438b/forge-gui-mobile/src/forge/adventure/world/WorldSave.java) uses plane-relative paths and shared `auto_save.sav` and `quick_save.sav` filenames. New runs of one plane therefore share automatic-save destinations. |
| The current header does not identify a campaign or playthrough. | [`WorldSaveHeader.java`](https://github.com/mountogdengc/forge/blob/37216f201e57ad43e033345167cdfa066dbc438b/forge-gui-mobile/src/forge/adventure/world/WorldSaveHeader.java) explicitly serializes name, preview, and date. Adding Java fields alone would not add them to that serialized layout. |
| Continue records a filename and uses the current plane. | [`SettingData.java`](https://github.com/mountogdengc/forge/blob/37216f201e57ad43e033345167cdfa066dbc438b/forge-gui-mobile/src/forge/adventure/data/SettingData.java) and [`StartScene.java`](https://github.com/mountogdengc/forge/blob/37216f201e57ad43e033345167cdfa066dbc438b/forge-gui-mobile/src/forge/adventure/scene/StartScene.java) need a structured campaign/playthrough/snapshot locator. |

## Campaign activation and loading

Campaign selection must be resolved before building character-creation options or restoring a world. Introduce an explicit active campaign/playthrough context and a coordinated transition rather than changing only `SettingData.plane`.

Campaign-dependent configuration, resource lookups, quest definitions, hero/avatar data, enemy and shop catalogs, reward pools, localization, and scene instances must be rebuilt or appropriately scoped. The source review found process-wide caches in `Config`, `HeroListData`, `WorldData`, `RewardData`, and `NewGameScene`; inventory the full lifecycle before implementing in-session switching.

Load in this order:

1. Read lightweight snapshot metadata and identify its campaign, version, playthrough, and format without creating a world.
2. Resolve the installed package and check compatibility. A missing or incompatible package leaves the snapshot intact and produces an actionable error.
3. Finish or isolate writes from the previous session and prepare the target campaign's resources.
4. Restore into a staged context and make it active only after restoration succeeds. A failed load must not regenerate a substitute world or overwrite the existing save.
5. Update Continue using campaign, playthrough, and snapshot IDs. Track the last played run separately from the completion time of a background write.

A metadata envelope or index should support listing snapshots without eagerly constructing every native preview image. Maintain a reader for the existing custom save header; design a versioned transition rather than silently changing its binary layout.

## Save integrity and version policy

Capture the immutable campaign/playthrough identity, snapshot ID, destination, and session token when a save is queued. The existing asynchronous path resolves its file path from global configuration inside the worker; that must be changed before campaign switching is enabled. A late completion updates its own run's metadata and must not redirect the active session's Continue pointer.

Use one reliable write strategy for manual, quick, and automatic saves: write and validate a temporary snapshot, commit the completed file, retain an appropriate recovery copy, and update metadata only after success. Report failures accurately. The existing synchronous manual path and asynchronous path differ, so review both rather than assuming one path's guarantees cover the other.

Snapshots record the campaign version and save-format version they were created with. Load only a compatible installed version. Content updates need an explicit compatibility rule or migration; do not silently reinterpret old quest IDs or map identities under a different package. A campaign display-name change must not break discovery of its saves.

For legacy imports, retain the original files and associate them with the correct legacy campaign descriptor. Old numbered slots cannot reliably establish whether two files belong to the same run. Do not group them automatically by slot number or character name. Provide a clear import choice or separate imported runs unless a shared origin is established. Never claim a legacy migration passed from a successful fresh-game test alone.

## Implementation sequence

1. Add campaign descriptors, discovery, validation, and explicit adapters for retained plane folders. Make starting locations and quests data-driven.
2. Introduce campaign/playthrough/snapshot identity and a versioned persistence/index layer. Isolate every save destination and asynchronous completion from global selection.
3. Implement coordinated activation and loading, including campaign-specific cache and scene lifecycle management.
4. Add campaign selection before character creation and a run-oriented Load Game screen. Bind Continue and each save action to an explicit run.
5. Prove isolation with two small original test campaigns and three playthroughs before investing in a full campaign adaptation. Validate legacy import separately.

Campaign content assessment can proceed alongside this work. The selectable-campaign foundation precedes committing substantial content to a single campaign. A full Curse of Strahd adaptation is not needed to prove the architecture.

## Acceptance cases

These are requirements for future implementation. Initial status for every case is **Not run**.

| ID | Scenario | Required result |
| --- | --- | --- |
| C01 | Start New Game with two installed campaigns. | The player chooses a campaign before character options are built; only that campaign's starting options and opening quests appear. |
| C02 | Create two runs of the same campaign with identical character and run names. If offered, also start New Game+ from one run. | Every new run receives a separate identity and save history; ordinary runs have independent state, and New Game+ follows its declared carryover rules while preserving the source run. |
| C03 | Add a third run in another campaign and alternate between all three. | The correct campaign resources, UI, story, and state load without changing a global setting or restarting the application. |
| C04 | Create several manual checkpoints in one run, then quicksave and autosave in each run. | Every checkpoint remains associated with its run; no other run's manual or automatic saves change. |
| C05 | Restart the application after playing a run in the second campaign. | Continue resolves that run and its compatible campaign automatically. |
| C06 | Queue an autosave in run A, then switch to run B before it completes. | The write and completion metadata remain attached to A; B's files and Continue target remain correct. |
| C07 | Remove or make a required campaign version incompatible. | Its saves remain listed with an explanation; loading does not substitute content, regenerate a world, or overwrite data. |
| C08 | Fail a write or load during restoration. | Existing snapshots remain recoverable, errors are reported, and no partial state becomes the active successful session. |
| C09 | Rename a campaign title and a run label. | Stable identities preserve discovery and loading; labels do not determine destinations. |
| C10 | Import legacy manual, auto, and quicksave files. | Originals remain intact and the selected legacy campaign loads; unrelated slots are not silently combined. |
| C11 | Update campaign content or the save format. | Compatible saves load under the documented rule; incompatible ones require an explicit migration or the appropriate version. |
| C12 | Select a campaign with missing required startup/story data. | Validation explains the problem before starting; shared fallback does not inject another campaign's opening. |

Record actual test evidence in the [validation guide](Progression-Validation.md). The prior progression patch's passing tests do not establish that this proposed architecture works.
