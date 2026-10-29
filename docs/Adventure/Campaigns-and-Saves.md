# Selectable campaigns and independent playthroughs

## Decision and status

Accepted product requirement, October 29, 2026: the player chooses a campaign whenever starting a new game and can keep multiple saved playthroughs. This includes several runs of the same campaign and runs of different campaigns at the same time.

The campaign picker, independent playthrough storage, save browser, campaign activation lifecycle, and legacy import path are implemented on `adventure/progression-foundation`. This document describes their contract and current limits. Build and interactive evidence are recorded separately in [Progression validation](Progression-Validation.md); implementation alone does not establish that every acceptance case has passed.

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

If a campaign enables New Game+, it receives a new playthrough ID. The first implementation uses the inherited carryover behavior: retain the character and collection, generate a new world, clear quest progress and quest items, and apply the selected difficulty and campaign opening. The source run and its snapshots remain intact. A descriptor can enable or disable this behavior; custom carryover policies are future work.

For example, two runs of the same campaign can each have an autosave, quicksave, and several named manual checkpoints. A third run in another campaign can coexist with both. Reusing a character name never joins those runs or changes their storage destinations.

The storage root is `<Adventure user directory>/campaign-saves/`. Human-readable titles and names never become directory names. The browser reads JSON metadata without allocating every save's preview image.

| Relative path | Purpose |
| --- | --- |
| `playthroughs/<playthroughId>/playthrough.json` | Run identity, campaign ID/version, name, character, and creation time. |
| `playthroughs/<playthroughId>/saves/<snapshotId>.sav` | Immutable binary payload, preserving the existing Forge save-header format. |
| `playthroughs/<playthroughId>/saves/<snapshotId>.json` | Versioned envelope, kind, label, location, timestamp, and SHA-256 checksum. Published after the payload is complete. |
| `continue.json` | Campaign, version, playthrough, and snapshot locator. |

The normal retention target is three autosaves and three quicksaves per run. The current Continue target and a newly committed save awaiting its callback may temporarily extend that history. Manual, initial, and imported snapshots are not automatically pruned. Explicit replacement creates a new manual snapshot before archiving the old envelope as `.json.replaced`; the old payload remains available for recovery.

## Campaign definitions

`CampaignRegistry` discovers built-in folders under `res/adventure/` and user folders under `<Adventure user directory>/userplanes/`. A package provides `campaign.json` at its root. A minimal explicit descriptor looks like this:

```json
{
  "schemaVersion": 1,
  "id": "fork.example-adventure",
  "version": "1.0.0",
  "title": "Example Adventure",
  "description": "An original campaign with its own world and opening.",
  "startingQuestIds": [],
  "startingLocation": null,
  "allowNewGamePlus": false,
  "inheritCommonWorld": false,
  "inheritCommonQuests": false,
  "development": false
}
```

This is a descriptor example, not a complete playable content package. Supply `world/world.json` and `world/quests.json` plus their referenced content. An empty quest catalog is valid when the campaign declares no opening quests. `startingQuestIds` is required and names numeric quest-template IDs as strings. `startingLocation` is an optional point-of-interest name; null starts on the overworld. Existing `config.json`, hero, deck, difficulty, and reward formats continue to provide character and starting options, with shared configuration/art/UI fallback.

IDs and versions use 1–128 letters, digits, periods, underscores, or hyphens, beginning with a letter or digit. An explicit campaign keeps its ID and version when its title or installation folder is renamed. Duplicate installed campaign IDs are reported as ambiguous and excluded, rather than selecting an arbitrary copy. Only one installed version per campaign ID is supported in this first implementation.

Descriptor schema and save-envelope format are both version 1. Loads require an exact campaign-content version match. Dependency declarations, version migrations, preview artwork, arbitrary initialization hooks, and custom New Game+ carryover rules are extension work; they are not implied by the descriptor above.

Shared Java startup code reads opening quests from the descriptor for both New Game and New Game+. Explicit campaigns receive no automatic Shandalar opening.

Support the selected content design, including authored locations and event sequences as well as procedural components where useful. A new campaign must be able to be a different adventure, rather than only a visual or deck-selection variant of the inherited story.

Common world and quest-catalog fallback require separate opt-ins. Shared art, UI, configuration, and other resource fallback remain available. Discovery checks descriptor syntax, world dimensions, the quest catalog, and declared opening quest templates; character setup checks the configuration. It does not yet exhaustively validate every referenced map, enemy, dialog, or starting location.

Existing plane folders without a descriptor use a legacy adapter. The adapter retains common resource fallback, `Spawn`, and New Game+, and adds historical opening quest `28` only when that template exists. Its ID is derived from the original built-in/user folder name and its version is `legacy-1`; rename stability applies to explicit manifests, so do not rename a legacy folder after creating saves. The inherited development planes remain hidden unless developer mode is enabled; explicit packages use their `development` flag.

