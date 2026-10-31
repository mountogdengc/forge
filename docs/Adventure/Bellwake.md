# The Last Fare of Bellwake — 0.2.0 candidate

Bellwake is the fork's first chapter. Five passengers return from the night crossing; the sixth leaves a wet coat. Speak to Mira at Bellwake Quay, investigate the Drowned Tollhouse, recover Ada Vale's ticket, and decide how her crossing ends. The chapter has two definite endings and no New Game+.

## Playing the candidate

Use the fork's Windows test package and a fresh Bellwake playthrough. Choose **The Last Fare of Bellwake** in New Game, then **Lantern Watch** (white creatures and protection) or **Ember Crew** (red creatures and damage). Both are 40-card decks. The chapter uses one forgiving difficulty with 20 starting life and no gold or maximum-life loss on defeat.

Talk to Mira beside the landing. Tor offers an optional practice game on the west side of the quay. The first practice win lets you choose two Pacifisms or two Lightning Strikes; later practice wins do not repeat that reward. Inspect new cards in the collection and open Decks to choose replacements. Rewards do not change your deck automatically. Mira offers free recovery.

Leave the quay through its south landing and travel east across the marsh. The fare book inside the tollhouse provides an optional clue. Defeat the Keeper, take the ticket, return west, and **speak to Mira** to choose the ending. Merely entering the quay does not resolve the story. The quay's later dialogue reflects your choice; the journal retains a chapter-complete entry.

## Saves and content versions

The new save implementation supports stable exploration inside locations as well as the overworld. F5 quicksaves and F8 quickloads the active run. Finish an open conversation, battle, reward collection, or transition before saving/loading. The brief result animation must also settle. Save feedback appears after the checkpoint commits.

Interior snapshots include the room and player position, live actors, per-visit defeated encounters, persistent map changes, shop identity/stock seed, and the state needed for room exits. Returning to a saved town must not heal the character or replay its opening conversation. Loading an older checkpoint intentionally restores that older point in the story.

Campaign ID remains `fork.bellwake`; this candidate's content version is `0.2.0`. Existing `0.1.0` saves are not migrated. Keep the matching old package if you want to use them. Only one content version per campaign ID is supported per installation; do not overwrite an old installation containing saves you need.

## Implementation and acceptance

The chapter keeps existing Forge rules, AI, UI foundations, and shared art. Its world, quests, enemies, starters, and consequences are campaign-owned. Root maps remain in the namespaced shared folder `common/maps/map/bellwake` because the current root-POI loader uses that prefix. The maps still reuse inherited town/dungeon art; the target duration of 30–45 minutes is a playtest target, not a measured result.

The implementation plan is [Bellwake chapter polish](../superpowers/plans/2026-10-31-bellwake-chapter-polish.md). Current verification and remaining acceptance work belong in [Progression validation](Progression-Validation.md). Build/automated success does not establish match balance or an unassisted finish.

The explicit graphical runner is `tools/adventure/run_bellwake_smoke.ps1`. It compiles the desktop test fixtures, starts a native libGDX client with a disposable profile, and retains its log and screenshot. It checks restoration, rollback, connected rooms, respawning encounters, shops, and assisted chapter interactions. It supplies victories; it is not a played match or balance test. See the validation guide for invocation and actual results.

## Historical 0.1.0 record

The following records the earlier data-only chapter, not the behavior or verification of 0.2.0.

# The Last Fare of Bellwake â€” 0.1.0

An original self-contained Adventure chapter. Five passengers return from the night crossing. The sixth leaves a wet coat. Start in Bellwake Quay, investigate the Drowned Tollhouse to the east, and bring Ada Vale's last ticket back to Mira. The ending is the player's choice, not a sequel hook. No New Game+ or imported Shandalar quest line.

## Play on the Windows test build

The data-only add-on targets build `8f260e762907`. Close the game and copy the add-on's `res` into the directory containing `Start-Adventure-Test.cmd`, alongside the existing `res`. The files added are under `res/adventure/Bellwake` and `res/adventure/common/maps/map/bellwake` only. Reopen the game and select New Game â†’ The Last Fare of Bellwake. Use a fresh character. A separate game client, new Java installation, and changes to the profile are unnecessary. Keep the add-on installed while using its saves.

To try both endings without replaying the battle, make a named overworld save after retrieving the ticket and before entering the quay. Revisit Mira after the ending. Come out to the overworld again before saving. Version matching is exact; do not replace the content version under existing saves.

## Implementation and scope

Stable campaign ID: `fork.bellwake`. Opening quest: `7001` (The Sixth Coat). Spawn is the quay so the existing defeat/respawn path can find it. The tollhouse is placed 15 tiles east on a 120 Ã— 120 marsh. The chapter reuses Forge's starter options, art, and base tile layers from `towns/swamp_town.tmx` and `evilgrove/evilgrove_8_troll.tmx`. Its chapter maps, actors, quests, deck, and consequences are separate files. The two map copies reside under a uniquely named common subfolder because the target client resolves root POI maps from the common map directory. No existing maps are replaced.

## Verification and limits (ending spoilers)

A Linux graphical smoke used the Java client and assets from the Windows test ZIP, system Java 21, Xvfb, and an isolated test profile. Campaign selection, character creation, opening dialogue, an overworld checkpoint, both maps and the Keeper's actual duel startup were exercised. An assisted progression test invoked the map's post-victory dialogue, took the ticket, and tested each ending from the same saved checkpoint. The first branch restored ending 1 with one Ghoulcaller's Bell and zero Traveler's Amulets; the second restored ending 2 with the inverse counts. Re-entry restored the matching quay dialogue without awarding a second card, and the tollhouse enemy stayed removed after loading the pre-choice checkpoint. This is not an unassisted match win, a balance assessment, or native Windows graphical validation. The final boss greeting and the roaming rat deck choice are content-only polish.

Native Windows acceptance: play a complete match against the Keeper, return for an ending, save outside, quit, and use Continue. Check that revisiting either location does not repeat the fight or grant a second ending reward. Test a loss/rematch separately. No changes to multiplayer, engine code, or the product name.