## Historical source audit

Source review baseline before this implementation: [`37216f201e57ad43e033345167cdfa066dbc438b`](https://github.com/mountogdengc/forge/commit/37216f201e57ad43e033345167cdfa066dbc438b). The table records the problems that motivated the changes, not the new implementation's behavior.

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

Character setup uses `Config.forCampaign()` and campaign-specific hero data as a preview. Selecting a campaign or cancelling setup does not activate it or change the running playthrough. Pressing Start checkpoints the outgoing run, activates the chosen package, creates a new run identity, generates the world, adds declared opening quests, and commits an initial snapshot before entering a starting location. If generation fails, the menu reports the failure and attempts to restore the previous Continue checkpoint. A failed first save leaves the new game active for a save retry.

`CampaignManager` coordinates campaign configuration, resource lookups, quest/event controllers, hero/avatar data, enemy/shop/item catalogs, reward pools, localization, music, scenes, stages, and HUD listeners. It clears the Back history and retires outgoing UI resources after the next scene switch. The old global World selector has been removed from Settings. Quickload also switches to the restored campaign's scene.

Load in this order:

1. Read lightweight snapshot metadata and identify its campaign, version, playthrough, and format without creating a world.
2. Resolve the installed package and check compatibility. A missing or incompatible package leaves the snapshot intact and produces an actionable error.
3. Finish or isolate writes from the previous session and prepare the target campaign's resources.
4. Capture a detached copy of the outgoing overworld state, restore the target, and publish its active session only after restoration succeeds. If restoration throws, reactivate and restore the previous campaign/state. If rollback itself fails, report that failure and leave the saved checkpoints intact. No failure path generates a substitute world.
5. Update Continue using campaign, playthrough, and snapshot IDs. Track the last played run separately from the completion time of a background write.

A JSON envelope supports listing snapshots without eagerly constructing every native preview image. The existing custom binary header layout and its explicit serialization identifier are retained.

Saving and loading while inside a town, dungeon, or other interior map remain blocked. Return to the overworld before saving or switching playthroughs. This boundary protects rollback while exact interior restoration is not implemented; independent saves do not promise saving anywhere.

## Save integrity and version policy

Every queued write captures immutable campaign/playthrough identity, snapshot ID, destination, and session token before entering the background worker. A late write stays in its original run. Continue publication checks both the active session and request ordering, so an old session cannot select itself and an older autosave cannot replace a newer manual checkpoint's Continue selection.

Manual, quick, initial, imported, and automatic saves share the repository writer: write and validate a temporary payload, calculate its checksum, move it into place, then publish the JSON envelope. Failed writes leave older snapshots available. Metadata replacement uses an atomic move where supported, with a completed-file move fallback. This is interrupted-write protection, not a guarantee against every filesystem or power-loss failure.

Snapshots record the campaign version and save-format version they were created with. Load only a compatible installed version. Content updates need an explicit compatibility rule or migration; do not silently reinterpret old quest IDs or map identities under a different package. A campaign display-name change must not break discovery of its saves.

Load Game includes **Import old saves**. It lists supported old numbered, auto, and quicksave files by campaign and imports a selected file as a copy into its own named playthrough. Old numbered slots cannot reliably establish a shared run, so imports are never grouped automatically by name or slot number. The old filename-based Continue path also imports the selected legacy file before loading it. The originals remain untouched. Repository copy tests do not by themselves establish that every historical binary save can be loaded.

## Implementation map

| Responsibility | Main implementation |
| --- | --- |
| Campaign discovery and startup contract | `campaign/CampaignDefinition`, `CampaignRegistry` |
| Activation and resource lifecycle | `campaign/CampaignManager`, `Config`, scene/stage reset hooks |
| Immutable save storage and identity | `world/PlaythroughRepository`, metadata/locator classes, `PlaythroughSession` |
| Live game capture, restore, Continue, and import | `world/WorldSave` |
| Campaign picker and character preview | `scene/CampaignSelectionScene`, `NewGameScene` |
| Run browser, named checkpoints, and New Game+ | `scene/SaveLoadScene`, `StartScene` |
| Regression coverage | `test/forge/adventure/campaign/CampaignRegistryTest`, `test/forge/adventure/world/PlaythroughRepositoryTest` |

Campaign content assessment can proceed alongside this work. The selectable-campaign foundation precedes committing substantial content to a single campaign. A full Curse of Strahd adaptation is not needed to prove the architecture.

## Acceptance cases

These are acceptance requirements. Consult the validation guide for actual automated and interactive coverage; a partial automated case is not a complete gameplay pass.

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

Record actual test evidence in the [validation guide](Progression-Validation.md). The prior progression patch's passing tests are separate from campaign and save validation. Run renaming, campaign-content migrations, and custom New Game+ policies remain future work even though names are not used as storage keys.
